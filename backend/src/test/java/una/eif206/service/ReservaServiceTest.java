package una.eif206.service;

import una.eif206.data.MemoriaRepositorio;
import una.eif206.data.Repositorio;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.enums.EstadoReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReservaServiceTest {

    private ReservaService reservaService;
    private FuncionarioService funcionarioService;
    private CategoriaService categoriaService;
    private RecursoService recursoService;

    private Funcionario funcionario;
    private Recurso recurso;

    @BeforeEach
    void setUp() {
        Repositorio repositorio = new MemoriaRepositorio();
        reservaService = new ReservaService(repositorio);
        funcionarioService = new FuncionarioService(repositorio);
        categoriaService = new CategoriaService(repositorio);
        recursoService = new RecursoService(repositorio);

        funcionario = funcionarioService.findById("111");
        CategoriaRecurso categoria = categoriaService.findAll().stream()
                .filter(c -> c.getId().equals("CAT-000002")).findFirst().orElseThrow();
        recurso = recursoService.findByCategoria(categoria).get(0);
    }

    private Reserva nuevaReserva(String actividad, String fecha, String horaInicio, String horaFin) {
        Reserva r = new Reserva(reservaService.generarId(), actividad, fecha, horaInicio, horaFin, funcionario);
        r.setRecursos(List.of(recurso));
        return r;
    }

    @Test
    void crearReservaSinSolapamiento() {
        String error = reservaService.create(nuevaReserva("Reunion", "2026-02-10", "09:00", "10:00"));
        assertNull(error);
        assertEquals(1, reservaService.findAll().size());
    }

    @Test
    void crearReservaConSolapamientoMismoRecurso() {
        reservaService.create(nuevaReserva("Reunion A", "2026-02-10", "09:00", "11:00"));

        String error = reservaService.create(nuevaReserva("Reunion B", "2026-02-10", "10:00", "12:00"));

        assertNotNull(error);
    }

    @Test
    void crearSegundaReservaHorarioDiferente() {
        reservaService.create(nuevaReserva("Reunion A", "2026-02-10", "09:00", "10:00"));

        String error = reservaService.create(nuevaReserva("Reunion B", "2026-02-10", "10:00", "11:00"));

        assertNull(error);
        assertEquals(2, reservaService.findAll().size());
    }

    @Test
    void cancelarReservaActiva() {
        Reserva reserva = nuevaReserva("Reunion", LocalDate.now().plusDays(7).toString(), "09:00", "10:00");
        reservaService.create(reserva);

        String error = reservaService.cancelar(reserva);

        assertNull(error);
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
    }

    @Test
    void cancelarReservaYaCandelada() {
        Reserva reserva = nuevaReserva("Reunion", LocalDate.now().plusDays(7).toString(), "09:00", "10:00");
        reservaService.create(reserva);

        reservaService.cancelar(reserva);
        String segundoIntento = reservaService.cancelar(reserva);

        assertNull(segundoIntento);
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
    }

    @Test
    void soloReservasDelFuncionarioEnSesion() {
        Funcionario otro = funcionarioService.findById("222");
        Reserva propia = nuevaReserva("Reunion Propia", "2026-02-10", "09:00", "10:00");
        Reserva ajena = new Reserva(reservaService.generarId(), "Reunion Ajena", "2026-02-10", "11:00", "12:00", otro);
        ajena.setRecursos(List.of(recurso));
        reservaService.create(propia);
        reservaService.create(ajena);

        List<Reserva> reservasDeFuncionario = reservaService.findByFuncionario(funcionario);

        assertTrue(reservasDeFuncionario.stream().anyMatch(r -> r.getId().equals(propia.getId())));
        assertTrue(reservasDeFuncionario.stream().noneMatch(r -> r.getId().equals(ajena.getId())));
    }

    @Test
    void reservaApareceConEstadoActiva() {
        Reserva reserva = nuevaReserva("Reunion", "2026-02-10", "09:00", "10:00");
        reservaService.create(reserva);

        Reserva encontrada = reservaService.findAll().stream()
                .filter(r -> r.getId().equals(reserva.getId())).findFirst().orElseThrow();
        assertEquals(EstadoReserva.ACTIVA, encontrada.getEstado());
    }

    @Test
    void horaInicioMayorQueHoraFin_error() {
        String error = reservaService.create(nuevaReserva("Reunion", "2026-02-10", "10:00", "09:00"));
        assertNotNull(error);
    }

    @Test
    void crearReservaSinActividad_error() {
        Reserva reserva = new Reserva(reservaService.generarId(), "", "2026-02-10", "09:00", "10:00", funcionario);
        reserva.setRecursos(List.of(recurso));
        String error = reservaService.create(reserva);
        assertNotNull(error);
    }

    @Test
    void crearReservaSinCategoria_error() {
        Reserva reserva = new Reserva(reservaService.generarId(), "Reunion", "2026-02-10", "09:00", "10:00", funcionario);
        String error = reservaService.create(reserva);
        assertNotNull(error);
    }

    @Test
    void tablaReservaConColumnaRecursos() {
        Reserva reserva = nuevaReserva("Reunion", "2026-02-10", "09:00", "10:00");

        String error = reservaService.create(reserva);

        assertNull(error);
        assertFalse(reserva.getRecursos().isEmpty());
    }

    @Test
    void errorMuestraTodasCategoriasNoDisponibles() {
        CategoriaRecurso categoriaConRecurso = categoriaService.findAll().stream()
                .filter(c -> c.getId().equals("CAT-000002")).findFirst().orElseThrow();
        CategoriaRecurso categoriaSinRecursos = categoriaService.findAll().stream()
                .filter(c -> c.getId().equals("CAT-000003")).findFirst().orElseThrow();

        Recurso salaDeJuntas = recursoService.findByCategoria(categoriaConRecurso).get(0);
        Reserva ocupante = new Reserva(reservaService.generarId(), "Ocupa sala", "2026-04-01", "09:00", "10:00", funcionario);
        ocupante.setRecursos(List.of(salaDeJuntas));
        reservaService.create(ocupante);

        List<Recurso> recursosAsignados = new ArrayList<>();
        String error = reservaService.asignarRecursosDisponibles(
                List.of(categoriaConRecurso, categoriaSinRecursos), "2026-04-01", "09:00", "10:00", recursosAsignados);

        assertNotNull(error);
        assertTrue(error.contains(categoriaConRecurso.getDescripcion()));
        assertTrue(error.contains(categoriaSinRecursos.getDescripcion()));
    }

    @Test
    void noPuedeCancelarReservaPasada() {
        String ayer = LocalDate.now().minusDays(1).toString();
        Reserva reserva = nuevaReserva("Reunion pasada", ayer, "09:00", "10:00");
        reservaService.create(reserva);

        String error = reservaService.cancelar(reserva);

        assertNotNull(error);
    }

    @Test
    void puedeCancelarReservaFutura() {
        String manana = LocalDate.now().plusDays(1).toString();
        Reserva reserva = nuevaReserva("Reunion futura", manana, "09:00", "10:00");
        reservaService.create(reserva);

        String error = reservaService.cancelar(reserva);

        assertNull(error);
    }
}
