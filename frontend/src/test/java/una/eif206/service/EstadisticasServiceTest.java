package una.eif206.service;

import una.eif206.data.Data;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EstadisticasServiceTest {

    private EstadisticasService estadisticasService;
    private ReservaService reservaService;
    private FuncionarioService funcionarioService;
    private CategoriaService categoriaService;
    private RecursoService recursoService;

    private Funcionario funcionario;

    @BeforeEach
    void setUp() {
        Data data = new Data();
        estadisticasService = new EstadisticasService(data);
        reservaService = new ReservaService(data);
        funcionarioService = new FuncionarioService(data);
        categoriaService = new CategoriaService(data);
        recursoService = new RecursoService(data);

        funcionario = funcionarioService.findById("111");
    }

    @Test
    void recursosAgrupadosPorCategoria() throws Exception {
        CategoriaRecurso categoria = categoriaService.findAll().stream()
                .filter(c -> c.getId().equals("CAT-000001")).findFirst().orElseThrow();
        List<Recurso> recursosCategoria = recursoService.findByCategoria(categoria);
        assertEquals(2, recursosCategoria.size());

        Reserva r1 = new Reserva(reservaService.generarId(), "Uso laptop 1", "2026-05-01", "09:00", "10:00", funcionario);
        r1.setRecursos(List.of(recursosCategoria.get(0)));
        reservaService.create(r1);

        Reserva r2 = new Reserva(reservaService.generarId(), "Uso laptop 2", "2026-05-01", "11:00", "12:00", funcionario);
        r2.setRecursos(List.of(recursosCategoria.get(1)));
        reservaService.create(r2);

        Map<String, Integer> estadisticas = estadisticasService.getEstadisticasRecursos("2026-05-01", "2026-05-01");

        assertEquals(1, estadisticas.size());
        assertEquals(2, estadisticas.get(categoria.getDescripcion()));
    }

    @Test
    void actividadesAgrupadasPorSemana() throws Exception {
        Recurso recurso = recursoService.findAll().get(0);

        LocalDate lunesSemana1 = LocalDate.of(2026, 6, 1).with(DayOfWeek.MONDAY);
        LocalDate fechaSemana1a = lunesSemana1;
        LocalDate fechaSemana1b = lunesSemana1.plusDays(2);
        LocalDate fechaSemana2 = lunesSemana1.plusWeeks(1).plusDays(1);

        Reserva r1 = new Reserva(reservaService.generarId(), "Reunion 1", fechaSemana1a.toString(), "09:00", "10:00", funcionario);
        r1.setRecursos(List.of(recurso));
        reservaService.create(r1);

        Reserva r2 = new Reserva(reservaService.generarId(), "Reunion 2", fechaSemana1b.toString(), "11:00", "12:00", funcionario);
        r2.setRecursos(List.of(recurso));
        reservaService.create(r2);

        Reserva r3 = new Reserva(reservaService.generarId(), "Reunion 3", fechaSemana2.toString(), "09:00", "10:00", funcionario);
        r3.setRecursos(List.of(recurso));
        reservaService.create(r3);

        Map<String, Integer> estadisticas = estadisticasService.getEstadisticasActividades(
                lunesSemana1.toString(), fechaSemana2.plusDays(6).toString());

        assertEquals(2, estadisticas.size());
        assertEquals(2, estadisticas.get(lunesSemana1.toString()));
        assertEquals(1, estadisticas.get(lunesSemana1.plusWeeks(1).toString()));
    }
}
