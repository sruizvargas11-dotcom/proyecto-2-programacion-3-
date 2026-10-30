package una.eif206.client;

import una.eif206.model.Reserva;
import una.eif206.protocol.Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servidor chico SOLO para las pruebas de integracion del Frontend.
 * Habla el mismo protocolo que el Server/Worker del Backend (handshake SYNC con
 * sid, socket ASYNC asociado por sid, writeInt(op) + writeObject(args) y
 * respuesta readInt(status) + readObject(resultado)), pero guarda los datos
 * en un FakeService. Asi el Frontend se prueba por red sin depender del modulo backend.
 */
class ServidorDePrueba {

    private final FakeService datos = new FakeService();
    private final Map<String, ObjectOutputStream> canalesAsync = new ConcurrentHashMap<>();
    private final List<Socket> conexiones = new CopyOnWriteArrayList<>();
    private ServerSocket serverSocket;
    private volatile boolean activo;

    /** Arranca en un puerto libre y devuelve ese puerto. */
    int iniciar() throws IOException {
        serverSocket = new ServerSocket(0);
        activo = true;
        Thread aceptador = new Thread(() -> {
            while (activo) {
                try {
                    Socket s = serverSocket.accept();
                    if (!activo) {          // llego justo mientras se apagaba
                        s.close();
                        break;
                    }
                    conexiones.add(s);
                    Thread t = new Thread(() -> atender(s));
                    t.setDaemon(true);
                    t.start();
                } catch (IOException e) {
                    // cerrado
                }
            }
        });
        aceptador.setDaemon(true);
        aceptador.start();
        return serverSocket.getLocalPort();
    }

    void detener() throws IOException {
        activo = false;
        serverSocket.close();
        // Simula que el Backend se apaga: se cortan todas las conexiones abiertas.
        for (Socket s : conexiones) {
            try { s.close(); } catch (IOException ignored) { }
        }
    }

    private void atender(Socket socket) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            int tipo = in.readInt();
            if (tipo == Protocol.ASYNC) {
                String sid = (String) in.readObject();
                canalesAsync.put(sid, out);
                return;
            }
            String sid = UUID.randomUUID().toString();
            out.writeObject(sid);
            out.flush();
            while (activo) {
                int op = in.readInt();
                switch (op) {
                    case Protocol.LOGOUT:
                        socket.close();
                        return;
                    case Protocol.LOGIN: {
                        String id = (String) in.readObject();
                        String clave = (String) in.readObject();
                        responder(out, () -> datos.login(id, clave));
                        break;
                    }
                    case Protocol.CATEGORIA_FIND_ALL:
                        responder(out, datos::findAllCategorias);
                        break;
                    case Protocol.FUNCIONARIO_FIND_ALL:
                        responder(out, datos::findAllFuncionarios);
                        break;
                    case Protocol.RESERVA_FIND_ALL:
                        responder(out, datos::findAllReservas);
                        break;
                    case Protocol.RESERVA_CREATE: {
                        Reserva r = (Reserva) in.readObject();
                        String error;
                        synchronized (datos) { error = datos.create(r); }
                        responder(out, () -> error);
                        if (error == null) avisarALosDemas(sid, r);
                        break;
                    }
                    case Protocol.RESERVA_IA_EXTRAER: {
                        String frase = (String) in.readObject();
                        responder(out, () -> datos.extraerReserva(frase));
                        break;
                    }
                    default:
                        out.writeInt(Protocol.ERROR_ERROR);
                        out.writeObject("Operacion no soportada: " + op);
                        out.flush();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // cliente desconectado
        }
    }

    private void responder(ObjectOutputStream out, Callable<Object> accion) throws IOException {
        Object resultado;
        int status;
        try {
            synchronized (datos) { resultado = accion.call(); }
            status = Protocol.ERROR_NO_ERROR;
        } catch (Exception e) {
            resultado = e.getMessage();
            status = Protocol.ERROR_ERROR;
        }
        out.reset();
        out.writeInt(status);
        out.writeObject(resultado);
        out.flush();
    }

    /** Igual que el Backend: la alerta va a todos MENOS al que creo la reserva. */
    private void avisarALosDemas(String sidCreador, Reserva r) {
        for (Map.Entry<String, ObjectOutputStream> e : canalesAsync.entrySet()) {
            if (e.getKey().equals(sidCreador)) continue;
            ObjectOutputStream o = e.getValue();
            synchronized (o) {
                try {
                    o.reset();
                    o.writeInt(Protocol.DELIVER_MESSAGE);
                    o.writeObject(r);
                    o.flush();
                } catch (IOException ex) {
                    canalesAsync.remove(e.getKey());
                }
            }
        }
    }
}
