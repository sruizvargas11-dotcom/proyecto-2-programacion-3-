package una.eif206.service;

import una.eif206.data.Data;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.enums.EstadoReserva;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class EstadisticasService {

    private final Data data;

    public EstadisticasService(Data data) {
        this.data = data;
    }

    public Map<String, Integer> getEstadisticasRecursos(String desde, String hasta) throws Exception {
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

    public Map<String, Integer> getEstadisticasActividades(String desde, String hasta) throws Exception {
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
}
