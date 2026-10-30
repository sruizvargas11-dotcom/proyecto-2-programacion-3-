package una.eif206.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import una.eif206.client.FakeService;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Reserva;
import una.eif206.util.Sesion;
import una.eif206.view.ReservasView;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de unidad de la creacion de reservas (una sola llamada al servicio). */
class ReservasControllerTest {

    static class ReservasViewDePrueba extends ReservasView {
        String ultimoError;
        String ultimoMensaje;

        @Override public void mostrarError(String m) { ultimoError = m; }
        @Override public void mostrarMensaje(String m) { ultimoMensaje = m; }
    }

    private FakeService service;
    private ReservasViewDePrueba vista;
    private ReservasController controller;
    private List<CategoriaRecurso> soloLaptops;
    private String fechaFutura;

    @BeforeEach
    void setUp() {
        service = new FakeService();
        Sesion.setUsuario(service.findAllFuncionarios().get(0));
        vista = new ReservasViewDePrueba();
        controller = new ReservasController(vista, service);
        soloLaptops = List.of(service.findAllCategorias().get(0));
        fechaFutura = LocalDate.now().plusDays(10).toString();
    }

    @AfterEach
    void tearDown() {
        Sesion.logout();
    }

    @Test
    void crear_reservaQuedaConIdYRecurso() {
        controller.create("Reunion clientes", fechaFutura, "08:00", "10:00", soloLaptops);
        assertNull(vista.ultimoError);
        assertEquals("RESERVA CREADA", vista.ultimoMensaje);
        Reserva creada = service.findAllReservas().get(0);
        assertFalse(creada.getId().isEmpty());
        assertEquals(1, creada.getRecursos().size());
    }

    @Test
    void terceraReservaIgual_noHayLaptopsLibres() {
        controller.create("Uno", fechaFutura, "08:00", "10:00", soloLaptops);
        controller.create("Dos", fechaFutura, "08:00", "10:00", soloLaptops);
        controller.create("Tres", fechaFutura, "08:00", "10:00", soloLaptops);
        assertNotNull(vista.ultimoError);
        assertTrue(vista.ultimoError.startsWith("No hay recursos disponibles"));
        assertEquals(2, service.findAllReservas().size());
    }

    @Test
    void horarioInvalido_daError() {
        controller.create("Mal horario", fechaFutura, "10:00", "08:00", soloLaptops);
        assertNotNull(vista.ultimoError);
        assertTrue(service.findAllReservas().isEmpty());
    }

    @Test
    void elAdministradorNoPuedeCrearReservas() {
        Sesion.setUsuario(service.login("admin", "admin123"));
        controller.create("Reunion", fechaFutura, "08:00", "10:00", soloLaptops);
        assertEquals("Solo un funcionario puede crear reservas", vista.ultimoError);
    }
}