package una.eif206.net;

import una.eif206.protocol.Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Cliente ASYNC minimo reusado por los *WorkerIT de alertas: abre un segundo
 * socket y se une al Worker del sid indicado, igual que hace un cliente real
 * despues del handshake SYNC.
 */
final class ClienteAsyncDePrueba implements AutoCloseable {

    final Socket socket;
    final ObjectOutputStream out;
    final ObjectInputStream in;

    ClienteAsyncDePrueba(int puerto, String sid) throws IOException {
        socket = new Socket("localhost", puerto);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        out.writeInt(Protocol.ASYNC);
        out.writeObject(sid);
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
