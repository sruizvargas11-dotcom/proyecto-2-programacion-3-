package una.eif206.net;

import una.eif206.data.MemoriaRepositorio;
import una.eif206.model.Funcionario;
import una.eif206.model.Usuario;
import una.eif206.protocol.Protocol;
import una.eif206.service.Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integracion por socket real (sin Oracle: Service usa
 * MemoriaRepositorio) del handshake SYNC/ASYNC y de las operaciones minimas
 * de la Etapa 3 (login, logout, findAll). Puerto libre: se arranca con 0 y
 * se lee el puerto real asignado, para no depender de que 7070 este libre.
 */
class ServerIT {

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

    /** Conexion SYNC: crea su propio ObjectOutputStream, flush, luego ObjectInputStream. */
    private static final class ClienteSync implements AutoCloseable {
        final Socket socket;
        final ObjectOutputStream out;
        final ObjectInputStream in;

        ClienteSync(int puerto) throws IOException {
            socket = new Socket("localhost", puerto);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        }

        String handshakeSync() throws IOException, ClassNotFoundException {
            out.writeInt(Protocol.SYNC);
            out.flush();
            return (String) in.readObject();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    @Test
    void handshakeSyncDevuelveSid() throws Exception {
        try (ClienteSync cliente = new ClienteSync(puerto)) {
            String sid = cliente.handshakeSync();
            assertNotNull(sid);
            assertFalse(sid.isBlank());
            assertNotNull(server.buscarWorkerPorSid(sid));
        }
    }

    @Test
    void handshakeAsyncSeUneAlWorkerDelSid() throws Exception {
        try (ClienteSync sync = new ClienteSync(puerto)) {
            String sid = sync.handshakeSync();

            try (Socket asyncSocket = new Socket("localhost", puerto)) {
                ObjectOutputStream asyncOut = new ObjectOutputStream(asyncSocket.getOutputStream());
                asyncOut.flush();
                asyncOut.writeInt(Protocol.ASYNC);
                asyncOut.writeObject(sid);
                asyncOut.flush();

                esperarHasta(() -> server.buscarWorkerPorSid(sid).tieneCanalAsync());
                assertTrue(server.buscarWorkerPorSid(sid).tieneCanalAsync());
            }
        }
    }

    @Test
    void handshakeAsyncConSidDesconocidoCierraElSocket() throws Exception {
        try (Socket asyncSocket = new Socket("localhost", puerto)) {
            ObjectOutputStream asyncOut = new ObjectOutputStream(asyncSocket.getOutputStream());
            asyncOut.flush();
            asyncOut.writeInt(Protocol.ASYNC);
            asyncOut.writeObject("sid-que-no-existe");
            asyncOut.flush();

            ObjectInputStream asyncIn = new ObjectInputStream(asyncSocket.getInputStream());
            assertThrowsEof(() -> asyncIn.readInt());
        }
    }

    @Test
    void loginExitosoYFallidoPorSocket() throws Exception {
        try (ClienteSync cliente = new ClienteSync(puerto)) {
            cliente.handshakeSync();

            cliente.out.writeInt(Protocol.LOGIN);
            cliente.out.writeObject("111");
            cliente.out.writeObject("111");
            cliente.out.flush();
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            Usuario logueado = (Usuario) cliente.in.readObject();
            assertNotNull(logueado);
            assertEquals("111", logueado.getId());
            assertTrue(logueado instanceof Funcionario);

            cliente.out.writeInt(Protocol.LOGIN);
            cliente.out.writeObject("111");
            cliente.out.writeObject("clave-incorrecta");
            cliente.out.flush();
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());
        }
    }

    @Test
    void findAllPorSocket() throws Exception {
        try (ClienteSync cliente = new ClienteSync(puerto)) {
            cliente.handshakeSync();

            cliente.out.writeInt(Protocol.FUNCIONARIO_FIND_ALL);
            cliente.out.flush();
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            List<?> funcionarios = (List<?>) cliente.in.readObject();
            assertFalse(funcionarios.isEmpty());

            cliente.out.writeInt(Protocol.CATEGORIA_FIND_ALL);
            cliente.out.flush();
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertFalse(((List<?>) cliente.in.readObject()).isEmpty());

            cliente.out.writeInt(Protocol.RECURSO_FIND_ALL);
            cliente.out.flush();
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertFalse(((List<?>) cliente.in.readObject()).isEmpty());

            cliente.out.writeInt(Protocol.RESERVA_FIND_ALL);
            cliente.out.flush();
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertTrue(cliente.in.readObject() instanceof List);
        }
    }

    @Test
    void logoutCierraElSocketYQuitaElWorker() throws Exception {
        ClienteSync cliente = new ClienteSync(puerto);
        String sid = cliente.handshakeSync();
        assertNotNull(server.buscarWorkerPorSid(sid));

        cliente.out.writeInt(Protocol.LOGOUT);
        cliente.out.flush();

        assertThrowsEof(() -> cliente.in.readInt());
        esperarHasta(() -> server.buscarWorkerPorSid(sid) == null);
        assertNull(server.buscarWorkerPorSid(sid));

        cliente.socket.close();
    }

    /**
     * Regresion del bug donde el hilo aceptador era daemon: ServerMain.main()
     * llama a start() y retorna de inmediato, asi que si ese hilo fuera el
     * unico no-daemon, la JVM se cerraria sola apenas arranca. Esta prueba
     * arranca un Server propio (no el del @BeforeEach) desde un hilo
     * dedicado, espera a que ESE hilo termine por completo y confirma que el
     * servidor sigue aceptando conexiones igual; despues confirma que stop()
     * libera el puerto de verdad (una conexion nueva ya no puede completarse).
     */
    @Test
    void siguenAceptandoConexionesDespuesDeQueElHiloQueLoArrancoTermineYStopLiberaElPuerto() throws Exception {
        Server servidorPropio = new Server(0, new Service(new MemoriaRepositorio()));
        Thread arrancador = new Thread(() -> {
            try {
                servidorPropio.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "arrancador-de-prueba");
        arrancador.start();
        arrancador.join(2000);
        assertFalse(arrancador.isAlive(), "el hilo que llamo a start() ya deberia haber terminado");

        int puertoPropio = servidorPropio.getPuertoLocal();
        try (Socket socket = new Socket("localhost", puertoPropio)) {
            assertTrue(socket.isConnected());
        }

        servidorPropio.stop();

        assertThrows(IOException.class, () -> {
            try (Socket socket = new Socket("localhost", puertoPropio)) {
                socket.getOutputStream().write(1);
                socket.getOutputStream().flush();
            }
        });
    }

    private interface LecturaBloqueante {
        int leer() throws IOException;
    }

    private static void assertThrowsEof(LecturaBloqueante lectura) {
        try {
            lectura.leer();
            throw new AssertionError("Se esperaba que el socket estuviera cerrado (EOF)");
        } catch (EOFException expected) {
            // el servidor cerro el socket: comportamiento esperado
        } catch (IOException tambienValido) {
            // segun el SO, un socket cerrado del otro lado puede dar otra IOException
        }
    }

    private interface Condicion {
        boolean cumplida();
    }

    private static void esperarHasta(Condicion condicion) throws InterruptedException {
        long limite = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < limite) {
            if (condicion.cumplida()) {
                return;
            }
            Thread.sleep(20);
        }
    }
}
