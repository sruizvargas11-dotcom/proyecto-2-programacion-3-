package una.eif206.service;

import una.eif206.data.Data;
import una.eif206.model.Reserva;
import una.eif206.model.enums.EstadoReserva;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class ActividadesService {

    public static final String[] NOMBRES_DIAS = {
            "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"
    };

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Data data;

    public ActividadesService(Data data) {
        this.data = data;
    }

    public Map<String, Map<String, String>> getActividadesSemana(String fechaReferencia) throws Exception {
        LocalDate referencia = LocalDate.parse(fechaReferencia, FORMATO_FECHA);
        LocalDate lunes = referencia.with(DayOfWeek.MONDAY);
        LocalDate domingo = lunes.plusDays(6);
        Map<String, Map<String, String>> matriz = new LinkedHashMap<>();
        for (Reserva reserva : data.getReservas()) {
            if (reserva.getEstado() == EstadoReserva.CANCELADA) continue;
            LocalDate fecha;
            try { fecha = LocalDate.parse(reserva.getFecha(), FORMATO_FECHA); }
            catch (Exception ex) { continue; }
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
}
