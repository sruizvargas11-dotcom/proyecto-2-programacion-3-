package una.eif206.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de unidad del FakeService (datos en memoria).
 * Verifican que se comporte igual que el Backend real, para que las demas
 * pruebas del Frontend sean confiables sin necesitar el servidor.
 */
class FakeServiceTest {

    private FakeService service;
    private Funcionario juan;
    private CategoriaRecurso laptops;
    private String fechaFutura;

    @BeforeEach
    void setUp() {
        service = new FakeService();
        juan = service.findAllFuncionarios().get(0);
        laptops = service.findAllCategorias().get(0);
        fechaFutura = LocalDate.now().plusDays(10).toString();
    }

    private Reserva reservaDeLaptop(String actividad) {
        Reserva r = new Reserva("", actividad, fechaFutura, "08:00", "10:00", juan);
        r.getRecursos().add(new Recurso("", "", laptops));
        return r;
    }

    @Test
    void loginCorrecto() {
        assertNotNull(service.login("111", "111"));
    }

    @Test
    void loginConClaveIncorrecta() {
        assertNull(service.login("111", "mala"));
    }

    @Test
    void crearFuncionario_laClaveInicialEsElId() {
        assertNull(service.create(new Funcionario("333", "Ana Lopez", "TI", "8888")));
        assertNotNull(service.login("333", "333"));
    }

    @Test
    void crearReserva_generaIdYAsignaUnRecursoDeLaCategoria() {
        Reserva r = reservaDeLaptop("Reunion");
        assertNull(service.create(r));
        assertFalse(r.getId().isEmpty(), "el servidor debe generar el id");
        assertEquals(1, r.getRecursos().size());
        assertEquals(laptops.getId(), r.getRecursos().get(0).getCategoria().getId());
    }

    @Test
    void dosReservasMismaHora_usanLaptopsDistintas() {
        Reserva r1 = reservaDeLaptop("Primera");
        Reserva r2 = reservaDeLaptop("Segunda");
        service.create(r1);
        service.create(r2);
        assertNotEquals(r1.getRecursos().get(0).getId(), r2.getRecursos().get(0).getId());
    }

    @Test
    void sinRecursosLibres_daError() {
        service.create(reservaDeLaptop("Primera"));
        service.create(reservaDeLaptop("Segunda"));
        String error = service.create(reservaDeLaptop("Tercera"));
        assertNotNull(error);
        assertTrue(error.startsWith("No hay recursos disponibles"));
    }

    @Test
    void horaInicioDespuesDeHoraFin_daError() {
        Reserva r = reservaDeLaptop("Mal horario");
        r.setHoraInicio("10:00");
        r.setHoraFin("08:00");
        assertNotNull(service.create(r));
    }

    @Test
    void noSePuedeCancelarUnaReservaPasada() {
        Reserva r = reservaDeLaptop("Pasada");
        r.setFecha(LocalDate.now().minusDays(1).toString());
        service.create(r);
        assertEquals("Solo se pueden cancelar reservas con fecha futura", service.cancelar(r));
    }

    @Test
    void noSePuedeBorrarCategoriaConRecursos() {
        assertNotNull(service.delete(laptops));
    }
}