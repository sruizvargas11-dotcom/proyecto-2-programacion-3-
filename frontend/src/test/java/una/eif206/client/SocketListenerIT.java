package una.eif206.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de integracion de las ALERTAS: dos clientes, cada uno con su socket
 * SYNC (Service) y su socket ASYNC (SocketListener).
 */
class SocketListenerIT {

    /** Recibe las alertas como lo haria AlertaController. */
    static class Receptor implements ThreadListener {
        final List<Reserva> recibidas = new CopyOnWriteArrayList<>();
        volatile boolean siempreEnHiloDeSwing = true;
        volatile CountDownLatch llego = new CountDownLatch(1);

        @Override
        public void deliver_message(Reserva reserva) {
            if (!SwingUtilities.isEventDispatchThread()) siempreEnHiloDeSwing = false;
            recibidas.add(reserva);
            llego.countDown();
        }
    }

    private ServidorDePrueba servidor;
    private Service serviceA, serviceB;
    private SocketListener listenerA, listenerB;
    private Receptor receptorA, receptorB;

    @BeforeEach
    void setUp() throws Exception {
        servidor = new ServidorDePrueba();
        int puerto = servidor.iniciar();
        serviceA = new Service("localhost", puerto);
        serviceB = new Service("localhost", puerto);
        listenerA = new SocketListener("localhost", puerto, serviceA.getSid());
        listenerB = new SocketListener("localhost", puerto, serviceB.getSid());
        listenerA.start();
        listenerB.start();
        receptorA = new Receptor();
        receptorB = new Receptor();
        SwingUtilities.invokeAndWait(() -> {
            listenerA.setDestino(receptorA);
            listenerB.setDestino(receptorB);
        });
        Thread.sleep(200); // que el servidor registre los dos sockets ASYNC
    }

    @AfterEach
    void tearDown() throws IOException {
        listenerA.stop();
        listenerB.stop();
        serviceA.cerrar();
        serviceB.cerrar();
        servidor.detener();
    }

    private Reserva nuevaReserva(Service s, String actividad) {
        Reserva r = new Reserva("", actividad, LocalDate.now().plusDays(5).toString(), "08:00", "10:00",
                s.findAllFuncionarios().get(0));
        r.getRecursos().add(new Recurso("", "", s.findAllCategorias().get(1)));
        return r;
    }

    @Test
    void elOtroClienteRecibeLaAlerta_yElCreadorNo() throws Exception {
        assertNull(serviceB.create(nuevaReserva(serviceB, "Reunion clientes")));

        assertTrue(receptorA.llego.await(3, TimeUnit.SECONDS), "A debe recibir la alerta");
        Reserva alerta = receptorA.recibidas.get(0);
        assertEquals("Reunion clientes", alerta.getActividad());
        assertNotNull(alerta.getFuncionario().getNombre());
        assertFalse(alerta.getRecursos().isEmpty());
        assertTrue(receptorA.siempreEnHiloDeSwing, "debe entregarse con invokeLater");

        Thread.sleep(300);
        assertTrue(receptorB.recibidas.isEmpty(), "el creador no recibe su propia alerta");
    }

    @Test
    void sinSesion_lasAlertasNoSeEntregan() throws Exception {
        SwingUtilities.invokeAndWait(() -> listenerA.setDestino(null));
        serviceB.create(nuevaReserva(serviceB, "Durante el cierre de sesion"));
        Thread.sleep(500);
        assertTrue(receptorA.recibidas.isEmpty());
    }

    @Test
    void stop_noSeConsideraConexionPerdida() throws Exception {
        CountDownLatch avisado = new CountDownLatch(1);
        listenerA.setAlPerderConexion(avisado::countDown);
        listenerA.stop();
        assertFalse(avisado.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void siSeCaeElServidor_elListenerAvisa() throws Exception {
        CountDownLatch avisado = new CountDownLatch(1);
        listenerA.setAlPerderConexion(avisado::countDown);
        servidor.detener();
        assertTrue(avisado.await(3, TimeUnit.SECONDS));
    }
}
