package una.eif206.service;

import una.eif206.data.MemoriaRepositorio;
import una.eif206.data.Repositorio;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.enums.EstadoReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de ActividadesService (antes tenia solo 1). Usa
 * MemoriaRepositorio real a traves de los demas *Service, igual que
 * CalendarioServiceTest/EstadisticasServiceTest, para que las reservas de
 * prueba pasen por las mismas validaciones que en produccion.
 */
public class ActividadesServiceTest {

    private ActividadesService actividadesService;
    private ReservaService reservaService;
    private FuncionarioService funcionarioService;
    private RecursoService recursoService;

    private Funcionario funcionario;
    private Funcionario otroFuncionario;
    private Recurso recurso;
    private Recurso otroRecurso;

    private LocalDate lunesSemana;
    private LocalDate domingoSemana;

    @BeforeEach
    void setUp() {
        Repositorio repositorio = new MemoriaRepositorio();
        actividadesService = new ActividadesService(repositorio);
        reservaService = new ReservaService(repositorio);
        funcionarioService = new FuncionarioService(repositorio);
        recursoService = new RecursoService(repositorio);

        funcionario = funcionarioService.findById("111");
        otroFuncionario = funcionarioService.findById("222");
        List<Recurso> recursos = recursoService.findAll();
        recurso = recursos.get(0);
        otroRecurso = recursos.get(1);

        // Semana fija (no depende de "hoy"): lunes de la semana que contiene 2031-03-03.
        lunesSemana = LocalDate.of(2031, 3, 3).with(DayOfWeek.MONDAY);
        domingoSemana = lunesSemana.plusDays(6);
    }

    private Reserva reservar(String actividad, LocalDate fecha, String horaInicio, String horaFin,
                              Funcionario func, Recurso rec) {
        Reserva r = new Reserva(reservaService.generarId(), actividad, fecha.toString(), horaInicio, horaFin, func);
        r.setRecursos(List.of(rec));
        String error = reservaService.create(r);
        assertNull(error, "la reserva de prueba deberia crearse sin error: " + error);
        return r;
    }

    // ===================== caso ya existente =====================

    @Test
    void celdaMuestraActividadYFuncionario() throws Exception {
        Reserva reserva = reservar("Capacitacion", lunesSemana, "09:00", "10:00", funcionario, recurso);

        Map<String, Map<String, String>> matriz = actividadesService.getActividadesSemana(lunesSemana.toString());

        String celda = matriz.get("09:00").get("LUNES");
        assertNotNull(celda);
        assertTrue(celda.contains(" - "));
        assertTrue(celda.contains("Capacitacion"));
        assertTrue(celda.contains(funcionario.getNombre()));
        assertFalse(reserva.getId().isBlank());
    }

    // ===================== semana sin reservas =====================

    @Test
    void semanaSinReservasDaUnaMatrizVacia() throws Exception {
        Map<String, Map<String, String>> matriz = actividadesService.getActividadesSemana(lunesSemana.toString());

        assertTrue(matriz.isEmpty());
    }

    // ===================== varias reservas del mismo dia =====================

    @Test
    void dosReservasMismoDiaYMismaHoraSeFusionanEnUnaSolaCelda() throws Exception {
        reservar("Reunion A", lunesSemana, "09:00", "10:00", funcionario, recurso);
        reservar("Reunion B", lunesSemana, "09:00", "10:00", otroFuncionario, otroRecurso);

        Map<String, Map<String, String>> matriz = actividadesService.getActividadesSemana(lunesSemana.toString());

        String celda = matriz.get("09:00").get("LUNES");
        assertNotNull(celda);
        assertTrue(celda.contains("Reunion A - " + funcionario.getNombre()));
        assertTrue(celda.contains("Reunion B - " + otroFuncionario.getNombre()));
        assertTrue(celda.contains(";"), "las dos reservas en la misma celda deben separarse con '; '");
    }

    @Test
    void dosReservasMismoDiaDistintaHoraQuedanEnFilasDistintas() throws Exception {
        reservar("Reunion temprano", lunesSemana, "09:00", "10:00", funcionario, recurso);
        reservar("Reunion tarde", lunesSemana, "15:00", "16:00", funcionario, recurso);

        Map<String, Map<String, String>> matriz = actividadesService.getActividadesSemana(lunesSemana.toString());

        assertEquals("Reunion temprano - " + funcionario.getNombre(), matriz.get("09:00").get("LUNES"));
        assertEquals("Reunion tarde - " + funcionario.getNombre(), matriz.get("15:00").get("LUNES"));
    }

    @Test
    void reservasEnDiasDistintosQuedanEnColumnasDistintas() throws Exception {
        reservar("Reunion lunes", lunesSemana, "09:00", "10:00", funcionario, recurso);
        reservar("Reunion miercoles", lunesSemana.plusDays(2), "09:00", "10:00", funcionario, recurso);

        Map<String, Map<String, String>> matriz = actividadesService.getActividadesSemana(lunesSemana.toString());

        assertEquals("Reunion lunes - " + funcionario.getNombre(), matriz.get("09:00").get("LUNES"));
        assertEquals("Reunion miercoles - " + funcionario.getNombre(), matriz.get("09:00").get("MIERCOLES"));
    }

    // ===================== reservas canceladas excluidas =====================

    @Test
    void reservaCanceladaNoApareceEnLaSemana() throws Exception {
        Reserva reserva = reservar("Reunion a cancelar", lunesSemana, "09:00", "10:00", funcionario, recurso);
        reserva.setEstado(EstadoReserva.CANCELADA);

        Map<String, Map<String, String>> matriz = actividadesService.getActividadesSemana(lunesSemana.toString());

        assertTrue(matriz.isEmpty());
    }

    @Test
    void reservaCanceladaNoOcultaOtraReservaActivaEnLaMismaCelda() throws Exception {
        Reserva cancelada = reservar("Reunion cancelada", lunesSemana, "09:00", "10:00", funcionario, recurso);
        cancelada.setEstado(EstadoReserva.CANCELADA);
        reservar("Reunion activa", lunesSemana, "09:00", "10:00", otroFuncionario, otroRecurso);

        Map<String, Map<String, String>> matriz = actividadesService.getActividadesSemana(lunesSemana.toString());

        String celda = matriz.get("09:00").get("LUNES");
        assertEquals("Reunion activa - " + otroFuncionario.getNombre(), celda);
    }

    /**
     * Documenta un hallazgo, no corregido (la tarea pide no tocar codigo de
     * produccion): ActividadesService solo excluye EstadoReserva.CANCELADA
     * (`if (reserva.getEstado() == CANCELADA) continue;`), asi que una
     * reserva COMPLETADA SI aparece en la semana. CalendarioService hace lo
     * opuesto (`if (estado != ACTIVA) continue;`): ahi una COMPLETADA NO
     * aparece. Las dos vistas tratan el mismo estado de forma inconsistente.
     * Hoy esto no se manifiesta en produccion porque ningun codigo actual
     * pone una reserva en COMPLETADA.
     */
    @Test
    void reservaCompletadaSiApareceEnLaSemana_inconsistenteConCalendarioService() throws Exception {
        Reserva completada = reservar("Reunion completada", lunesSemana, "09:00", "10:00", funcionario, recurso);
        completada.setEstado(EstadoReserva.COMPLETADA);

        Map<String, Map<String, String>> matriz = actividadesService.getActividadesSemana(lunesSemana.toString());

        assertEquals("Reunion completada - " + funcionario.getNombre(), matriz.get("09:00").get("LUNES"));
    }

    // ===================== fecha de referencia nula o invalida =====================

    /**
     * Documenta el comportamiento actual, no corregido: a diferencia de
     * CalendarioService (que nunca parsea `fecha` como LocalDate, solo
     * compara strings y por eso nunca lanza con una fecha invalida),
     * ActividadesService SI hace `LocalDate.parse(fechaReferencia, ...)` sin
     * try/catch propio. Con fechaReferencia null, Objects.requireNonNull
     * dentro de LocalDate.parse lanza NullPointerException.
     */
    @Test
    void fechaReferenciaNulaLanzaExcepcionEnVezDeDevolverVacio() {
        assertThrows(RuntimeException.class, () -> actividadesService.getActividadesSemana(null));
    }

    /** Mismo hallazgo que el caso null: un texto que no es una fecha ISO tambien revienta el parse. */
    @Test
    void fechaReferenciaConFormatoInvalidoLanzaExcepcionEnVezDeDevolverVacio() {
        assertThrows(RuntimeException.class, () -> actividadesService.getActividadesSemana("no-es-una-fecha"));
    }

    // ===================== limites de la semana =====================

    @Test
    void reservaElLunesDeLaSemanaApareceEnElLimiteInferior() throws Exception {
        reservar("Reunion lunes", lunesSemana, "09:00", "10:00", funcionario, recurso);

        Map<String, Map<String, String>> matriz = actividadesService.getActividadesSemana(lunesSemana.toString());

        assertEquals("Reunion lunes - " + funcionario.getNombre(), matriz.get("09:00").get("LUNES"));
    }

    @Test
    void reservaElDomingoDeLaSemanaApareceEnElLimiteSuperior() throws Exception {
        reservar("Reunion domingo", domingoSemana, "09:00", "10:00", funcionario, recurso);

        Map<String, Map<String, String>> matriz = actividadesService.getActividadesSemana(lunesSemana.toString());

        assertEquals("Reunion domingo - " + funcionario.getNombre(), matriz.get("09:00").get("DOMINGO"));
    }

    @Test
    void reservaElDomingoAnteriorNoApareceEnLaSemana() throws Exception {
        reservar("Reunion semana pasada", lunesSemana.minusDays(1), "09:00", "10:00", funcionario, recurso);

        Map<String, Map<String, String>> matriz = actividadesService.getActividadesSemana(lunesSemana.toString());

        assertTrue(matriz.isEmpty());
    }

    @Test
    void reservaElLunesSiguienteNoApareceEnLaSemana() throws Exception {
        reservar("Reunion semana siguiente", domingoSemana.plusDays(1), "09:00", "10:00", funcionario, recurso);

        Map<String, Map<String, String>> matriz = actividadesService.getActividadesSemana(lunesSemana.toString());

        assertTrue(matriz.isEmpty());
    }

    @Test
    void fechaReferenciaAMitadDeSemanaUsaLaMismaVentanaLunesADomingo() throws Exception {
        reservar("Reunion lunes", lunesSemana, "09:00", "10:00", funcionario, recurso);
        reservar("Reunion domingo", domingoSemana, "11:00", "12:00", otroFuncionario, otroRecurso);

        LocalDate miercoles = lunesSemana.plusDays(2);
        Map<String, Map<String, String>> matriz = actividadesService.getActividadesSemana(miercoles.toString());

        assertEquals("Reunion lunes - " + funcionario.getNombre(), matriz.get("09:00").get("LUNES"));
        assertEquals("Reunion domingo - " + otroFuncionario.getNombre(), matriz.get("11:00").get("DOMINGO"));
    }
}
