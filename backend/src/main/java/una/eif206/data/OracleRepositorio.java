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
 */
public class OracleRepositorio implements Repositorio {

    private final ServicioUsuario servicioUsuario;
    private final ServicioFuncionario servicioFuncionario;
    private final ServicioCategoria servicioCategoria;
    private final ServicioRecurso servicioRecurso;
    private final ServicioReserva servicioReserva;
    private final ServicioEstadisticas servicioEstadisticas;

    public OracleRepositorio() {
        this(new ServicioUsuario(), new ServicioFuncionario(), new ServicioCategoria(),
                new ServicioRecurso(), new ServicioReserva(), new ServicioEstadisticas());
    }

    /** Para pruebas: permite inyectar ServicioX con un ConexionProvider simulado. */
    public OracleRepositorio(ServicioUsuario servicioUsuario, ServicioFuncionario servicioFuncionario,
                              ServicioCategoria servicioCategoria, ServicioRecurso servicioRecurso,
                              ServicioReserva servicioReserva, ServicioEstadisticas servicioEstadisticas) {
        this.servicioUsuario = servicioUsuario;
        this.servicioFuncionario = servicioFuncionario;
        this.servicioCategoria = servicioCategoria;
        this.servicioRecurso = servicioRecurso;
        this.servicioReserva = servicioReserva;
        this.servicioEstadisticas = servicioEstadisticas;
    }

    // ===================== Usuario =====================

    @Override
    public Usuario login(String id, String clave) {
        try {
            return servicioUsuario.login(id, clave);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public void cambiarClave(String id, String claveNueva) throws GlobalException, NoDataException {
        servicioUsuario.cambiarClave(id, claveNueva);
    }

    // ===================== Funcionario =====================

    @Override
    public List<Funcionario> listarFuncionarios() {
        try {
            return servicioFuncionario.listar();
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public Funcionario buscarFuncionarioPorId(String id) {
        try {
            return servicioFuncionario.buscar(id);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public List<Funcionario> buscarFuncionariosPorTexto(String texto) {
        try {
            return servicioFuncionario.listarPorTexto(texto);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public void insertarFuncionario(Funcionario f) throws GlobalException, NoDataException {
        servicioFuncionario.insertar(f);
    }

    @Override
    public void modificarFuncionario(Funcionario f) throws GlobalException, NoDataException {
        servicioFuncionario.modificar(f);
    }

    @Override
    public void eliminarFuncionario(String id) throws GlobalException, NoDataException {
        servicioFuncionario.eliminar(id);
    }

    // ===================== Categoria =====================

    @Override
    public List<CategoriaRecurso> listarCategorias() {
        try {
            return servicioCategoria.listar();
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public CategoriaRecurso buscarCategoriaPorId(String id) {
        try {
            return servicioCategoria.buscar(id);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public List<CategoriaRecurso> buscarCategoriasPorTexto(String texto) {
        try {
            return servicioCategoria.listarPorTexto(texto);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public String insertarCategoria(String descripcion) throws GlobalException, NoDataException {
        return servicioCategoria.insertar(descripcion);
    }

    @Override
    public void modificarCategoria(CategoriaRecurso c) throws GlobalException, NoDataException {
        servicioCategoria.modificar(c);
    }

    @Override
    public void eliminarCategoria(String id) throws GlobalException, NoDataException {
        servicioCategoria.eliminar(id);
    }

    // ===================== Recurso =====================

    @Override
    public List<Recurso> listarRecursos() {
        try {
            return servicioRecurso.listar();
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public List<Recurso> listarRecursosPorCategoria(String categoriaId) {
        try {
            return servicioRecurso.listarPorCategoria(categoriaId);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public Recurso buscarRecursoPorId(String id) {
        try {
            return servicioRecurso.buscar(id);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public void insertarRecurso(Recurso r) throws GlobalException, NoDataException {
        servicioRecurso.insertar(r);
    }

    @Override
    public void modificarRecurso(Recurso r) throws GlobalException, NoDataException {
        servicioRecurso.modificar(r);
    }

    @Override
    public void eliminarRecurso(String id) throws GlobalException, NoDataException {
        servicioRecurso.eliminar(id);
    }

    // ===================== Reserva =====================

    @Override
    public List<Reserva> listarReservas() {
        try {
            return servicioReserva.listar();
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public Reserva buscarReservaPorId(String id) {
        try {
            return servicioReserva.buscar(id);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public List<Reserva> listarReservasPorFuncionario(String funcionarioId) {
        try {
            return servicioReserva.listarPorFuncionario(funcionarioId);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public List<String> listarIdsRecursosOcupados(String fecha, String horaInicio, String horaFin) {
        try {
            return servicioReserva.listarIdsRecursosOcupados(fecha, horaInicio, horaFin);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public String siguienteIdReserva() {
        try {
            return servicioReserva.siguienteId();
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public void crearReserva(Reserva reserva) throws GlobalException, NoDataException {
        servicioReserva.crear(reserva);
    }

    @Override
    public void cancelarReserva(String id) throws GlobalException, NoDataException {
        servicioReserva.cancelar(id);
    }

    // ===================== Estadisticas =====================

    @Override
    public Map<String, Integer> estadisticasRecursos(String desde, String hasta) {
        try {
            return servicioEstadisticas.estadisticasRecursos(desde, hasta);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Integer> estadisticasActividades(String desde, String hasta) {
        try {
            return servicioEstadisticas.estadisticasActividades(desde, hasta);
        } catch (GlobalException | NoDataException e) {
            throw new RepositorioException(e.getMessage(), e);
        }
    }
}
