package una.eif206.service;

import una.eif206.data.GlobalException;
import una.eif206.data.NoDataException;
import una.eif206.data.Repositorio;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.enums.EstadoReserva;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReservaService {

    private final Repositorio repositorio;

    public ReservaService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    public List<Reserva> findAll() {
        return repositorio.listarReservas();
    }

    public List<Reserva> findByFuncionario(Funcionario f) {
        return repositorio.listarReservasPorFuncionario(f.getId());
    }

    public String generarId() {
        return repositorio.siguienteIdReserva();
    }

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
        if (repositorio.buscarReservaPorId(e.getId()) != null) {
            return "Reserva ya existe";
        }
        try {
            repositorio.crearReserva(e);
        } catch (GlobalException | NoDataException ex) {
            return ex.getMessage();
        }
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

    /**
     * Unica copia de la busqueda de disponibilidad (antes duplicada en ReservasController).
     */
    public Recurso buscarRecursoDisponible(CategoriaRecurso categoria, String fecha, String horaInicio, String horaFin) {
        List<String> ocupados = repositorio.listarIdsRecursosOcupados(fecha, horaInicio, horaFin);
        for (Recurso recurso : repositorio.listarRecursosPorCategoria(categoria.getId())) {
            if (!recurso.isDisponible()) continue;
            if (ocupados.contains(recurso.getId())) continue;
            return recurso;
        }
        return null;
    }

    /**
     * Busca el primer recurso disponible para cada categoria solicitada.
     * Los recursos asignados se agregan a recursosAsignados; si alguna
     * categoria no tiene recurso disponible, retorna un error indicando
     * TODAS las categorias sin disponibilidad.
     */
    public String asignarRecursosDisponibles(List<CategoriaRecurso> categorias, String fecha,
                                              String horaInicio, String horaFin, List<Recurso> recursosAsignados) {
        List<String> categoriasSinDisponibilidad = new ArrayList<>();
        for (CategoriaRecurso categoria : categorias) {
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

    public String cancelar(Reserva r) {
        Reserva existente = repositorio.buscarReservaPorId(r.getId());
        if (existente == null) {
            return "Reserva no encontrada";
        }
        LocalDate fecha = LocalDate.parse(existente.getFecha());
        if (!fecha.isAfter(LocalDate.now())) {
            return "Solo se pueden cancelar reservas con fecha futura";
        }
        try {
            repositorio.cancelarReserva(existente.getId());
        } catch (GlobalException | NoDataException ex) {
            return ex.getMessage();
        }
        r.setEstado(EstadoReserva.CANCELADA);
        existente.setEstado(EstadoReserva.CANCELADA);
        return null;
    }
}
