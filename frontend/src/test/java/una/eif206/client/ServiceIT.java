package una.eif206.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.Usuario;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integracion del Proxy (Service): sockets reales contra un servidor
 * que habla el mismo protocolo que el Backend.
 */
class ServiceIT {

    private ServidorDePrueba servidor;
    private int puerto;
    private Service service;

    @BeforeEach
    void setUp() throws IOException {
        servidor = new ServidorDePrueba();
        puerto = servidor.iniciar();
        service = new Service("localhost", puerto);
    }

    @AfterEach
    void tearDown() throws IOException {
        service.cerrar();
        servidor.detener();
    }

    @Test
    void handshake_recibeUnSid() {
        assertNotNull(service.getSid());
        assertFalse(service.getSid().isEmpty());
    }

    @Test
    void login_porRed() {
        Usuario u = service.login("111", "111");
        assertNotNull(u);
        assertEquals("Juan Perez", u.getNombre());
        assertNull(service.login("111", "mala"));
    }

    @Test
    void listaDeCategorias_llegaCompleta() {
        List<CategoriaRecurso> categorias = service.findAllCategorias();
        assertEquals(2, categorias.size());
    }

    @Test
    void crearReserva_porRed_elServidorAsignaIdYRecurso() {
        Reserva r = new Reserva("", "Reunion", LocalDate.now().plusDays(5).toString(), "08:00", "10:00",
                service.findAllFuncionarios().get(0));
        r.getRecursos().add(new Recurso("", "", service.findAllCategorias().get(0)));
        assertNull(service.create(r));
        Reserva guardada = service.findAllReservas().get(0);
        assertFalse(guardada.getId().isEmpty());
        assertFalse(guardada.getRecursos().get(0).getId().isEmpty());
    }

    @Test
    void errorEnElServidor_llegaComoExcepcionConMensaje() {
        Exception e = assertThrows(IllegalStateException.class, () -> service.extraerReserva("hola"));
        assertTrue(e.getMessage().contains("IA"));
    }

    @Test
    void generarId_noSeExponePorRed() {
        assertThrows(UnsupportedOperationException.class, () -> service.generarId());
    }

    @Test
    void servidorApagado_avisaConexionPerdida() throws Exception {
        CountDownLatch avisado = new CountDownLatch(1);
        service.setAlPerderConexion(avisado::countDown);
        servidor.detener();
        Thread.sleep(200);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> service.findAllCategorias());
        assertTrue(e.getMessage().startsWith("Se perdio la conexion"));
        assertTrue(avisado.await(3, TimeUnit.SECONDS), "debe avisar que se perdio la conexion");
        assertFalse(service.isConectado());
    }

    @Test
    void sinServidor_noSePuedeConectar() throws IOException {
        servidor.detener();
        IOException e = assertThrows(IOException.class, () -> new Service("localhost", puerto));
        assertTrue(e.getMessage().startsWith("No se pudo conectar"));
    }
}
