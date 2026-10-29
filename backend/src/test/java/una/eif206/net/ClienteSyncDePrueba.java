package una.eif206.net;

import una.eif206.protocol.Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Cliente SYNC minimo reusado por los *WorkerIT: crea su propio
 * ObjectOutputStream, hace flush() y luego crea el ObjectInputStream (mismo
 * orden que el Server, para evitar el deadlock de streams de objetos
 * full-duplex sobre el mismo socket). Reemplaza a la clase privada
 * equivalente que ya existe en ServerIT, para no duplicarla en cada
 * *WorkerIT nuevo.
 */
final class ClienteSyncDePrueba implements AutoCloseable {

    final Socket socket;
    final ObjectOutputStream out;
    final ObjectInputStream in;

    ClienteSyncDePrueba(int puerto) throws IOException {
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

    void enviar(int operacion, Object... argumentos) throws IOException {
        out.writeInt(operacion);
        for (Object argumento : argumentos) {
            out.writeObject(argumento);
        }
        out.flush();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
