package una.eif206.data;

import una.eif206.model.Admin;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.Usuario;
import una.eif206.model.enums.EstadoReserva;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Repositorio solo en memoria: reutiliza la logica de validacion/consulta que
 * antes vivia directamente en los *Service contra Data. Usado por los tests
 * (ServicioXTest no la usa, pero RepositorioContractTest, los *ServiceTest y
 * la prueba de concurrencia si) y como reemplazo de Oracle cuando no hay base
 * de datos disponible.
 */
public class MemoriaRepositorio implements Repositorio {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("H:mm");

    private final Data data;
    private final Lock crearReservaLock = new ReentrantLock();

    public MemoriaRepositorio() {
        this(new Data());
    }

    public MemoriaRepositorio(Data data) {
        this.data = data;
    }

    public Data getData() {
        return data;
    }

    // ===================== Usuario =====================

    @Override
    public Usuario login(String id, String clave) {
        for (Admin a : data.getAdmins()) {
            if (a.getId().equals(id) && a.getClave().equals(clave)) {
                return a;
            }
        }
        for (Funcionario f : data.getFuncionarios()) {
            if (f.getId().equals(id) && f.getClave().equals(clave)) {
                return f;
            }
        }
        return null;
    }

    @Override
    public void cambiarClave(String id, String claveNueva) {
        for (Admin a : data.getAdmins()) {
            if (a.getId().equals(id)) {
                a.setClave(claveNueva);
                return;
            }
        }
        for (Funcionario f : data.getFuncionarios()) {
            if (f.getId().equals(id)) {
                f.setClave(claveNueva);
                return;
            }
        }
    }

    // ===================== Funcionario =====================

    @Override
    public List<Funcionario> listarFuncionarios() {
        return data.getFuncionarios();
    }

    @Override
    public Funcionario buscarFuncionarioPorId(String id) {
        return data.getFuncionarios().stream()
                .filter(f -> f.getId().equals(id))
                .findFirst().orElse(null);
    }

    @Override
    public List<Funcionario> buscarFuncionariosPorTexto(String texto) {
        String t = texto.toLowerCase();
        return data.getFuncionarios().stream()
                .filter(f -> f.getId().toLowerCase().contains(t) || f.getNombre().toLowerCase().contains(t))
                .collect(Collectors.toList());
    }

    @Override
    public void insertarFuncionario(Funcionario f) throws GlobalException {
        if (buscarFuncionarioPorId(f.getId()) != null) {
            throw new GlobalException("Ya existe un registro con ese identificador");
        }
        data.getFuncionarios().add(f);
    }

    @Override
    public void modificarFuncionario(Funcionario f) throws GlobalException {
        Funcionario existente = buscarFuncionarioPorId(f.getId());
        if (existente == null) {
            throw new GlobalException("Funcionario no encontrado");
        }
        existente.setNombre(f.getNombre());
        existente.setDepartamento(f.getDepartamento());
        existente.setTelefono(f.getTelefono());
    }

    @Override
    public void eliminarFuncionario(String id) throws GlobalException {
        Funcionario existente = buscarFuncionarioPorId(id);
        if (existente == null || !data.getFuncionarios().remove(existente)) {
            throw new GlobalException("Funcionario no encontrado");
        }
    }

    // ===================== Categoria =====================

    @Override
    public List<CategoriaRecurso> listarCategorias() {
        return data.getCategorias();
    }

    @Override
    public CategoriaRecurso buscarCategoriaPorId(String id) {
        return data.getCategorias().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst().orElse(null);
    }

    @Override
    public List<CategoriaRecurso> buscarCategoriasPorTexto(String texto) {
        String t = texto.toLowerCase();
        return data.getCategorias().stream()
                .filter(c -> c.getDescripcion().toLowerCase().contains(t))
                .collect(Collectors.toList());
    }

    @Override
    public String insertarCategoria(String descripcion) {
        String id = data.generarIdCategoria();
        data.getCategorias().add(new CategoriaRecurso(id, descripcion));
        return id;
    }

    @Override
    public void modificarCategoria(CategoriaRecurso c) throws GlobalException {
        CategoriaRecurso existente = buscarCategoriaPorId(c.getId());
        if (existente == null) {
            throw new GlobalException("Categoria no encontrada");
        }
        existente.setDescripcion(c.getDescripcion());
    }

    @Override
    public void eliminarCategoria(String id) throws GlobalException {
        CategoriaRecurso existente = buscarCategoriaPorId(id);
        if (existente == null || !data.getCategorias().remove(existente)) {
            throw new GlobalException("Categoria no encontrada");
        }
    }

    // ===================== Recurso =====================

    @Override
    public List<Recurso> listarRecursos() {
        return data.getRecursos();
    }

    @Override
    public List<Recurso> listarRecursosPorCategoria(String categoriaId) {
        return data.getRecursos().stream()
                .filter(r -> r.getCategoria() != null && r.getCategoria().getId().equals(categoriaId))
                .collect(Collectors.toList());
    }

    @Override
    public Recurso buscarRecursoPorId(String id) {
        return data.getRecursos().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst().orElse(null);
    }

    @Override
    public void insertarRecurso(Recurso r) throws GlobalException {
        if (buscarRecursoPorId(r.getId()) != null) {
            throw new GlobalException("Ya existe un registro con ese identificador");
        }
        data.getRecursos().add(r);
    }

    @Override
    public void modificarRecurso(Recurso r) throws GlobalException {
        Recurso existente = buscarRecursoPorId(r.getId());
        if (existente == null) {
            throw new GlobalException("Recurso no encontrado");
        }
        existente.setDescripcion(r.getDescripcion());
        existente.setCategoria(r.getCategoria());
    }

    @Override
    public void eliminarRecurso(String id) throws GlobalException {
        Recurso existente = buscarRecursoPorId(id);
        if (existente == null || !data.getRecursos().remove(existente)) {
            throw new GlobalException("Recurso no encontrado");
        }
    }

    // ===================== Reserva =====================

    @Override
    public List<Reserva> listarReservas() {
        return data.getReservas();
    }

    @Override
    public Reserva buscarReservaPorId(String id) {
        return data.getReservas().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst().orElse(null);
    }

    @Override
    public List<Reserva> listarReservasPorFuncionario(String funcionarioId) {
        return data.getReservas().stream()
                .filter(r -> r.getFuncionario() != null && r.getFuncionario().getId().equals(funcionarioId))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listarIdsRecursosOcupados(String fecha, String horaInicio, String horaFin) {
        List<String> ocupados = new ArrayList<>();
        for (Reserva r : data.getReservas()) {
            if (r.getEstado() == EstadoReserva.CANCELADA) continue;
            if (!r.getFecha().equals(fecha)) continue;
            if (r.getRecursos() == null || !horariosSolapan(r.getHoraInicio(), r.getHoraFin(), horaInicio, horaFin)) {
                continue;
            }
            for (Recurso rec : r.getRecursos()) {
                ocupados.add(rec.getId());
            }
        }
        return ocupados;
    }

    @Override
    public String siguienteIdReserva() {
        return data.generarIdReserva();
    }

    @Override
    public void crearReserva(Reserva reserva) throws GlobalException {
        crearReservaLock.lock();
        try {
            List<String> ocupados = listarIdsRecursosOcupados(reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());
            for (Recurso r : reserva.getRecursos()) {
                if (ocupados.contains(r.getId())) {
                    throw new GlobalException("No hay disponibilidad para el recurso en ese horario");
                }
            }
            data.getReservas().add(reserva);
        } finally {
            crearReservaLock.unlock();
        }
    }

    @Override
    public void cancelarReserva(String id) throws GlobalException {
        Reserva existente = buscarReservaPorId(id);
        if (existente == null) {
            throw new GlobalException("Reserva no encontrada");
        }
        existente.setEstado(EstadoReserva.CANCELADA);
    }

    // ===================== Estadisticas =====================

    @Override
    public Map<String, Integer> estadisticasRecursos(String desde, String hasta) {
        LocalDate d1 = LocalDate.parse(desde);
        LocalDate d2 = LocalDate.parse(hasta);
        Map<String, Integer> resultado = new LinkedHashMap<>();
        for (Reserva r : data.getReservas()) {
            if (r.getEstado() == EstadoReserva.CANCELADA) continue;
            LocalDate fecha = LocalDate.parse(r.getFecha());
            if (fecha.isBefore(d1) || fecha.isAfter(d2)) continue;
            for (Recurso rec : r.getRecursos()) {
                String categoria = rec.getCategoria() != null ? rec.getCategoria().getDescripcion() : "";
                resultado.merge(categoria, 1, Integer::sum);
            }
        }
        return resultado;
    }

    @Override
    public Map<String, Integer> estadisticasActividades(String desde, String hasta) {
        LocalDate d1 = LocalDate.parse(desde);
        LocalDate d2 = LocalDate.parse(hasta);
        Map<LocalDate, Integer> porSemana = new TreeMap<>();
        for (Reserva r : data.getReservas()) {
            if (r.getEstado() == EstadoReserva.CANCELADA) continue;
            LocalDate fecha = LocalDate.parse(r.getFecha());
            if (fecha.isBefore(d1) || fecha.isAfter(d2)) continue;
            LocalDate lunes = fecha.with(DayOfWeek.MONDAY);
            porSemana.merge(lunes, 1, Integer::sum);
        }
        Map<String, Integer> resultado = new LinkedHashMap<>();
        for (Map.Entry<LocalDate, Integer> entry : porSemana.entrySet()) {
            resultado.put(entry.getKey().toString(), entry.getValue());
        }
        return resultado;
    }

    private static boolean horariosSolapan(String inicioA, String finA, String inicioB, String finB) {
        LocalTime a1 = LocalTime.parse(inicioA, FORMATO_HORA);
        LocalTime a2 = LocalTime.parse(finA, FORMATO_HORA);
        LocalTime b1 = LocalTime.parse(inicioB, FORMATO_HORA);
        LocalTime b2 = LocalTime.parse(finB, FORMATO_HORA);
        return a1.isBefore(b2) && b1.isBefore(a2);
    }
}
