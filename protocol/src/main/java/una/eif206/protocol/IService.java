package una.eif206.protocol;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.ReservaExtraccion;
import una.eif206.model.Usuario;

import java.util.List;
import java.util.Map;

/**
 * Agrupa los metodos publicos de los 9 services del P1 (una.eif206.service.*).
 * Nota: Java no permite dos metodos con el mismo nombre y los mismos parametros
 * pero distinto tipo de retorno, asi que los unicos metodos renombrados respecto
 * al P1 son findAll() y findById(String), que colisionaban entre Categoria,
 * Funcionario, Recurso y Reserva. El resto conserva nombre y firma originales.
 */
public interface IService {

    // ===================== UsuarioService =====================

    Usuario login(String id, String clave);

    String cambiarClave(Usuario u, String claveActual, String claveNueva);

    // ===================== FuncionarioService =====================

    List<Funcionario> findAllFuncionarios();

    List<Funcionario> search(Funcionario e);

    List<Funcionario> search(String texto);

    String create(Funcionario e);

    String update(Funcionario f);

    String delete(Funcionario e);

    Funcionario findFuncionarioById(String id);

    // ===================== CategoriaService =====================

    List<CategoriaRecurso> findAllCategorias();

    CategoriaRecurso findCategoriaById(String id);

    List<CategoriaRecurso> search(CategoriaRecurso e);

    String create(CategoriaRecurso e);

    String update(CategoriaRecurso c);

    String delete(CategoriaRecurso e);

    // ===================== RecursoService =====================

    List<Recurso> findAllRecursos();

    List<Recurso> findByCategoria(CategoriaRecurso categoria);

    String create(Recurso e);

    String update(Recurso r);

    String delete(Recurso e);

    // ===================== ReservaService =====================

    List<Reserva> findAllReservas();

    List<Reserva> findByFuncionario(Funcionario f);

    String generarId();

    String create(Reserva e);

    Recurso buscarRecursoDisponible(CategoriaRecurso categoria, String fecha, String horaInicio, String horaFin);

    String asignarRecursosDisponibles(List<CategoriaRecurso> categorias, String fecha,
                                       String horaInicio, String horaFin, List<Recurso> recursosAsignados);

    String cancelar(Reserva r);

    // ===================== CalendarioService =====================

    String[][] getCalendario(String fecha, CategoriaRecurso categoria);

    // ===================== ActividadesService =====================

    Map<String, Map<String, String>> getActividadesSemana(String fechaReferencia) throws Exception;

    // ===================== EstadisticasService =====================

    Map<String, Integer> getEstadisticasRecursos(String desde, String hasta) throws Exception;

    Map<String, Integer> getEstadisticasActividades(String desde, String hasta) throws Exception;

    // ===================== ReservaIAService =====================

    ReservaExtraccion extraerReserva(String frase) throws Exception;
}
