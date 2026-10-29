package una.eif206.net;

import una.eif206.data.MemoriaRepositorio;
import una.eif206.model.CategoriaRecurso;
import una.eif206.protocol.Protocol;
import una.eif206.service.Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IT por socket (sin Oracle: Service usa MemoriaRepositorio) de las
 * operaciones de CategoriaService agregadas en la Etapa 3: findById, search,
 * create, update, delete.
 */
class CategoriaWorkerIT {

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
    void findByIdDevuelveLaCategoriaCompleta() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.CATEGORIA_FIND_BY_ID, "CAT-000001");

            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            CategoriaRecurso c = (CategoriaRecurso) cliente.in.readObject();
            assertEquals("Laptop windows", c.getDescripcion());
        }
    }

    @Test
    void searchPorTextoDevuelveTodasLasCoincidencias() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.CATEGORIA_SEARCH, new CategoriaRecurso("", "Sala"));

            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            List<?> resultado = (List<?>) cliente.in.readObject();
            assertEquals(2, resultado.size());
        }
    }

    @Test
    void createGeneraIdYQuedaDisponibleParaSearch() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.CATEGORIA_CREATE, new CategoriaRecurso(null, "Proyector"));
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());

            cliente.enviar(Protocol.CATEGORIA_SEARCH, new CategoriaRecurso("", "Proyector"));
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            List<?> resultado = (List<?>) cliente.in.readObject();
            assertEquals(1, resultado.size());
            assertTrue(((CategoriaRecurso) resultado.get(0)).getId().startsWith("CAT-"));
        }
    }

    @Test
    void updateModificaLaDescripcion() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.CATEGORIA_UPDATE, new CategoriaRecurso("CAT-000001", "Laptop Mac"));
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());

            cliente.enviar(Protocol.CATEGORIA_FIND_BY_ID, "CAT-000001");
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertEquals("Laptop Mac", ((CategoriaRecurso) cliente.in.readObject()).getDescripcion());
        }
    }

    @Test
    void deleteSinRecursosAsociadosLaElimina() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.CATEGORIA_DELETE, new CategoriaRecurso("CAT-000003", "Sala para 10 personas"));
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());

            cliente.enviar(Protocol.CATEGORIA_FIND_BY_ID, "CAT-000003");
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());
        }
    }

    @Test
    void deleteConRecursosAsociadosDevuelveMensajeDeNegocio() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.CATEGORIA_DELETE, new CategoriaRecurso("CAT-000002", "Sala de Juntas"));

            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertEquals("No se puede eliminar: existen recursos asociados.", cliente.in.readObject());
        }
    }
}
