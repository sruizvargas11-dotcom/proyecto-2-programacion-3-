package una.eif206.client;

import una.eif206.model.Admin;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.ReservaExtraccion;
import una.eif206.model.Usuario;
import una.eif206.model.enums.EstadoReserva;
import una.eif206.protocol.IService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Implementacion de IService con datos en memoria, usada por el frontend
 * mientras no existe conexion real al backend (Server/Worker, Etapa 3).
 * Semilla: 1 admin, 1 funcionario, 2 categorias, 3 recursos.
 */
public class FakeService implements IService {

    private static final String[] NOMBRES_DIAS = {
            "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"
    };

    private final List<Admin> admins = new ArrayList<>();
    private final List<Funcionario> funcionarios = new ArrayList<>();
    private final List<CategoriaRecurso> categorias = new ArrayList<>();
    private final List<Recurso> recursos = new ArrayList<>();
    private final List<Reserva> reservas = new ArrayList<>();

    private int categoriaCounter = 3;
    private int reservaCounter = 1;

    public FakeService() {
        admins.add(new Admin("admin", "admin123"));

        funcionarios.add(new Funcionario("111", "Juan Perez", "TI", "3323"));

        CategoriaRecurso catLaptops = new CategoriaRecurso("CAT-000001", "Laptop windows");
        CategoriaRecurso catSalas = new CategoriaRecurso("CAT-000002", "Sala de Juntas");
        categorias.add(catLaptops);
        categorias.add(catSalas);

        recursos.add(new Recurso("238715", "Laptop #238715", catLaptops));
        recursos.add(new Recurso("45238", "Laptop #45238", catLaptops));
        recursos.add(new Recurso("34343", "Sala 1 primer piso", catSalas));
    }

    private String generarIdCategoria() {
        return String.format("CAT-%06d", ++categoriaCounter);
    }

    private String generarIdReservaInterno() {
        return String.format("RES-%06d", reservaCounter++);
    }

    // ===================== UsuarioService =====================

    @Override
    public Usuario login(String id, String clave) {
        for (Admin a : admins) {
            if (a.getId().equals(id) && a.getClave().equals(clave)) return a;
        }
        for (Funcionario f : funcionarios) {
            if (f.getId().equals(id) && f.getClave().equals(clave)) return f;
        }
        return null;
    }

    @Override
    public String cambiarClave(Usuario u, String claveActual, String claveNueva) {
        if (!u.getClave().equals(claveActual)) {
            return "Clave actual incorrecta";
        }
        u.setClave(claveNueva);
        return null;
    }

    // ===================== FuncionarioService =====================

    @Override
    public List<Funcionario> findAllFuncionarios() {
        return funcionarios;
    }

    @Override
    public List<Funcionario> search(Funcionario e) {
        return funcionarios.stream()
                .filter(i -> i.getNombre().toLowerCase().contains(e.getNombre().toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Funcionario> search(String texto) {
        Funcionario porId = findFuncionarioById(texto);
        if (porId != null) {
            return List.of(porId);
        }
        Funcionario filtro = new Funcionario();
        filtro.setNombre(texto);
        return search(filtro);
    }

    @Override
    public String create(Funcionario e) {
        if (e.getId() == null || e.getId().trim().isEmpty()
                || e.getNombre() == null || e.getNombre().trim().isEmpty()) {
            return "El ID y el nombre son obligatorios";
        }
        if (findFuncionarioById(e.getId()) != null) return "Funcionario ya existe";
        if (e.getClave() == null || e.getClave().isEmpty()) e.setClave(e.getId());
        funcionarios.add(e);
        return null;
    }

    @Override
    public String update(Funcionario f) {
        Funcionario result = findFuncionarioById(f.getId());
        if (result == null) return "Funcionario no encontrado";
        result.setNombre(f.getNombre());
        result.setTelefono(f.getTelefono());
        result.setDepartamento(f.getDepartamento());
        return null;
    }

    @Override
    public String delete(Funcionario e) {
        if (!funcionarios.remove(e)) return "Funcionario no encontrado";
        return null;
    }

    @Override
    public Funcionario findFuncionarioById(String id) {
        return funcionarios.stream().filter(i -> i.getId().equals(id)).findFirst().orElse(null);
    }

    // ===================== CategoriaService =====================

    @Override
    public List<CategoriaRecurso> findAllCategorias() {
        return categorias;
    }

    @Override
    public CategoriaRecurso findCategoriaById(String id) {
        return categorias.stream().filter(i -> i.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public List<CategoriaRecurso> search(CategoriaRecurso e) {
        return categorias.stream()
                .filter(i -> i.getDescripcion().toLowerCase().contains(e.getDescripcion().toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public String create(CategoriaRecurso e) {
        e.setId(generarIdCategoria());
        categorias.add(e);
        return null;
    }

    @Override
    public String update(CategoriaRecurso c) {
        CategoriaRecurso result = findCategoriaById(c.getId());
        if (result == null) return "Categoria no encontrada";
        result.setDescripcion(c.getDescripcion());
        return null;
    }

    @Override
    public String delete(CategoriaRecurso e) {
        boolean tieneRecursosAsociados = recursos.stream()
                .anyMatch(r -> r.getCategoria() != null && r.getCategoria().getId().equals(e.getId()));
        if (tieneRecursosAsociados) {
            return "No se puede eliminar: existen recursos asociados.";
        }
        if (!categorias.remove(e)) return "Categoria no encontrada";
        return null;
    }

    // ===================== RecursoService =====================

    @Override
    public List<Recurso> findAllRecursos() {
        return recursos;
    }

    @Override
    public List<Recurso> findByCategoria(CategoriaRecurso categoria) {
        return recursos.stream()
                .filter(r -> r.getCategoria() != null && r.getCategoria().getId().equals(categoria.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public String create(Recurso e) {
        if (e.getCategoria() == null) {
            return "Debe asignar una categoria al recurso";
        }
        if (recursos.stream().anyMatch(i -> i.getId().equals(e.getId()))) return "Recurso ya existe";
        recursos.add(e);
        return null;
    }

    @Override
    public String update(Recurso r) {
        Recurso result = recursos.stream().filter(i -> i.getId().equals(r.getId())).findFirst().orElse(null);
        if (result == null) return "Recurso no encontrado";
        result.setDescripcion(r.getDescripcion());
        result.setCategoria(r.getCategoria());
        return null;
    }

    @Override
    public String delete(Recurso e) {
        if (!recursos.remove(e)) return "Recurso no encontrado";
        return null;
    }

    // ===================== ReservaService =====================

    @Override
    public List<Reserva> findAllReservas() {
        return reservas;
    }

    @Override
    public List<Reserva> findByFuncionario(Funcionario f) {
        return reservas.stream()
                .filter(i -> i.getFuncionario() != null && i.getFuncionario().getId().equals(f.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public String generarId() {
        return generarIdReservaInterno();
    }

    @Override
    public String create(Reserva e) {
        if (e.getActividad() == null || e.getActividad().trim().isEmpty()) {
            return "La actividad es obligatoria";
        }
        if (e.getRecursos() == null || e.getRecursos().isEmpty()) {
            return "Debe asignar al menos un recurso a la reserva";
        }
        if (!horarioValido(e.getHoraInicio(), e.getHoraFin())) {
            return "La hora de inicio debe ser anterior a la hora de fin";
        }
        for (Recurso r : e.getRecursos()) {
            if (!tieneId(r) && r.getCategoria() == null) {
                return "Cada recurso debe traer su id o, si no, una categoria para elegir uno disponible";
            }
        }
        if (tieneId(e.getId()) && reservas.stream().anyMatch(i -> i.getId().equals(e.getId()))) {
            return "Reserva ya existe";
        }

        // Igual que el Backend real: un Recurso con id vacio y con categoria
        // significa "cualquier recurso libre de esa categoria".
        List<Recurso> resueltos = new ArrayList<>();
        List<String> categoriasSinDisponibilidad = new ArrayList<>();
        for (Recurso solicitado : e.getRecursos()) {
            if (tieneId(solicitado)) {
                if (recursoOcupado(solicitado.getId(), e.getFecha(), e.getHoraInicio(), e.getHoraFin())
                        || contieneRecurso(resueltos, solicitado.getId())) {
                    return "No hay disponibilidad para el recurso en ese horario";
                }
                resueltos.add(solicitado);
                continue;
            }
            Recurso libre = buscarLibre(solicitado.getCategoria(), e.getFecha(),
                    e.getHoraInicio(), e.getHoraFin(), resueltos);
            if (libre == null) {
                categoriasSinDisponibilidad.add(solicitado.getCategoria().getDescripcion());
            } else {
                resueltos.add(libre);
            }
        }
        if (!categoriasSinDisponibilidad.isEmpty()) {
            return "No hay recursos disponibles para: " + String.join(", ", categoriasSinDisponibilidad);
        }

        if (!tieneId(e.getId())) {
            e.setId(generarIdReservaInterno());
        }
        e.setRecursos(resueltos);
        reservas.add(e);
        return null;
    }

    private static boolean tieneId(Recurso r) {
        return tieneId(r.getId());
    }

    private static boolean tieneId(String id) {
        return id != null && !id.trim().isEmpty();
    }

    private static boolean contieneRecurso(List<Recurso> lista, String idRecurso) {
        return lista.stream().anyMatch(r -> idRecurso.equals(r.getId()));
    }

    private boolean recursoOcupado(String idRecurso, String fecha, String horaInicio, String horaFin) {
        return reservas.stream()
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                .filter(r -> r.getFecha().equals(fecha))
                .filter(r -> r.getRecursos() != null && contieneRecurso(r.getRecursos(), idRecurso))
                .anyMatch(r -> horariosSolapan(r.getHoraInicio(), r.getHoraFin(), horaInicio, horaFin));
    }

    private Recurso buscarLibre(CategoriaRecurso categoria, String fecha, String horaInicio,
                                String horaFin, List<Recurso> yaElegidos) {
        for (Recurso candidato : recursos) {
            if (candidato.getCategoria() == null
                    || !candidato.getCategoria().getId().equals(categoria.getId())) continue;
            if (!candidato.isDisponible()) continue;
            if (contieneRecurso(yaElegidos, candidato.getId())) continue;
            if (recursoOcupado(candidato.getId(), fecha, horaInicio, horaFin)) continue;
            return candidato;
        }
        return null;
    }

    @Override
    public Recurso buscarRecursoDisponible(CategoriaRecurso categoria, String fecha, String horaInicio, String horaFin) {
        for (Recurso recurso : recursos) {
            if (!categoria.equals(recurso.getCategoria())) continue;
            if (!recurso.isDisponible()) continue;
            boolean ocupado = reservas.stream()
                    .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                    .filter(r -> r.getFecha().equals(fecha))
                    .filter(r -> r.getRecursos().contains(recurso))
                    .anyMatch(r -> horariosSolapan(r.getHoraInicio(), r.getHoraFin(), horaInicio, horaFin));
            if (!ocupado) return recurso;
        }
        return null;
    }

    @Override
    public String asignarRecursosDisponibles(List<CategoriaRecurso> categoriasSolicitadas, String fecha,
                                             String horaInicio, String horaFin, List<Recurso> recursosAsignados) {
        List<String> categoriasSinDisponibilidad = new ArrayList<>();
        for (CategoriaRecurso categoria : categoriasSolicitadas) {
            Recurso disponible = buscarRecursoDisponible(categoria, fecha, horaInicio, horaFin);
            if (disponible == null) {
                categoriasSinDisponibilidad.add(categoria.getDescripcion());
            } else {
                recursosAsignados.add(disponible);
            }
        }
        if (!categoriasSinDisponibilidad.isEmpty()) {
            return "No hay recursos disponibles para: " + String.join(", ", categoriasSinDisponibilidad);
        }
        return null;
    }

    @Override
    public String cancelar(Reserva r) {
        Reserva result = reservas.stream().filter(i -> i.getId().equals(r.getId())).findFirst().orElse(null);
        if (result == null) return "Reserva no encontrada";
        LocalDate fecha = LocalDate.parse(result.getFecha());
        if (!fecha.isAfter(LocalDate.now())) {
            return "Solo se pueden cancelar reservas con fecha futura";
        }
        result.setEstado(EstadoReserva.CANCELADA);
        return null;
    }

    private boolean horarioValido(String horaInicio, String horaFin) {
        try {
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("H:mm");
            LocalTime inicio = LocalTime.parse(horaInicio, formato);
            LocalTime fin = LocalTime.parse(horaFin, formato);
            return inicio.isBefore(fin);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean horariosSolapan(String inicioA, String finA, String inicioB, String finB) {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("H:mm");
        LocalTime a1 = LocalTime.parse(inicioA, formato);
        LocalTime a2 = LocalTime.parse(finA, formato);
        LocalTime b1 = LocalTime.parse(inicioB, formato);
        LocalTime b2 = LocalTime.parse(finB, formato);
        return a1.isBefore(b2) && b1.isBefore(a2);
    }

    // ===================== CalendarioService =====================

    @Override
    public String[][] getCalendario(String fecha, CategoriaRecurso categoria) {
        String[] horas = {"06:00", "07:00", "08:00", "09:00", "10:00", "11:00",
                "12:00", "13:00", "14:00", "15:00", "16:00", "17:00",
                "18:00", "19:00", "20:00", "21:00", "22:00"};
        List<Recurso> recursosCategoria = findByCategoria(categoria);
        String[][] matriz = new String[horas.length][recursosCategoria.size() + 1];
        for (int i = 0; i < horas.length; i++) {
            matriz[i][0] = horas[i];
        }
        for (Reserva r : reservas) {
            if (!r.getFecha().equals(fecha)) continue;
            if (r.getEstado() != EstadoReserva.ACTIVA) continue;
            for (int col = 0; col < recursosCategoria.size(); col++) {
                Recurso rec = recursosCategoria.get(col);
                boolean usaRecurso = r.getRecursos() != null &&
                        r.getRecursos().stream().anyMatch(rv -> rv.getId().equals(rec.getId()));
                if (!usaRecurso) continue;
                for (int i = 0; i < horas.length; i++) {
                    if (horaEnRango(horas[i], r.getHoraInicio(), r.getHoraFin())) {
                        matriz[i][col + 1] = r.getActividad() + " - " +
                                (r.getFuncionario() != null ? r.getFuncionario().getNombre() : "");
                    }
                }
            }
        }
        return matriz;
    }

    private boolean horaEnRango(String hora, String inicio, String fin) {
        try {
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("H:mm");
            LocalTime h = LocalTime.parse(hora, formato);
            LocalTime i = LocalTime.parse(inicio, formato);
            LocalTime f = LocalTime.parse(fin, formato);
            return !h.isBefore(i) && h.isBefore(f);
        } catch (Exception e) {
            return false;
        }
    }

    // ===================== ActividadesService =====================

    @Override
    public Map<String, Map<String, String>> getActividadesSemana(String fechaReferencia) throws Exception {
        DateTimeFormatter formatoFecha = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate referencia = LocalDate.parse(fechaReferencia, formatoFecha);
        LocalDate lunes = referencia.with(DayOfWeek.MONDAY);
        LocalDate domingo = lunes.plusDays(6);
        Map<String, Map<String, String>> matriz = new LinkedHashMap<>();
        for (Reserva reserva : reservas) {
            if (reserva.getEstado() == EstadoReserva.CANCELADA) continue;
            LocalDate fecha;
            try {
                fecha = LocalDate.parse(reserva.getFecha(), formatoFecha);
            } catch (Exception ex) {
                continue;
            }
            if (fecha.isBefore(lunes) || fecha.isAfter(domingo)) continue;
            String dia = NOMBRES_DIAS[fecha.getDayOfWeek().getValue() - 1];
            Map<String, String> fila = matriz.computeIfAbsent(reserva.getHoraInicio(), k -> new LinkedHashMap<>());
            String textoExistente = fila.get(dia);
            String funcionarioNombre = reserva.getFuncionario() != null ? reserva.getFuncionario().getNombre() : "";
            String textoNuevo = reserva.getActividad() + " - " + funcionarioNombre;
            if (textoExistente != null && !textoExistente.isEmpty()) {
                textoNuevo = textoExistente + "; " + textoNuevo;
            }
            fila.put(dia, textoNuevo);
        }
        return matriz;
    }

    // ===================== EstadisticasService =====================

    @Override
    public Map<String, Integer> getEstadisticasRecursos(String desde, String hasta) throws Exception {
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
    public Map<String, Integer> getEstadisticasActividades(String desde, String hasta) throws Exception {
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

    // ===================== ReservaIAService =====================

    @Override
    public ReservaExtraccion extraerReserva(String frase) throws Exception {
        throw new Exception("La extraccion con IA no esta disponible en el modo de datos en memoria (FakeService)");
    }
}