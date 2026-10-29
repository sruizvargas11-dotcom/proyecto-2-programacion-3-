package una.eif206.net;

import una.eif206.data.MemoriaRepositorio;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Recurso;
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
 * operaciones de RecursoService agregadas en la Etapa 3: findByCategoria,
 * create, update, delete.
 */
class RecursoWorkerIT {

    private static final CategoriaRecurso CAT_LAPTOPS = new CategoriaRecurso("CAT-000001", "Laptop windows");
    private static final CategoriaRecurso CAT_SALA_JUNTAS = new CategoriaRecurso("CAT-000002", "Sala de Juntas");

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
    void findByCategoriaDevuelveLosRecursosDeEsaCategoria() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.RECURSO_FIND_BY_CATEGORIA, CAT_LAPTOPS);

            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            List<?> resultado = (List<?>) cliente.in.readObject();
            assertEquals(2, resultado.size());
        }
    }

    @Test
    void createNuevoRecursoQuedaDisponibleEnFindByCategoria() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.RECURSO_CREATE, new Recurso("999999", "Laptop nueva", CAT_LAPTOPS));
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());

            cliente.enviar(Protocol.RECURSO_FIND_BY_CATEGORIA, CAT_LAPTOPS);
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertEquals(3, ((List<?>) cliente.in.readObject()).size());
        }
    }

    @Test
    void createSinCategoriaDevuelveMensajeDeNegocio() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            Recurso sinCategoria = new Recurso("888888", "Sin categoria", null);
            cliente.enviar(Protocol.RECURSO_CREATE, sinCategoria);

            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertEquals("Debe asignar una categoria al recurso", cliente.in.readObject());
        }
    }

    @Test
    void updateModificaLaDescripcion() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.RECURSO_UPDATE, new Recurso("238715", "Laptop renombrada", CAT_LAPTOPS));
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());

            cliente.enviar(Protocol.RECURSO_FIND_BY_CATEGORIA, CAT_LAPTOPS);
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            List<?> resultado = (List<?>) cliente.in.readObject();
            assertTrue(resultado.stream().anyMatch(r -> "Laptop renombrada".equals(((Recurso) r).getDescripcion())));
        }
    }

    @Test
    void deleteEliminaElRecurso() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.RECURSO_DELETE, new Recurso("34343", "Sala 1 primer piso", CAT_SALA_JUNTAS));
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());

            cliente.enviar(Protocol.RECURSO_FIND_BY_CATEGORIA, CAT_SALA_JUNTAS);
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertTrue(((List<?>) cliente.in.readObject()).isEmpty());
        }
    }
}
