package una.eif206.net;

import una.eif206.model.Reserva;
import una.eif206.protocol.IService;
import una.eif206.protocol.Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Escucha en un puerto, acepta clientes y crea un Worker por cada socket
 * SYNC. Cada conexion entrante (SYNC o ASYNC) crea primero su propio
 * ObjectOutputStream y hace flush() antes de crear el ObjectInputStream,
 * en ambos extremos, para evitar el deadlock clasico de streams de objetos
 * full-duplex sobre el mismo socket.
 */
public class Server implements AutoCloseable {

    private final int puerto;
    private final IService service;
    private final List<Worker> workers = new CopyOnWriteArrayList<>();

    private ServerSocket serverSocket;
    private Thread hiloAceptador;
    private volatile boolean activo;

    public Server(int puerto, IService service) {
        this.puerto = puerto;
        this.service = service;
    }

    /**
     * Arranca el ServerSocket y el hilo que acepta clientes. Bloquea hasta
     * que el puerto quede abierto, pero retorna sin esperar a que el hilo
     * aceptador termine (eso solo pasa con stop()/close()).
     *
     * <p>El hilo aceptador NO es daemon a proposito: si lo fuera, en cuanto
     * el hilo que llamo a start() retorna (como hace ServerMain.main()) y no
     * queda ningun otro hilo no-daemon en la JVM, la JVM se cierra sola sin
     * avisar, cerrando el servidor apenas arranca. Al no ser daemon, el
     * proceso sigue vivo hasta que stop()/close() lo detenga a proposito.</p>
     */
    public synchronized void start() throws IOException {
        serverSocket = new ServerSocket(puerto);
        activo = true;
        hiloAceptador = new Thread(this::aceptarClientes, "server-accept");
        hiloAceptador.setDaemon(false);
        hiloAceptador.start();
        log("Servidor escuchando en el puerto " + getPuertoLocal());
    }

    /** Puerto real en el que quedo escuchando (util cuando se arranca con puerto 0). */
    public int getPuertoLocal() {
        return serverSocket.getLocalPort();
    }

    private void aceptarClientes() {
        while (activo) {
            try {
                Socket socket = serverSocket.accept();
                // Daemon a proposito: un hilo de conexion colgado (o un
                // Worker que no alcanzo a notar el cierre de su socket) no
                // debe impedir que la JVM termine. Solo el hilo aceptador en
                // si necesita ser no-daemon (ver start()); como Thread hereda
                // el daemon de quien lo crea, sin este setDaemon(true)
                // explicito este hilo (y el Worker que arranca desde el) lo
                // heredarian en false y podrian colgar el proceso.
                Thread hiloConexion = new Thread(() -> manejarConexion(socket), "server-conexion");
                hiloConexion.setDaemon(true);
                hiloConexion.start();
            } catch (IOException e) {
                if (activo) {
                    log("Error aceptando conexion: " + e.getMessage());
                }
            }
        }
    }

    private void manejarConexion(Socket socket) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            int tipo = ois.readInt();
            if (tipo == Protocol.SYNC) {
                atenderSync(socket, oos, ois);
            } else if (tipo == Protocol.ASYNC) {
                atenderAsync(socket, oos, ois);
            } else {
                socket.close();
            }
        } catch (IOException | ClassNotFoundException e) {
            cerrarSilenciosamente(socket);
        }
    }

    private void atenderSync(Socket socket, ObjectOutputStream oos, ObjectInputStream ois) throws IOException {
        String sid = UUID.randomUUID().toString();
        Worker worker = new Worker(sid, socket, oos, ois, service, this);
        workers.add(worker);
        oos.writeObject(sid);
        oos.flush();
        log("SYNC nuevo cliente sid=" + sid);
        // Daemon a proposito, misma razon que el hilo "server-conexion".
        Thread hiloWorker = new Thread(worker, "worker-" + sid);
        hiloWorker.setDaemon(true);
        hiloWorker.start();
    }

    private void atenderAsync(Socket socket, ObjectOutputStream oos, ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        String sid = (String) ois.readObject();
        Worker worker = buscarWorkerPorSid(sid);
        if (worker == null) {
            log("ASYNC con sid desconocido: " + sid);
            socket.close();
            return;
        }
        worker.join(socket, oos);
        log("ASYNC asociado a sid=" + sid);
    }

    /** Visibilidad de paquete: usado por Worker/ASYNC y por las pruebas de integracion. */
    Worker buscarWorkerPorSid(String sid) {
        for (Worker w : workers) {
            if (w.getSid().equals(sid)) {
                return w;
            }
        }
        return null;
    }

    /** Cantidad de Workers activos; usado por las pruebas de integracion. */
    int cantidadWorkers() {
        return workers.size();
    }

    void removerWorker(Worker worker) {
        workers.remove(worker);
    }

    /**
     * Difunde la reserva recien creada por ALERTAS: por el canal ASYNC de
     * todos los Worker EXCEPTO el creador. {@code workers} es un
     * CopyOnWriteArrayList, asi que iterarlo mientras otro hilo agrega/quita
     * Worker (nuevas conexiones, logouts) es seguro. Si el envio a un Worker
     * falla (su canal ASYNC esta roto: cliente desconectado abruptamente) se
     * lo quita de la lista y se sigue con el resto, sin tumbar la difusion
     * ni la respuesta del creador.
     */
    void broadcastReservaCreada(Reserva reserva, Worker creador) {
        for (Worker w : workers) {
            if (w == creador) {
                continue;
            }
            try {
                w.enviarAlertaReserva(reserva);
            } catch (IOException e) {
                workers.remove(w);
                log("ASYNC de sid=" + w.getSid() + " fallo al recibir la alerta, se quita de la lista: " + e.getMessage());
            }
        }
    }

    private void log(String mensaje) {
        System.out.println("[" + LocalTime.now() + "] " + mensaje);
    }

    private static void cerrarSilenciosamente(Socket socket) {
        if (socket == null || socket.isClosed()) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // cierre de mejor esfuerzo
        }
    }

    /**
     * Apaga el servidor de forma limpia y sincrona: marca activo=false,
     * cierra todos los Worker, cierra el ServerSocket (lo que hace que
     * accept() en el hilo aceptador lance IOException y el bucle de
     * aceptarClientes() termine solo) y espera a que ese hilo realmente
     * termine antes de retornar, para que quien llame a stop() tenga la
     * garantia de que el puerto ya quedo libre. Idempotente: llamarlo mas
     * de una vez (p.ej. un test en @AfterEach tras un cierre manual) no falla.
     */
    public synchronized void stop() {
        activo = false;
        for (Worker w : workers) {
            w.cerrar();
        }
        cerrarSilenciosamente(serverSocket);
        if (hiloAceptador != null && hiloAceptador != Thread.currentThread()) {
            try {
                hiloAceptador.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void cerrarSilenciosamente(ServerSocket socket) {
        if (socket == null || socket.isClosed()) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // cierre de mejor esfuerzo
        }
    }

    /** Puente para AutoCloseable/try-with-resources: delega todo en stop(). */
    @Override
    public void close() {
        stop();
    }
}
