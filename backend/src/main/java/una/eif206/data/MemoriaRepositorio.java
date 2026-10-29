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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Repositorio solo en memoria: reutiliza la logica de validacion/consulta que
 * antes vivia directamente en los *Service contra Data (P1, eliminado junto
 * con el resto del XML en la Etapa 4). Usado por los tests (ServicioXTest no
 * la usa, pero RepositorioContractTest, los *ServiceTest y la prueba de
 * concurrencia si) y como reemplazo de Oracle cuando no hay base de datos
 * disponible.
 */
public class MemoriaRepositorio implements Repositorio {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("H:mm");

    private final List<Admin> admins;
    private final List<Funcionario> funcionarios;
    private final List<CategoriaRecurso> categorias;
    private final List<Recurso> recursos;
    private final List<Reserva> reservas;
    private int categoriaCounter = 4;
    private int reservaCounter = 1;

    private final Lock crearReservaLock = new ReentrantLock();

    public MemoriaRepositorio() {
        admins = new ArrayList<>();
        admins.add(new Admin("admin", "admin123"));

        funcionarios = new ArrayList<>();
        funcionarios.add(new Funcionario("111", "Juan Perez", "TI", "3323"));
        funcionarios.add(new Funcionario("222", "Maria Lopez", "RRHH", "4444"));

        categorias = new ArrayList<>();
        categorias.add(new CategoriaRecurso("CAT-000001", "Laptop windows"));
        categorias.add(new CategoriaRecurso("CAT-000002", "Sala de Juntas"));
        categorias.add(new CategoriaRecurso("CAT-000003", "Sala para 10 personas"));

        recursos = new ArrayList<>();
        recursos.add(new Recurso("238715", "Laptop #238715", categorias.get(0)));
        recursos.add(new Recurso("45238", "Laptop #45238", categorias.get(0)));
        recursos.add(new Recurso("34343", "Sala 1 primer piso", categorias.get(1)));

        reservas = new ArrayList<>();
    }

    /**
     * Para pruebas: agrega una categoria directo a la lista en memoria, sin
     * pasar por create() (sin generar id ni validar). Util para armar
     * fixtures que la API publica no permite construir directamente.
     */
    public void insertarCategoriaDirecta(CategoriaRecurso categoria) {
        categorias.add(categoria);
    }

    /**
     * Para pruebas: agrega un recurso directo a la lista en memoria, sin
     * pasar por insertarRecurso() (sin validar duplicados).
     */
    public void insertarRecursoDirecto(Recurso recurso) {
        recursos.add(recurso);
    }

    /**
     * Para pruebas: agrega una reserva directo a la lista en memoria, sin
     * pasar por crearReserva() (sin generar id, resolver categoria ni
     * validar disponibilidad). Unica forma de reproducir en una prueba un
     * estado que el flujo normal no deja alcanzar (p.ej. dos reservas
     * ACTIVAS solapadas en el mismo recurso, u horas malformadas).
     */
    public void insertarReservaDirecta(Reserva reserva) {
        reservas.add(reserva);
    }

    // ===================== Usuario =====================

    @Override
    public Usuario login(String id, String clave) {
        for (Admin a : admins) {
            if (a.getId().equals(id) && a.getClave().equals(clave)) {
                return a;
            }
        }
        for (Funcionario f : funcionarios) {
            if (f.getId().equals(id) && f.getClave().equals(clave)) {
                return f;
            }
        }
        return null;
    }

    @Override
    public void cambiarClave(String id, String claveNueva) {
        for (Admin a : admins) {
            if (a.getId().equals(id)) {
                a.setClave(claveNueva);
                return;
            }
        }
        for (Funcionario f : funcionarios) {
            if (f.getId().equals(id)) {
                f.setClave(claveNueva);
                return;
            }
        }
    }

    // ===================== Funcionario =====================

    @Override
    public List<Funcionario> listarFuncionarios() {
        return funcionarios;
    }

    @Override
    public Funcionario buscarFuncionarioPorId(String id) {
        return funcionarios.stream()
                .filter(f -> f.getId().equals(id))
                .findFirst().orElse(null);
    }

    @Override
    public List<Funcionario> buscarFuncionariosPorTexto(String texto) {
        String t = texto.toLowerCase();
        return funcionarios.stream()
                .filter(f -> f.getId().toLowerCase().contains(t) || f.getNombre().toLowerCase().contains(t))
                .collect(Collectors.toList());
    }

    @Override
    public void insertarFuncionario(Funcionario f) throws GlobalException {
        if (buscarFuncionarioPorId(f.getId()) != null) {
            throw new GlobalException("Ya existe un registro con ese identificador");
        }
        funcionarios.add(f);
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
        if (existente == null || !funcionarios.remove(existente)) {
            throw new GlobalException("Funcionario no encontrado");
        }
    }

    // ===================== Categoria =====================

    @Override
    public List<CategoriaRecurso> listarCategorias() {
        return categorias;
    }

    @Override
    public CategoriaRecurso buscarCategoriaPorId(String id) {
        return categorias.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst().orElse(null);
    }

    @Override
    public List<CategoriaRecurso> buscarCategoriasPorTexto(String texto) {
        String t = texto.toLowerCase();
        return categorias.stream()
                .filter(c -> c.getDescripcion().toLowerCase().contains(t))
                .collect(Collectors.toList());
    }

    @Override
    public String insertarCategoria(String descripcion) {
        String id = generarIdCategoria();
        categorias.add(new CategoriaRecurso(id, descripcion));
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
        if (existente == null || !categorias.remove(existente)) {
            throw new GlobalException("Categoria no encontrada");
        }
    }

    // ===================== Recurso =====================

    @Override
    public List<Recurso> listarRecursos() {
        return recursos;
    }

    @Override
    public List<Recurso> listarRecursosPorCategoria(String categoriaId) {
        return recursos.stream()
                .filter(r -> r.getCategoria() != null && r.getCategoria().getId().equals(categoriaId))
                .collect(Collectors.toList());
    }

    @Override
    public Recurso buscarRecursoPorId(String id) {
        return recursos.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst().orElse(null);
    }

    @Override
    public void insertarRecurso(Recurso r) throws GlobalException {
        if (buscarRecursoPorId(r.getId()) != null) {
            throw new GlobalException("Ya existe un registro con ese identificador");
        }
        recursos.add(r);
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
        if (existente == null || !recursos.remove(existente)) {
            throw new GlobalException("Recurso no encontrado");
        }
    }

    // ===================== Reserva =====================

    @Override
    public List<Reserva> listarReservas() {
        return reservas;
    }

    @Override
    public Reserva buscarReservaPorId(String id) {
        return reservas.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst().orElse(null);
    }

    @Override
    public List<Reserva> listarReservasPorFuncionario(String funcionarioId) {
        return reservas.stream()
                .filter(r -> r.getFuncionario() != null && r.getFuncionario().getId().equals(funcionarioId))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listarIdsRecursosOcupados(String fecha, String horaInicio, String horaFin) {
        List<String> ocupados = new ArrayList<>();
        for (Reserva r : reservas) {
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
        return generarIdReserva();
    }

    /**
     * Genera el id (si viene vacio), resuelve cada Recurso solicitado solo
     * por categoria (id vacio + categoria presente) a un recurso libre real
     * de esa categoria, valida disponibilidad de los que ya traen id, e
     * inserta la reserva: todo dentro del mismo crearReservaLock, para que
     * dos hilos pidiendo la misma categoria/horario nunca reciban el mismo
     * recurso (equivalente a ServicioReserva.crear contra Oracle, que hace
     * lo mismo dentro de su CREAR_RESERVA_LOCK + transaccion).
     */
    @Override
    public void crearReserva(Reserva reserva) throws GlobalException {
        crearReservaLock.lock();
        try {
            if (reserva.getId() == null || reserva.getId().trim().isEmpty()) {
                reserva.setId(generarIdReserva());
            }
            List<String> ocupados = listarIdsRecursosOcupados(reserva.getFecha(), reserva.getHoraInicio(), reserva.getHoraFin());
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
                Recurso libre = buscarRecursoLibrePorCategoria(solicitado.getCategoria(), usados);
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
            reserva.setRecursos(resueltos);
            reservas.add(reserva);
        } finally {
            crearReservaLock.unlock();
        }
    }

    private static boolean tieneId(Recurso r) {
        return r.getId() != null && !r.getId().trim().isEmpty();
    }

    private Recurso buscarRecursoLibrePorCategoria(CategoriaRecurso categoria, Set<String> usados) {
        for (Recurso candidato : recursos) {
            if (candidato.getCategoria() == null || !candidato.getCategoria().getId().equals(categoria.getId())) {
                continue;
            }
            if (!candidato.isDisponible() || usados.contains(candidato.getId())) {
                continue;
            }
            return candidato;
        }
        return null;
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
        for (Reserva r : reservas) {
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
        for (Reserva r : reservas) {
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

    private String generarIdCategoria() {
        return String.format("CAT-%06d", categoriaCounter++);
    }

    private String generarIdReserva() {
        return String.format("RES-%06d", reservaCounter++);
    }

    private static boolean horariosSolapan(String inicioA, String finA, String inicioB, String finB) {
        LocalTime a1 = LocalTime.parse(inicioA, FORMATO_HORA);
        LocalTime a2 = LocalTime.parse(finA, FORMATO_HORA);
        LocalTime b1 = LocalTime.parse(inicioB, FORMATO_HORA);
        LocalTime b2 = LocalTime.parse(finB, FORMATO_HORA);
        return a1.isBefore(b2) && b1.isBefore(a2);
    }
}
