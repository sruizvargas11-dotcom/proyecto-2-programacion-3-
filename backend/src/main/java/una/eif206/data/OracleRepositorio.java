package una.eif206.data;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.Usuario;

import java.util.List;
import java.util.Map;

/**
 * Repositorio contra Oracle real: delega en los ServicioX. Los metodos de
 * solo lectura envuelven GlobalException/NoDataException como
 * RepositorioException (sin chequear) porque el contrato de Repositorio
 * (y de los *Service del P1 que lo usan) no declara throws para ellos.
 *
 * <p>Esta clase (y por lo tanto el {@code Service} que la envuelve) se
 * comparte entre todos los Worker/hilo de {@link una.eif206.net.Server}. Para
 * que eso sea seguro, cada metodo crea su propio ServicioX con
 * {@code nuevoXxx()} en vez de guardarlo en un campo: {@code Servicio.conexion}
 * es un campo de instancia (no hay forma de hacerlo local sin tocar el
 * contrato de P1 que varias pruebas ya ejercitan), asi que si dos hilos
 * reusaran el MISMO ServicioX podrian pisarse la conexion JDBC el uno al
 * otro. Con una instancia nueva por llamada, "conexion" nunca se comparte
 * entre hilos y no hace falta sincronizar nada.</p>
 */
public class OracleRepositorio implements Repositorio {

    private final ConexionProvider conexionProvider;

    public OracleRepositorio() {
        this(null);
    }

    /** Para pruebas: permite inyectar un ConexionProvider simulado (Mockito), sin tocar Oracle. */
    public OracleRepositorio(ConexionProvider conexionProvider) {
        this.conexionProvider = conexionProvider;
    }

    private ServicioUsuario nuevoServicioUsuario() {
        return conexionProvider == null ? new ServicioUsuario() : new ServicioUsuario(conexionProvider);
    }

    private ServicioFuncionario nuevoServicioFuncionario() {
        return conexionProvider == null ? new ServicioFuncionario() : new ServicioFuncionario(conexionProvider);
    }

    private ServicioCategoria nuevoServicioCategoria() {
        return conexionProvider == null ? new ServicioCategoria() : new ServicioCategoria(conexionProvider);
    }

    private ServicioRecurso nuevoServicioRecurso() {
        return conexionProvider == null ? new ServicioRecurso() : new ServicioRecurso(conexionProvider);
    }

    private ServicioReserva nuevoServicioReserva() {
        return conexionProvider == null ? new ServicioReserva() : new ServicioReserva(conexionProvider);
    }

    private ServicioEstadisticas nuevoServicioEstadisticas() {
        return conexionProvider == null ? new ServicioEstadisticas() : new ServicioEstadisticas(conexionProvider);
    }

    // ===================== Usuario =====================

    @Override
    public Usuario login(String id, String clave) {
        try {
            return nuevoServicioUsuario().login(id, clave);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public void cambiarClave(String id, String claveNueva) throws GlobalException, NoDataException {
        nuevoServicioUsuario().cambiarClave(id, claveNueva);
    }

    // ===================== Funcionario =====================

    @Override
    public List<Funcionario> listarFuncionarios() {
        try {
            return nuevoServicioFuncionario().listar();
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public Funcionario buscarFuncionarioPorId(String id) {
        try {
            return nuevoServicioFuncionario().buscar(id);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public List<Funcionario> buscarFuncionariosPorTexto(String texto) {
        try {
            return nuevoServicioFuncionario().listarPorTexto(texto);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public void insertarFuncionario(Funcionario f) throws GlobalException, NoDataException {
        nuevoServicioFuncionario().insertar(f);
    }

    @Override
    public void modificarFuncionario(Funcionario f) throws GlobalException, NoDataException {
        nuevoServicioFuncionario().modificar(f);
    }

    @Override
    public void eliminarFuncionario(String id) throws GlobalException, NoDataException {
        nuevoServicioFuncionario().eliminar(id);
    }

    // ===================== Categoria =====================

    @Override
    public List<CategoriaRecurso> listarCategorias() {
        try {
            return nuevoServicioCategoria().listar();
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public CategoriaRecurso buscarCategoriaPorId(String id) {
        try {
            return nuevoServicioCategoria().buscar(id);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public List<CategoriaRecurso> buscarCategoriasPorTexto(String texto) {
        try {
            return nuevoServicioCategoria().listarPorTexto(texto);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public String insertarCategoria(String descripcion) throws GlobalException, NoDataException {
        return nuevoServicioCategoria().insertar(descripcion);
    }

    @Override
    public void modificarCategoria(CategoriaRecurso c) throws GlobalException, NoDataException {
        nuevoServicioCategoria().modificar(c);
    }

    @Override
    public void eliminarCategoria(String id) throws GlobalException, NoDataException {
        nuevoServicioCategoria().eliminar(id);
    }

    // ===================== Recurso =====================

    @Override
    public List<Recurso> listarRecursos() {
        try {
            return nuevoServicioRecurso().listar();
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public List<Recurso> listarRecursosPorCategoria(String categoriaId) {
        try {
            return nuevoServicioRecurso().listarPorCategoria(categoriaId);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public Recurso buscarRecursoPorId(String id) {
        try {
            return nuevoServicioRecurso().buscar(id);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public void insertarRecurso(Recurso r) throws GlobalException, NoDataException {
        nuevoServicioRecurso().insertar(r);
    }

    @Override
    public void modificarRecurso(Recurso r) throws GlobalException, NoDataException {
        nuevoServicioRecurso().modificar(r);
    }

    @Override
    public void eliminarRecurso(String id) throws GlobalException, NoDataException {
        nuevoServicioRecurso().eliminar(id);
    }

    // ===================== Reserva =====================

    @Override
    public List<Reserva> listarReservas() {
        try {
            return nuevoServicioReserva().listar();
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public Reserva buscarReservaPorId(String id) {
        try {
            return nuevoServicioReserva().buscar(id);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public List<Reserva> listarReservasPorFuncionario(String funcionarioId) {
        try {
            return nuevoServicioReserva().listarPorFuncionario(funcionarioId);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public List<String> listarIdsRecursosOcupados(String fecha, String horaInicio, String horaFin) {
        try {
            return nuevoServicioReserva().listarIdsRecursosOcupados(fecha, horaInicio, horaFin);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public String siguienteIdReserva() {
        try {
            return nuevoServicioReserva().siguienteId();
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public void crearReserva(Reserva reserva) throws GlobalException, NoDataException {
        nuevoServicioReserva().crear(reserva);
    }

    @Override
    public void cancelarReserva(String id) throws GlobalException, NoDataException {
        nuevoServicioReserva().cancelar(id);
    }

    // ===================== Estadisticas =====================

    @Override
    public Map<String, Integer> estadisticasRecursos(String desde, String hasta) {
        try {
            return nuevoServicioEstadisticas().estadisticasRecursos(desde, hasta);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Integer> estadisticasActividades(String desde, String hasta) {
        try {
            return nuevoServicioEstadisticas().estadisticasActividades(desde, hasta);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }
}
