package una.eif206;

import una.eif206.data.Config;
import una.eif206.net.Server;
import una.eif206.protocol.Protocol;
import una.eif206.service.Service;

import java.io.IOException;

/**
 * Arranque por consola del backend. Sin interfaz grafica: solo abre el
 * ServerSocket y queda escuchando. La base de datos sale de las variables
 * de entorno (o backend/config.properties), nunca de este archivo.
 */
public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) throws IOException {
        int puerto = resolverPuerto();
        Server server = new Server(puerto, new Service());
        server.start();
        System.out.println("[ServerMain] backend escuchando en el puerto " + puerto
                + ". Ctrl+C para detener.");
    }

    private static int resolverPuerto() {
        try {
            return Integer.parseInt(Config.get("PORT"));
        } catch (IllegalStateException e) {
            return Protocol.PORT;
        }
    }
}
