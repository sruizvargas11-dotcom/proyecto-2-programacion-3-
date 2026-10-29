package una.eif206.net;

import una.eif206.data.MemoriaRepositorio;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.protocol.Protocol;
import una.eif206.service.Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IT por socket (sin Oracle: Service usa MemoriaRepositorio) de las
 * operaciones de ReservaService agregadas en la Etapa 3: findByFuncionario,
 * create, cancelar. extraerReserva (IA) no se cubre aqui porque necesita red
 * real; se cubre con el service simulado en WorkerDispatchTest.
 */
class ReservaWorkerIT {

    private static final Funcionario JUAN = new Funcionario("111", "Juan Perez", "TI", "3323");
    private static final CategoriaRecurso CAT_LAPTOPS = new CategoriaRecurso("CAT-000001", "Laptop windows");
    private static final Recurso LAPTOP = new Recurso("238715", "Laptop #238715", CAT_LAPTOPS);

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

    private static Reserva nuevaReservaFutura(String id) {
        Reserva reserva = new Reserva(id, "Reunion de prueba", "2031-01-01", "09:00", "10:00", JUAN);
        reserva.setRecursos(List.of(LAPTOP));
        return reserva;
    }

    @Test
    void createNuevaReservaQuedaDisponibleEnFindByFuncionario() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.RESERVA_CREATE, nuevaReservaFutura("RES-TEST-1"));
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());

            cliente.enviar(Protocol.RESERVA_FIND_BY_FUNCIONARIO, JUAN);
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            List<?> resultado = (List<?>) cliente.in.readObject();
            assertEquals(1, resultado.size());
            assertEquals("RES-TEST-1", ((Reserva) resultado.get(0)).getId());
        }
    }

    @Test
    void createSinRecursosDevuelveMensajeDeNegocio() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            Reserva sinRecursos = new Reserva("RES-TEST-2", "Reunion", "2031-01-01", "09:00", "10:00", JUAN);
            cliente.enviar(Protocol.RESERVA_CREATE, sinRecursos);

            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertEquals("Debe asignar al menos un recurso a la reserva", cliente.in.readObject());
        }
    }

    @Test
    void cancelarUnaReservaFuturaLaMarcaCancelada() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            cliente.enviar(Protocol.RESERVA_CREATE, nuevaReservaFutura("RES-TEST-3"));
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());

            cliente.enviar(Protocol.RESERVA_CANCELAR, nuevaReservaFutura("RES-TEST-3"));
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());
        }
    }

    @Test
    void cancelarUnaReservaConFechaPasadaDevuelveMensajeDeNegocio() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            Reserva pasada = new Reserva("RES-TEST-4", "Reunion vieja", "2020-01-01", "09:00", "10:00", JUAN);
            pasada.setRecursos(List.of(LAPTOP));
            cliente.enviar(Protocol.RESERVA_CREATE, pasada);
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());

            cliente.enviar(Protocol.RESERVA_CANCELAR, pasada);
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertEquals("Solo se pueden cancelar reservas con fecha futura", cliente.in.readObject());
        }
    }

    @Test
    void createSinIdYConRecursoSoloPorCategoriaAsignaIdGeneradoYRecursoReal() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();

            Reserva sinIdNiRecursoConcreto = new Reserva(null, "Reunion de prueba", "2031-01-01", "09:00", "10:00", JUAN);
            sinIdNiRecursoConcreto.setRecursos(List.of(new Recurso(null, null, CAT_LAPTOPS)));

            cliente.enviar(Protocol.RESERVA_CREATE, sinIdNiRecursoConcreto);
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            assertNull(cliente.in.readObject());

            cliente.enviar(Protocol.RESERVA_FIND_BY_FUNCIONARIO, JUAN);
            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            List<?> resultado = (List<?>) cliente.in.readObject();
            assertEquals(1, resultado.size());
            Reserva creada = (Reserva) resultado.get(0);

            assertNotNull(creada.getId());
            assertFalse(creada.getId().isBlank());
            assertEquals(1, creada.getRecursos().size());
            Recurso asignado = creada.getRecursos().get(0);
            assertNotNull(asignado.getId());
            assertTrue(asignado.getDescripcion() != null && asignado.getDescripcion().startsWith("Laptop #"));
        }
    }
}
