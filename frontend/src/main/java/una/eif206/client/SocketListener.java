package una.eif206.client;

import una.eif206.model.Reserva;
import una.eif206.protocol.Protocol;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketListener {

    private final String host;
    private final int puerto;
    private final String sid;

    private Socket socket;
    private ObjectInputStream entrada;
    private Thread hilo;
    private volatile boolean activo;

    private ThreadListener destino;

    public SocketListener(String host, int puerto, String sid) {
        this.host = host;
        this.puerto = puerto;
        this.sid = sid;
    }

    /** Abre el socket ASYNC, hace el handshake y arranca el hilo que escucha. */
    public void start() throws IOException {
        socket = new Socket(host, puerto);
        // Igual que en el socket SYNC: primero el ObjectOutputStream + flush.
        ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
        salida.flush();
        entrada = new ObjectInputStream(socket.getInputStream());

        salida.writeInt(Protocol.ASYNC);
        salida.writeObject(sid);
        salida.flush();

        activo = true;
        hilo = new Thread(this::listen, "socket-listener");
        // Daemon: si el usuario cierra la ventana, este hilo no impide que el programa termine.
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Ciclo del hilo: espera alertas hasta que se detenga o se caiga la conexion. */
    private void listen() {
        try {
            while (activo) {
                int tipo = entrada.readInt();
                if (tipo == Protocol.DELIVER_MESSAGE) {
                    Reserva reserva = (Reserva) entrada.readObject();
                    SwingUtilities.invokeLater(() -> entregar(reserva));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // Socket cerrado (stop() o Backend apagado): el hilo termina.
            // Si se cayo el Backend, el Service (SYNC) lo avisa en la siguiente operacion.
        } finally {
            activo = false;
        }
    }

    private void entregar(Reserva reserva) {
        if (destino != null) {
            destino.deliver_message(reserva);
        }
    }

    public void setDestino(ThreadListener destino) {
        this.destino = destino;
    }

    /** Detiene el hilo y cierra el socket ASYNC (al salir del programa). */
    public void stop() {
        activo = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
            // cierre de mejor esfuerzo
        }
    }

    public boolean isActivo() { return activo; }
}