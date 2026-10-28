package una.eif206.service;

import una.eif206.data.Data;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Recurso;
import una.eif206.model.enums.EstadoReserva;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class CalendarioService {

    private final Data data;

    public CalendarioService(Data data) {
        this.data = data;
    }

    public List<Recurso> findByCategoria(CategoriaRecurso categoria) {
        return data.getRecursos().stream()
                .filter(r -> r.getCategoria() != null &&
                        r.getCategoria().getId().equals(categoria.getId()))
                .collect(Collectors.toList());
    }

    public String[][] getCalendario(String fecha, CategoriaRecurso categoria) {
        String[] horas = {"06:00","07:00","08:00","09:00","10:00","11:00",
                "12:00","13:00","14:00","15:00","16:00","17:00",
                "18:00","19:00","20:00","21:00","22:00"};
        List<Recurso> recursos = data.getRecursos().stream()
                .filter(r -> r.getCategoria() != null &&
                        r.getCategoria().getId().equals(categoria.getId()))
                .collect(Collectors.toList());
        String[][] matriz = new String[horas.length][recursos.size() + 1];
        for (int i = 0; i < horas.length; i++) { matriz[i][0] = horas[i]; }
        for (una.eif206.model.Reserva r : data.getReservas()) {
            if (!r.getFecha().equals(fecha)) continue;
            if (r.getEstado() != EstadoReserva.ACTIVA) continue;
            for (int col = 0; col < recursos.size(); col++) {
                Recurso rec = recursos.get(col);
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
        } catch (Exception e) { return false; }
    }
}
