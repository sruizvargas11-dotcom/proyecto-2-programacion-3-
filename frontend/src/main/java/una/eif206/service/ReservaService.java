package una.eif206.service;

import una.eif206.data.Data;
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
import java.util.stream.Collectors;

public class ReservaService {

    private final Data data;

    public ReservaService(Data data) {
        this.data = data;
    }

    public List<Reserva> findAll() {
        return data.getReservas();
    }

    public List<Reserva> findByFuncionario(Funcionario f) {
        return data.getReservas().stream()
                .filter(i -> i.getFuncionario() != null && i.getFuncionario().getId().equals(f.getId()))
                .collect(Collectors.toList());
    }

    public String generarId() {
        return data.generarIdReserva();
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
        Reserva result = data.getReservas().stream()
                .filter(i -> i.getId().equals(e.getId()))
                .findFirst().orElse(null);
        if (result != null) return "Reserva ya existe";
        boolean ocupado = data.getReservas().stream()
                .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                .filter(r -> r.getFecha().equals(e.getFecha()))
                .filter(r -> r.getRecursos() != null &&
                        r.getRecursos().stream().anyMatch(rec -> e.getRecursos().contains(rec)))
                .anyMatch(r -> horariosSolapan(r.getHoraInicio(), r.getHoraFin(), e.getHoraInicio(), e.getHoraFin()));
        if (ocupado) return "No hay disponibilidad para el recurso en ese horario";
        data.getReservas().add(e);
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

    /**
     * Unica copia de la busqueda de disponibilidad (antes duplicada en ReservasController).
     * Reemplaza a ReservasController.buscarRecursoDisponible()/seSolapan(), eliminados del controller.
     */
    public Recurso buscarRecursoDisponible(CategoriaRecurso categoria, String fecha, String horaInicio, String horaFin) {
        for (Recurso recurso : data.getRecursos()) {
            if (!categoria.equals(recurso.getCategoria())) continue;
            if (!recurso.isDisponible()) continue;
            boolean ocupado = data.getReservas().stream()
                    .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
                    .filter(r -> r.getFecha().equals(fecha))
                    .filter(r -> r.getRecursos().contains(recurso))
                    .anyMatch(r -> horariosSolapan(r.getHoraInicio(), r.getHoraFin(), horaInicio, horaFin));
            if (!ocupado) return recurso;
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
        Reserva result = data.getReservas().stream()
                .filter(i -> i.getId().equals(r.getId()))
                .findFirst().orElse(null);
        if (result == null) return "Reserva no encontrada";
        LocalDate fecha = LocalDate.parse(result.getFecha());
        if (!fecha.isAfter(LocalDate.now())) {
            return "Solo se pueden cancelar reservas con fecha futura";
        }
        result.setEstado(EstadoReserva.CANCELADA);
        return null;
    }
}
