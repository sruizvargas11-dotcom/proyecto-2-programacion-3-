package una.eif206.data;

import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.enums.EstadoReserva;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * CRUD de RESERVA/RESERVA_RECURSO. Nombres exactos a
 * script_reservas_oracle11g.sql: insertarReserva, insertarReservaRecurso,
 * cancelarReserva, buscarReserva, listarReserva, listarReservaPorFuncionario,
 * listarRecursoPorReserva, listarRecursosOcupados, siguienteIdReserva.
 *
 * <p>Cada fila de RESERVA solo trae FUNCIONARIO_ID; para reconstruir el
 * Funcionario completo y la lista de recursos se hacen consultas adicionales
 * dentro de la MISMA conexion ya abierta (nunca se crea un ServicioFuncionario
 * ni un ServicioRecurso nuevo aqui, porque eso usaria su propio
 * ConexionProvider por defecto e ignoraria el inyectado en las pruebas).</p>
 */
public class ServicioReserva extends Servicio {

    private static final String INSERTAR = "{call insertarReserva(?,?,?,?,?,?)}";
    private static final String INSERTAR_RECURSO = "{call insertarReservaRecurso(?,?)}";
    private static final String CANCELAR = "{call cancelarReserva(?)}";
    private static final String BUSCAR = "{?=call buscarReserva(?)}";
    private static final String LISTAR = "{?=call listarReserva()}";
    private static final String LISTAR_POR_FUNCIONARIO = "{?=call listarReservaPorFuncionario(?)}";
    private static final String LISTAR_RECURSOS_POR_RESERVA = "{?=call listarRecursoPorReserva(?)}";
    private static final String LISTAR_RECURSOS_OCUPADOS = "{?=call listarRecursosOcupados(?,?,?)}";
    private static final String SIGUIENTE_ID = "{?=call siguienteIdReserva()}";
    private static final String BUSCAR_FUNCIONARIO = "{?=call buscarFuncionario(?)}";
    private static final String LISTAR_RECURSOS_POR_CATEGORIA = "{?=call listarRecursoPorCategoria(?)}";

    /** Serializa crear(...) dentro del proceso: dos clientes no reservan el mismo recurso. */
    private static final Lock CREAR_RESERVA_LOCK = new ReentrantLock();

    public ServicioReserva() {
        super();
    }

    public ServicioReserva(ConexionProvider conexionProvider) {
        super(conexionProvider);
    }

    public List<Reserva> listar() throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(LISTAR)) {
            registrarCursorSalida(cs, 1);
            cs.execute();
            return mapearLista(cs);
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    public List<Reserva> listarPorFuncionario(String funcionarioId) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(LISTAR_POR_FUNCIONARIO)) {
            registrarCursorSalida(cs, 1);
            cs.setString(2, funcionarioId);
            cs.execute();
            return mapearLista(cs);
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    public Reserva buscar(String id) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(BUSCAR)) {
            registrarCursorSalida(cs, 1);
            cs.setString(2, id);
            cs.execute();
            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                return rs.next() ? mapearFilaCompleta(rs) : null;
            }
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    public List<String> listarIdsRecursosOcupados(String fecha, String horaInicio, String horaFin)
            throws GlobalException, NoDataException {
        conectar();
        try {
            return idsRecursosOcupados(fecha, horaInicio, horaFin);
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    public String siguienteId() throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(SIGUIENTE_ID)) {
            cs.registerOutParameter(1, Types.VARCHAR);
            cs.execute();
            return cs.getString(1);
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    public void cancelar(String id) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(CANCELAR)) {
            cs.setString(1, id);
            cs.execute();
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    /**
     * Genera el id (si viene vacio), resuelve cada Recurso solicitado solo
     * por categoria (id vacio + categoria presente) a un recurso libre real
     * de esa categoria para esa fecha/franja, valida disponibilidad de los
     * que ya traen id, e inserta la reserva y sus recursos: todo en UNA
     * sola conexion con autoCommit(false) (si algo falla, rollback completo)
     * y bajo CREAR_RESERVA_LOCK, para que dos clientes concurrentes pidiendo
     * la misma categoria/horario nunca reciban el mismo recurso.
     */
    public void crear(Reserva reserva) throws GlobalException, NoDataException {
        CREAR_RESERVA_LOCK.lock();
        try {
            conectar();
            try {
                conexion.setAutoCommit(false);
                if (reserva.getId() == null || reserva.getId().trim().isEmpty()) {
                    reserva.setId(generarIdEnTransaccion());
                }
                List<Recurso> resueltos = resolverRecursos(reserva);
                try (CallableStatement cs = conexion.prepareCall(INSERTAR)) {
                    cs.setString(1, reserva.getId());
                    cs.setString(2, reserva.getActividad());
                    cs.setDate(3, Date.valueOf(reserva.getFecha()));
                    cs.setString(4, reserva.getHoraInicio());
                    cs.setString(5, reserva.getHoraFin());
                    cs.setString(6, reserva.getFuncionario().getId());
                    cs.execute();
                }
                try (CallableStatement cs = conexion.prepareCall(INSERTAR_RECURSO)) {
                    for (Recurso recurso : resueltos) {
                        cs.setString(1, reserva.getId());
                        cs.setString(2, recurso.getId());
                        cs.execute();
                    }
                }
                reserva.setRecursos(resueltos);
                conexion.commit();
            } catch (SQLException e) {
                rollback();
                throw errorSql(e);
            } catch (GlobalException e) {
                rollback();
                throw e;
            } finally {
                restaurarAutoCommit();
                desconectar();
            }
        } finally {
            CREAR_RESERVA_LOCK.unlock();
        }
    }

    private String generarIdEnTransaccion() throws SQLException {
        try (CallableStatement cs = conexion.prepareCall(SIGUIENTE_ID)) {
            cs.registerOutParameter(1, Types.VARCHAR);
            cs.execute();
            return cs.getString(1);
        }
    }

    /**
     * Para cada Recurso solicitado: si ya trae id, lo valida contra los
     * ocupados (mismo mensaje de siempre); si no trae id, elige el primer
     * recurso DISPONIBLE de esa categoria que no este ocupado ni ya elegido
     * para otro item de la misma reserva. Si alguna categoria se queda sin
     * recurso libre, no inserta nada: lanza con TODAS las categorias sin
     * disponibilidad en un solo mensaje.
     */
    private List<Recurso> resolverRecursos(Reserva reserva) throws SQLException, GlobalException {
        List<String> ocupados = idsRecursosOcupados(reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());
        Set<String> usados = new HashSet<>(ocupados);
        List<Recurso> resueltos = new ArrayList<>();
        List<String> categoriasSinDisponibilidad = new ArrayList<>();
        for (Recurso solicitado : new ArrayList<>(reserva.getRecursos())) {
            if (tieneId(solicitado)) {
                if (usados.contains(solicitado.getId())) {
                    throw new GlobalException("No hay disponibilidad para el recurso en ese horario");
                }
                resueltos.add(solicitado);
                usados.add(solicitado.getId());
                continue;
            }
            Recurso libre = buscarRecursoLibrePorCategoria(solicitado.getCategoria().getId(), usados);
            if (libre == null) {
                categoriasSinDisponibilidad.add(solicitado.getCategoria().getDescripcion());
                continue;
            }
            resueltos.add(libre);
            usados.add(libre.getId());
        }
        if (!categoriasSinDisponibilidad.isEmpty()) {
            throw new GlobalException("No hay recursos disponibles para: " + String.join(", ", categoriasSinDisponibilidad));
        }
        return resueltos;
    }

    private static boolean tieneId(Recurso r) {
        return r.getId() != null && !r.getId().trim().isEmpty();
    }

    private Recurso buscarRecursoLibrePorCategoria(String categoriaId, Set<String> usados) throws SQLException {
        try (CallableStatement cs = conexion.prepareCall(LISTAR_RECURSOS_POR_CATEGORIA)) {
            registrarCursorSalida(cs, 1);
            cs.setString(2, categoriaId);
            cs.execute();
            for (Recurso candidato : ServicioRecurso.mapearLista(cs)) {
                if (candidato.isDisponible() && !usados.contains(candidato.getId())) {
                    return candidato;
                }
            }
        }
        return null;
    }

    private List<String> idsRecursosOcupados(String fecha, String horaInicio, String horaFin) throws SQLException {
        try (CallableStatement cs = conexion.prepareCall(LISTAR_RECURSOS_OCUPADOS)) {
            registrarCursorSalida(cs, 1);
            cs.setDate(2, Date.valueOf(fecha));
            cs.setString(3, horaInicio);
            cs.setString(4, horaFin);
            cs.execute();
            List<String> ids = new ArrayList<>();
            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                while (rs.next()) {
                    ids.add(rs.getString("RECURSO_ID"));
                }
            }
            return ids;
        }
    }

    private void rollback() {
        try {
            if (conexion != null) {
                conexion.rollback();
            }
        } catch (SQLException ignored) {
            // Mejor esfuerzo: si el rollback falla, desconectar() cierra la conexion igual.
        }
    }

    private void restaurarAutoCommit() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.setAutoCommit(true);
            }
        } catch (SQLException ignored) {
        }
    }

    private List<Reserva> mapearLista(CallableStatement cs) throws SQLException {
        List<Reserva> lista = new ArrayList<>();
        try (ResultSet rs = (ResultSet) cs.getObject(1)) {
            while (rs.next()) {
                lista.add(mapearFilaCompleta(rs));
            }
        }
        return lista;
    }

    /** Fila base de RESERVA + 2 consultas de enriquecimiento (funcionario, recursos) en la misma conexion. */
    private Reserva mapearFilaCompleta(ResultSet rs) throws SQLException {
        String id = rs.getString("ID");
        Funcionario funcionario = cargarFuncionario(rs.getString("FUNCIONARIO_ID"));
        Reserva reserva = new Reserva(id, rs.getString("ACTIVIDAD"),
                rs.getDate("FECHA").toLocalDate().toString(),
                rs.getString("HORA_INICIO"), rs.getString("HORA_FIN"), funcionario);
        reserva.setEstado(EstadoReserva.valueOf(rs.getString("ESTADO")));
        reserva.setRecursos(cargarRecursos(id));
        return reserva;
    }

    private Funcionario cargarFuncionario(String funcionarioId) throws SQLException {
        try (CallableStatement cs = conexion.prepareCall(BUSCAR_FUNCIONARIO)) {
            registrarCursorSalida(cs, 1);
            cs.setString(2, funcionarioId);
            cs.execute();
            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                return rs.next() ? ServicioFuncionario.mapearFila(rs) : null;
            }
        }
    }

    private List<Recurso> cargarRecursos(String reservaId) throws SQLException {
        try (CallableStatement cs = conexion.prepareCall(LISTAR_RECURSOS_POR_RESERVA)) {
            registrarCursorSalida(cs, 1);
            cs.setString(2, reservaId);
            cs.execute();
            return ServicioRecurso.mapearLista(cs);
        }
    }
}
