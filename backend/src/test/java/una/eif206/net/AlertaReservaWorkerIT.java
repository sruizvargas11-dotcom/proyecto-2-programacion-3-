package una.eif206.net;

import una.eif206.data.MemoriaRepositorio;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.protocol.Protocol;
import una.eif206.service.Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * IT por socket (sin Oracle: Service usa MemoriaRepositorio) de la alerta que
 * el Worker difunde por el canal ASYNC de los demas clientes cuando
 * RESERVA_CREATE tiene exito: DELIVER_MESSAGE + la Reserva con el Funcionario
 * y los Recurso completos, nunca al creador, y sin que un cliente ASYNC roto
 * tumbe la difusion a los demas.
 */
class AlertaReservaWorkerIT {

    private static final Funcionario JUAN = new Funcionario("111", "Juan Perez", "TI", "3323");
    private static final CategoriaRecurso CAT_LAPTOPS = new CategoriaRecurso("CAT-000001", "Laptop windows");
    private static final Recurso LAPTOP_1 = new Recurso("238715", "Laptop #238715", CAT_LAPTOPS);
    private static final Recurso LAPTOP_2 = new Recurso("45238", "Laptop #45238", CAT_LAPTOPS);

    private Server server;
    private int puerto;

    @BeforeEach
    void arrancarServidor() throws IOException {
        server = new Server(0, new Service(new MemoriaRepositorio()));
        server.start();
        puerto = server.getPuertoLocal();
    }

    @AfterEach
    void detenerServidor() throws IOException {
        server.close();
    }

    private static Reserva nuevaReserva(String id, Recurso recurso) {
        Reserva reserva = new Reserva(id, "Reunion de prueba", "2031-01-01", "09:00", "10:00", JUAN);
        reserva.setRecursos(List.of(recurso));
        return reserva;
    }

    /** Espera a que el join() del canal ASYNC ya quedo registrado en el Worker del sid. */
    private void esperarCanalAsync(String sid) throws InterruptedException {
        long limite = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < limite) {
            Worker w = server.buscarWorkerPorSid(sid);
            if (w != null && w.tieneCanalAsync()) {
                return;
            }
            Thread.sleep(10);
        }
        fail("El canal ASYNC del sid " + sid + " nunca quedo registrado");
    }

    private void esperarHasta(java.util.function.BooleanSupplier condicion) throws InterruptedException {
        long limite = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < limite) {
            if (condicion.getAsBoolean()) {
                return;
            }
            Thread.sleep(10);
        }
    }

    private static void crearReservaExitosa(ClienteSyncDePrueba cliente, Reserva reserva) throws Exception {
        cliente.enviar(Protocol.RESERVA_CREATE, reserva);
        assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
        assertNull(cliente.in.readObject());
    }

    private static void asertarAlertaCompleta(Reserva alerta, String idEsperado) {
        assertEquals(idEsperado, alerta.getId());
        assertEquals("Juan Perez", alerta.getFuncionario().getNombre());
        assertEquals(1, alerta.getRecursos().size());
        assertTrue(alerta.getRecursos().get(0).getDescripcion().startsWith("Laptop #"));
    }

    @Test
    void conDosClientesSoloElSegundoRecibeLaAlertaCompleta() throws Exception {
        try (ClienteSyncDePrueba creador = new ClienteSyncDePrueba(puerto);
             ClienteSyncDePrueba receptor = new ClienteSyncDePrueba(puerto)) {
            String sidCreador = creador.handshakeSync();
            String sidReceptor = receptor.handshakeSync();

            try (ClienteAsyncDePrueba asyncCreador = new ClienteAsyncDePrueba(puerto, sidCreador);
                 ClienteAsyncDePrueba asyncReceptor = new ClienteAsyncDePrueba(puerto, sidReceptor)) {
                esperarCanalAsync(sidCreador);
                esperarCanalAsync(sidReceptor);

                crearReservaExitosa(creador, nuevaReserva("RES-ALERTA-1", LAPTOP_1));

                assertEquals(Protocol.DELIVER_MESSAGE, asyncReceptor.in.readInt());
                Reserva alerta = (Reserva) asyncReceptor.in.readObject();
                asertarAlertaCompleta(alerta, "RES-ALERTA-1");

                asyncCreador.socket.setSoTimeout(300);
                assertThrows(SocketTimeoutException.class, () -> asyncCreador.in.readInt());
            }
        }
    }

    @Test
    void conTresClientesLosDosQueNoCreanRecibenLaAlerta() throws Exception {
        try (ClienteSyncDePrueba creador = new ClienteSyncDePrueba(puerto);
             ClienteSyncDePrueba receptorB = new ClienteSyncDePrueba(puerto);
             ClienteSyncDePrueba receptorC = new ClienteSyncDePrueba(puerto)) {
            String sidCreador = creador.handshakeSync();
            String sidB = receptorB.handshakeSync();
            String sidC = receptorC.handshakeSync();

            try (ClienteAsyncDePrueba asyncCreador = new ClienteAsyncDePrueba(puerto, sidCreador);
                 ClienteAsyncDePrueba asyncB = new ClienteAsyncDePrueba(puerto, sidB);
                 ClienteAsyncDePrueba asyncC = new ClienteAsyncDePrueba(puerto, sidC)) {
                esperarCanalAsync(sidCreador);
                esperarCanalAsync(sidB);
                esperarCanalAsync(sidC);

                crearReservaExitosa(creador, nuevaReserva("RES-ALERTA-2", LAPTOP_1));

                assertEquals(Protocol.DELIVER_MESSAGE, asyncB.in.readInt());
                asertarAlertaCompleta((Reserva) asyncB.in.readObject(), "RES-ALERTA-2");

                assertEquals(Protocol.DELIVER_MESSAGE, asyncC.in.readInt());
                asertarAlertaCompleta((Reserva) asyncC.in.readObject(), "RES-ALERTA-2");

                asyncCreador.socket.setSoTimeout(300);
                assertThrows(SocketTimeoutException.class, () -> asyncCreador.in.readInt());
            }
        }
    }

    @Test
    void desconexionAbruptaDeUnReceptorNoAfectaAlCreadorNiALosDemas() throws Exception {
        try (ClienteSyncDePrueba creador = new ClienteSyncDePrueba(puerto);
             ClienteSyncDePrueba receptorRoto = new ClienteSyncDePrueba(puerto);
             ClienteSyncDePrueba receptorSano = new ClienteSyncDePrueba(puerto)) {
            creador.handshakeSync();
            String sidRoto = receptorRoto.handshakeSync();
            String sidSano = receptorSano.handshakeSync();

            ClienteAsyncDePrueba asyncRoto = new ClienteAsyncDePrueba(puerto, sidRoto);
            try (ClienteAsyncDePrueba asyncSano = new ClienteAsyncDePrueba(puerto, sidSano)) {
                esperarCanalAsync(sidRoto);
                esperarCanalAsync(sidSano);

                // Desconexion abrupta: se cierra SOLO el socket ASYNC del
                // receptor roto (su socket SYNC sigue vivo), simulando un
                // cliente cuyo canal de notificaciones se cayo.
                asyncRoto.socket.close();

                crearReservaExitosa(creador, nuevaReserva("RES-ALERTA-3", LAPTOP_1));

                assertEquals(Protocol.DELIVER_MESSAGE, asyncSano.in.readInt());
                asertarAlertaCompleta((Reserva) asyncSano.in.readObject(), "RES-ALERTA-3");

                // El Worker del receptor roto se quita de la lista (su
                // propio hilo SYNC todavia esta vivo, pero ya no es
                // candidato para futuras difusiones).
                esperarHasta(() -> server.cantidadWorkers() == 2);
                assertEquals(2, server.cantidadWorkers());
            }
        }
    }

    @Test
    void dosReservasCasiSimultaneasLleganCompletasYSinCorromperse() throws Exception {
        try (ClienteSyncDePrueba creadorA = new ClienteSyncDePrueba(puerto);
             ClienteSyncDePrueba creadorB = new ClienteSyncDePrueba(puerto);
             ClienteSyncDePrueba receptor = new ClienteSyncDePrueba(puerto)) {
            String sidA = creadorA.handshakeSync();
            String sidB = creadorB.handshakeSync();
            String sidReceptor = receptor.handshakeSync();

            try (ClienteAsyncDePrueba asyncA = new ClienteAsyncDePrueba(puerto, sidA);
                 ClienteAsyncDePrueba asyncB = new ClienteAsyncDePrueba(puerto, sidB);
                 ClienteAsyncDePrueba asyncReceptor = new ClienteAsyncDePrueba(puerto, sidReceptor)) {
                esperarCanalAsync(sidA);
                esperarCanalAsync(sidB);
                esperarCanalAsync(sidReceptor);

                CountDownLatch salida = new CountDownLatch(1);
                ExecutorService executor = Executors.newFixedThreadPool(2);
                AtomicReference<Exception> errorA = new AtomicReference<>();
                AtomicReference<Exception> errorB = new AtomicReference<>();

                executor.submit(() -> {
                    try {
                        salida.await();
                        crearReservaExitosa(creadorA, nuevaReserva("RES-ALERTA-SIM-A", LAPTOP_1));
                    } catch (Exception e) {
                        errorA.set(e);
                    }
                });
                executor.submit(() -> {
                    try {
                        salida.await();
                        crearReservaExitosa(creadorB, nuevaReserva("RES-ALERTA-SIM-B", LAPTOP_2));
                    } catch (Exception e) {
                        errorB.set(e);
                    }
                });

                salida.countDown();
                executor.shutdown();
                assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
                if (errorA.get() != null) throw errorA.get();
                if (errorB.get() != null) throw errorB.get();

                // El receptor (que no crea ninguna reserva) ve las DOS
                // alertas completas y sin corromperse, sin importar el orden.
                Set<String> idsRecibidos = new HashSet<>();
                for (int i = 0; i < 2; i++) {
                    assertEquals(Protocol.DELIVER_MESSAGE, asyncReceptor.in.readInt());
                    Reserva alerta = (Reserva) asyncReceptor.in.readObject();
                    assertEquals("Juan Perez", alerta.getFuncionario().getNombre());
                    assertEquals(1, alerta.getRecursos().size());
                    assertTrue(alerta.getRecursos().get(0).getDescripcion().startsWith("Laptop #"));
                    idsRecibidos.add(alerta.getId());
                }
                assertEquals(Set.of("RES-ALERTA-SIM-A", "RES-ALERTA-SIM-B"), idsRecibidos);

                // A y B tambien son "los demas" uno para el otro: A debe
                // recibir SOLO la alerta de B (nunca la propia) y viceversa.
                assertEquals(Protocol.DELIVER_MESSAGE, asyncA.in.readInt());
                assertEquals("RES-ALERTA-SIM-B", ((Reserva) asyncA.in.readObject()).getId());
                assertEquals(Protocol.DELIVER_MESSAGE, asyncB.in.readInt());
                assertEquals("RES-ALERTA-SIM-A", ((Reserva) asyncB.in.readObject()).getId());

                asyncA.socket.setSoTimeout(300);
                assertThrows(SocketTimeoutException.class, () -> asyncA.in.readInt());
                asyncB.socket.setSoTimeout(300);
                assertThrows(SocketTimeoutException.class, () -> asyncB.in.readInt());
            }
        }
    }
}
