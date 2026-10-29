package una.eif206.net;

import una.eif206.data.MemoriaRepositorio;
import una.eif206.model.Funcionario;
import una.eif206.protocol.Protocol;
import una.eif206.service.Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * IT por socket (sin Oracle: Service usa MemoriaRepositorio) de las
 * operaciones de FuncionarioService agregadas en la Etapa 3: search(objeto),
 * search(texto), create, update, delete, findById.
 */
class FuncionarioWorkerIT {

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
    void searchPorObjetoBuscaPorNombre() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.FUNCIONARIO_SEARCH, new Funcionario("", "Maria", "", ""));

            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            List<?> resultado = (List<?>) cliente.in.readObject();
            assertEquals(1, resultado.size());
            assertEquals("222", ((Funcionario) resultado.get(0)).getId());
        }
    }

    @Test
    void searchPorTextoConIdExactoDevuelveSoloEseFuncionario() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.FUNCIONARIO_SEARCH_TEXTO, "111");

            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            List<?> resultado = (List<?>) cliente.in.readObject();
            assertEquals(1, resultado.size());
            assertEquals("111", ((Funcionario) resultado.get(0)).getId());
        }
    }

    @Test
    void createNuevoFuncionarioQuedaDisponibleParaFindById() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.FUNCIONARIO_CREATE, new Funcionario("333", "Nuevo", "TI", "1234"));
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());

            cliente.enviar(Protocol.FUNCIONARIO_FIND_BY_ID, "333");
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            Funcionario creado = (Funcionario) cliente.in.readObject();
            assertEquals("Nuevo", creado.getNombre());
        }
    }

    @Test
    void createDuplicadoDevuelveMensajeDeNegocio() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.FUNCIONARIO_CREATE, new Funcionario("111", "Duplicado", "TI", "0"));

            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertEquals("Funcionario ya existe", cliente.in.readObject());
        }
    }

    @Test
    void updateModificaLosDatosYSePuedenVolverALeer() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.FUNCIONARIO_UPDATE, new Funcionario("111", "Juan Modificado", "Ventas", "9999"));
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());

            cliente.enviar(Protocol.FUNCIONARIO_FIND_BY_ID, "111");
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            Funcionario modificado = (Funcionario) cliente.in.readObject();
            assertEquals("Juan Modificado", modificado.getNombre());
            assertEquals("Ventas", modificado.getDepartamento());
        }
    }

    @Test
    void deleteEliminaYFindByIdLuegoDevuelveNull() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.FUNCIONARIO_DELETE, new Funcionario("222", "Maria Lopez", "RRHH", "4444"));
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());

            cliente.enviar(Protocol.FUNCIONARIO_FIND_BY_ID, "222");
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());
        }
    }

    @Test
    void findByIdDevuelveElFuncionarioCompleto() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.FUNCIONARIO_FIND_BY_ID, "111");

            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            Funcionario f = (Funcionario) cliente.in.readObject();
            assertEquals("Juan Perez", f.getNombre());
        }
    }
}
