package una.eif206.net;

import una.eif206.data.MemoriaRepositorio;
import una.eif206.model.Usuario;
import una.eif206.protocol.Protocol;
import una.eif206.service.Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * IT por socket (sin Oracle: Service usa MemoriaRepositorio) de
 * UsuarioService.cambiarClave, agregada en la Etapa 3 (login/logout ya se
 * cubren en ServerIT).
 */
class UsuarioWorkerIT {

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

    @Test
    void cambiarClaveExitosaPermiteLoguearseConLaClaveNueva() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.LOGIN, "111", "111");
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            Usuario logueado = (Usuario) cliente.in.readObject();
            assertNotNull(logueado);
            assertEquals("111", logueado.getClave());

            cliente.enviar(Protocol.CAMBIAR_CLAVE, logueado, "111", "nueva123");
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());

            // MemoriaRepositorio devuelve la MISMA instancia de Funcionario que
            // ya se envio en el primer LOGIN, ahora con la clave modificada:
            // sin el reset() del Worker antes de cada respuesta, el
            // ObjectOutputStream reenviaria el handle cacheado del primer
            // envio (clave vieja) en vez del estado actual.
            cliente.enviar(Protocol.LOGIN, "111", "nueva123");
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            Usuario reloguedo = (Usuario) cliente.in.readObject();
            assertNotNull(reloguedo);
            assertEquals("nueva123", reloguedo.getClave());

            cliente.enviar(Protocol.LOGIN, "111", "111");
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());
        }
    }

    @Test
    void cambiarClaveConClaveActualIncorrectaDevuelveMensajeDeNegocio() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.LOGIN, "111", "111");
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            Usuario logueado = (Usuario) cliente.in.readObject();

            cliente.enviar(Protocol.CAMBIAR_CLAVE, logueado, "incorrecta", "nueva123");

            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertEquals("Clave actual incorrecta", cliente.in.readObject());
        }
    }
}
