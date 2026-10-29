package una.eif206.data;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.Usuario;

import java.util.List;
import java.util.Map;

/**
 * Todas las operaciones de persistencia que necesitan los services de
 * una.eif206.service. Implementaciones: {@link OracleRepositorio} (delega en
 * los ServicioX contra la base real) y {@link MemoriaRepositorio} (solo en
 * memoria, para pruebas).
 *
 * <p>Los metodos de solo lectura (listarX, buscarXPorId, login) no declaran
 * excepciones chequeadas: si falla el acceso a datos lanzan
 * {@link RepositorioException} (sin chequear), porque los metodos del P1 que
 * los usan (findAll, findById, search, ...) tampoco declaran throws. Los
 * metodos de escritura (insertar/modificar/eliminar/cambiarClave/crearReserva/
 * cancelarReserva) si declaran {@link GlobalException}/{@link NoDataException}
 * para que el *Service que los llama pueda convertir el fallo en el mensaje
 * de error de String que espera el llamador (igual que en el P1).</p>
 */
public interface Repositorio {

    // ===================== Usuario =====================

    Usuario login(String id, String clave);

    void cambiarClave(String id, String claveNueva) throws GlobalException, NoDataException;

    // ===================== Funcionario =====================

    List<Funcionario> listarFuncionarios();

    Funcionario buscarFuncionarioPorId(String id);

    List<Funcionario> buscarFuncionariosPorTexto(String texto);

    void insertarFuncionario(Funcionario f) throws GlobalException, NoDataException;

    void modificarFuncionario(Funcionario f) throws GlobalException, NoDataException;

    void eliminarFuncionario(String id) throws GlobalException, NoDataException;

    // ===================== Categoria =====================

    List<CategoriaRecurso> listarCategorias();

    CategoriaRecurso buscarCategoriaPorId(String id);

    List<CategoriaRecurso> buscarCategoriasPorTexto(String texto);

    /** Inserta y devuelve el id generado (secuencia + prefijo, ej. CAT-000004). */
    String insertarCategoria(String descripcion) throws GlobalException, NoDataException;

    void modificarCategoria(CategoriaRecurso c) throws GlobalException, NoDataException;

    void eliminarCategoria(String id) throws GlobalException, NoDataException;

    // ===================== Recurso =====================

    List<Recurso> listarRecursos();

    List<Recurso> listarRecursosPorCategoria(String categoriaId);

    Recurso buscarRecursoPorId(String id);

    void insertarRecurso(Recurso r) throws GlobalException, NoDataException;

    void modificarRecurso(Recurso r) throws GlobalException, NoDataException;

    void eliminarRecurso(String id) throws GlobalException, NoDataException;

    // ===================== Reserva =====================

    List<Reserva> listarReservas();

    Reserva buscarReservaPorId(String id);

    List<Reserva> listarReservasPorFuncionario(String funcionarioId);

    /** Ids de recurso con una reserva ACTIVA que se traslapa con ese fecha/horario. */
    List<String> listarIdsRecursosOcupados(String fecha, String horaInicio, String horaFin);

    String siguienteIdReserva();

    /**
     * Resuelve lo que {@code reserva} no trae resuelto, e inserta la reserva
     * y su relacion con los recursos. Si {@code reserva.getId()} viene vacio,
     * genera uno. Cada Recurso de {@code reserva.getRecursos()} con id vacio
     * pero con categoria se trata como "un recurso libre de esa categoria
     * para esa fecha/franja" (si uno esta ocupado, elige otro libre de la
     * misma categoria); los que ya traen id solo se validan contra lo
     * ocupado. Si alguna categoria se queda sin recurso libre, no inserta
     * nada y lanza {@link GlobalException}. Al terminar con exito, deja a
     * {@code reserva} con el id generado y los recursos reales asignados
     * (nunca placeholders solo-categoria).
     *
     * <p>Debe ejecutarse en una sola conexion/transaccion (commit solo si
     * todo sale bien) y debe quedar protegida contra concurrencia dentro del
     * proceso: generar el id, elegir recurso por categoria, verificar
     * disponibilidad e insertar son, juntos, atomicos, de forma que dos
     * llamadas simultaneas pidiendo la misma categoria/horario nunca
     * terminen con el mismo recurso.</p>
     */
    void crearReserva(Reserva reserva) throws GlobalException, NoDataException;

    void cancelarReserva(String id) throws GlobalException, NoDataException;

    // ===================== Estadisticas =====================

    Map<String, Integer> estadisticasRecursos(String desde, String hasta);

    Map<String, Integer> estadisticasActividades(String desde, String hasta);
}
