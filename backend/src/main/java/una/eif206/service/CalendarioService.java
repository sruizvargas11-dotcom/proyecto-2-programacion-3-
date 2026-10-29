package una.eif206.service;

import una.eif206.data.Repositorio;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.enums.EstadoReserva;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CalendarioService {

    private final Repositorio repositorio;

    public CalendarioService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    public List<Recurso> findByCategoria(CategoriaRecurso categoria) {
        return repositorio.listarRecursosPorCategoria(categoria.getId());
    }

    public String[][] getCalendario(String fecha, CategoriaRecurso categoria) {
        String[] horas = {"06:00", "07:00", "08:00", "09:00", "10:00", "11:00",
                "12:00", "13:00", "14:00", "15:00", "16:00", "17:00",
                "18:00", "19:00", "20:00", "21:00", "22:00"};
        List<Recurso> recursos = repositorio.listarRecursosPorCategoria(categoria.getId());
        String[][] matriz = new String[horas.length][recursos.size() + 1];
        for (int i = 0; i < horas.length; i++) {
            matriz[i][0] = horas[i];
        }
        for (Reserva r : repositorio.listarReservas()) {
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
        } catch (Exception e) {
            return false;
        }
    }
}
