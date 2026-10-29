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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IT por socket (sin Oracle: Service usa MemoriaRepositorio) de
 * CalendarioService, ActividadesService y EstadisticasService: getCalendario,
 * getActividadesSemana, getEstadisticasRecursos, getEstadisticasActividades.
 */
class ReportesWorkerIT {

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

    private void crearReservaDePrueba(ClienteSyncDePrueba cliente) throws Exception {
        Reserva reserva = new Reserva("RES-REPORTE-1", "Reunion de prueba", "2031-01-01", "09:00", "10:00", JUAN);
        reserva.setRecursos(List.of(LAPTOP));
        cliente.enviar(Protocol.RESERVA_CREATE, reserva);
        assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
        assertNull(cliente.in.readObject());
    }

    @Test
    void getCalendarioMarcaLaActividadEnSuHorario() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();
            crearReservaDePrueba(cliente);

            cliente.enviar(Protocol.CALENDARIO_GET, "2031-01-01", CAT_LAPTOPS);

            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            String[][] matriz = (String[][]) cliente.in.readObject();
            String[] fila09 = Arrays.stream(matriz).filter(f -> "09:00".equals(f[0])).findFirst().orElseThrow();
            assertEquals("Reunion de prueba - Juan Perez", fila09[1]);
        }
    }

    @Test
    void getActividadesSemanaAgrupaPorDiaYHora() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();
            crearReservaDePrueba(cliente);

            cliente.enviar(Protocol.ACTIVIDADES_GET_SEMANA, "2031-01-01");

            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> matriz = (Map<String, Map<String, String>>) cliente.in.readObject();
            assertTrue(matriz.containsKey("09:00"));
            assertTrue(matriz.get("09:00").values().stream()
                    .anyMatch(v -> v.contains("Reunion de prueba")));
        }
    }

    @Test
    void getEstadisticasRecursosCuentaPorCategoria() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();
            crearReservaDePrueba(cliente);

            cliente.enviar(Protocol.ESTADISTICAS_RECURSOS, "2031-01-01", "2031-01-31");

            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            @SuppressWarnings("unchecked")
            Map<String, Integer> resultado = (Map<String, Integer>) cliente.in.readObject();
            assertEquals(1, resultado.get("Laptop windows"));
        }
    }

    @Test
    void getEstadisticasActividadesCuentaPorSemana() throws Exception {
        try (ClienteSyncDePrueba cliente = new ClienteSyncDePrueba(puerto)) {
            cliente.handshakeSync();
            crearReservaDePrueba(cliente);

            cliente.enviar(Protocol.ESTADISTICAS_ACTIVIDADES, "2031-01-01", "2031-01-31");

            assertEquals(Protocol.ERROR_NO_ERROR, cliente.in.readInt());
            @SuppressWarnings("unchecked")
            Map<String, Integer> resultado = (Map<String, Integer>) cliente.in.readObject();
            assertEquals(1, resultado.values().stream().mapToInt(Integer::intValue).sum());
        }
    }
}
