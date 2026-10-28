package una.eif206.service;

import una.eif206.data.Data;
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

public class ActividadesServiceTest {

    private ActividadesService actividadesService;
    private ReservaService reservaService;
    private FuncionarioService funcionarioService;
    private RecursoService recursoService;

    private Funcionario funcionario;
    private Recurso recurso;

    @BeforeEach
    void setUp() {
        Data data = new Data();
        actividadesService = new ActividadesService(data);
        reservaService = new ReservaService(data);
        funcionarioService = new FuncionarioService(data);
        recursoService = new RecursoService(data);

        funcionario = funcionarioService.findById("111");
        recurso = recursoService.findAll().get(0);
    }

    @Test
    void celdaMuestraActividadYFuncionario() throws Exception {
        LocalDate lunes = LocalDate.now().with(DayOfWeek.MONDAY);
        Reserva reserva = new Reserva(reservaService.generarId(), "Capacitacion", lunes.toString(), "09:00", "10:00", funcionario);
        reserva.setRecursos(List.of(recurso));
        reservaService.create(reserva);

        Map<String, Map<String, String>> matriz = actividadesService.getActividadesSemana(lunes.toString());

        String celda = matriz.get("09:00").get("LUNES");
        assertNotNull(celda);
        assertTrue(celda.contains(" - "));
        assertTrue(celda.contains("Capacitacion"));
        assertTrue(celda.contains(funcionario.getNombre()));
    }
}
