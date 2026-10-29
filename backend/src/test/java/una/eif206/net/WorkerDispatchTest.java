package una.eif206.net;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.ReservaExtraccion;
import una.eif206.model.Usuario;
import una.eif206.protocol.IService;
import una.eif206.protocol.Protocol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba el despacho del Worker (switch(operacion) -> service.metodo(...))
 * con un IService simulado (Mockito): nunca pasa por Service/*Service/
 * Repositorio real, asi que no toca Oracle ni la IA. Usa un socket real entre
 * dos hilos (como hace el cliente real) para ejercitar exactamente la misma
 * lectura/escritura de ObjectInputStream/ObjectOutputStream que usa el
 * protocolo, incluida la codificacion de argumentos con readObject() en el
 * orden de IService y el reset() antes de cada respuesta.
 */
@ExtendWith(MockitoExtension.class)
class WorkerDispatchTest {

    private IService service;
    private Server server;
    private Socket clientSocket;
    private Socket serverSocket;
    private ObjectOutputStream clientOut;
    private ObjectInputStream clientIn;
    private Thread hiloWorker;

    @BeforeEach
    void conectar() throws IOException {
        service = mock(IService.class);

        ServerSocket listener = new ServerSocket(0);
        clientSocket = new Socket("localhost", listener.getLocalPort());
        serverSocket = listener.accept();
        listener.close();

        clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
        clientOut.flush();
        ObjectOutputStream serverOut = new ObjectOutputStream(serverSocket.getOutputStream());
        serverOut.flush();
        clientIn = new ObjectInputStream(clientSocket.getInputStream());
        ObjectInputStream serverIn = new ObjectInputStream(serverSocket.getInputStream());

        server = new Server(0, service);
        Worker worker = new Worker("sid-prueba", serverSocket, serverOut, serverIn, service, server);
        hiloWorker = new Thread(worker, "worker-prueba");
        hiloWorker.start();
    }

    @AfterEach
    void desconectar() throws IOException, InterruptedException {
        clientOut.writeInt(Protocol.LOGOUT);
        clientOut.flush();
        hiloWorker.join(2000);
        clientSocket.close();
    }

    private void enviar(int operacion, Object... argumentos) throws IOException {
        clientOut.writeInt(operacion);
        for (Object argumento : argumentos) {
            clientOut.writeObject(argumento);
        }
        clientOut.flush();
    }

    private Object leerExitoso() throws IOException, ClassNotFoundException {
        assertEquals(Protocol.ERROR_NO_ERROR, clientIn.readInt());
        return clientIn.readObject();
    }

    // ===================== UsuarioService =====================

    @Test
    void cambiarClaveLlamaAlServiceConLosTresArgumentos() throws Exception {
        Usuario u = new Funcionario("111", "Juan Perez", "TI", "3323");
        when(service.cambiarClave(eq(u), eq("vieja"), eq("nueva"))).thenReturn(null);

        enviar(Protocol.CAMBIAR_CLAVE, u, "vieja", "nueva");

        assertNull(leerExitoso());
        verify(service).cambiarClave(eq(u), eq("vieja"), eq("nueva"));
    }

    @Test
    void cambiarClaveDevuelveElErrorDeNegocioComoString() throws Exception {
        Usuario u = new Funcionario("111", "Juan Perez", "TI", "3323");
        when(service.cambiarClave(eq(u), eq("mala"), eq("nueva"))).thenReturn("Clave actual incorrecta");

        enviar(Protocol.CAMBIAR_CLAVE, u, "mala", "nueva");

        assertEquals("Clave actual incorrecta", leerExitoso());
    }

    // ===================== FuncionarioService =====================

    @Test
    void funcionarioSearchPorObjetoLlamaAlService() throws Exception {
        Funcionario filtro = new Funcionario("", "Juan", "", "");
        Funcionario esperado = new Funcionario("111", "Juan Perez", "TI", "3323");
        when(service.search(eq(filtro))).thenReturn(List.of(esperado));

        enviar(Protocol.FUNCIONARIO_SEARCH, filtro);

        Object resultado = leerExitoso();
        assertEquals(List.of(esperado), resultado);
        ArgumentCaptor<Funcionario> captor = ArgumentCaptor.forClass(Funcionario.class);
        verify(service).search(captor.capture());
        assertEquals("Juan", captor.getValue().getNombre());
    }

    @Test
    void funcionarioSearchPorTextoLlamaAlServiceConString() throws Exception {
        Funcionario esperado = new Funcionario("111", "Juan Perez", "TI", "3323");
        when(service.search("111")).thenReturn(List.of(esperado));

        enviar(Protocol.FUNCIONARIO_SEARCH_TEXTO, "111");

        assertEquals(List.of(esperado), leerExitoso());
        verify(service).search("111");
    }

    @Test
    void funcionarioCreateEnviaElFuncionarioCompleto() throws Exception {
        Funcionario nuevo = new Funcionario("333", "Nuevo", "TI", "1234");
        when(service.create(any(Funcionario.class))).thenReturn(null);

        enviar(Protocol.FUNCIONARIO_CREATE, nuevo);

        assertNull(leerExitoso());
        ArgumentCaptor<Funcionario> captor = ArgumentCaptor.forClass(Funcionario.class);
        verify(service).create(captor.capture());
        assertEquals("333", captor.getValue().getId());
        assertEquals("Nuevo", captor.getValue().getNombre());
    }

    @Test
    void funcionarioUpdateDevuelveMensajeDeErrorSiNoExiste() throws Exception {
        Funcionario f = new Funcionario("999", "Fantasma", "TI", "0");
        when(service.update(any(Funcionario.class))).thenReturn("Funcionario no encontrado");

        enviar(Protocol.FUNCIONARIO_UPDATE, f);

        assertEquals("Funcionario no encontrado", leerExitoso());
        verify(service).update(f);
    }

    @Test
    void funcionarioDeleteLlamaAlService() throws Exception {
        Funcionario f = new Funcionario("111", "Juan Perez", "TI", "3323");
        when(service.delete(any(Funcionario.class))).thenReturn(null);

        enviar(Protocol.FUNCIONARIO_DELETE, f);

        assertNull(leerExitoso());
        verify(service).delete(f);
    }

    @Test
    void funcionarioFindByIdLlamaAlServiceConElId() throws Exception {
        Funcionario esperado = new Funcionario("111", "Juan Perez", "TI", "3323");
        when(service.findFuncionarioById("111")).thenReturn(esperado);

        enviar(Protocol.FUNCIONARIO_FIND_BY_ID, "111");

        assertEquals(esperado, leerExitoso());
        verify(service).findFuncionarioById("111");
    }

    // ===================== CategoriaService =====================

    @Test
    void categoriaFindByIdLlamaAlServiceConElId() throws Exception {
        CategoriaRecurso esperado = new CategoriaRecurso("CAT-000001", "Laptop windows");
        when(service.findCategoriaById("CAT-000001")).thenReturn(esperado);

        enviar(Protocol.CATEGORIA_FIND_BY_ID, "CAT-000001");

        Object resultado = leerExitoso();
        assertEquals("CAT-000001", ((CategoriaRecurso) resultado).getId());
        verify(service).findCategoriaById("CAT-000001");
    }

    @Test
    void categoriaSearchEnviaElObjetoDeFiltro() throws Exception {
        CategoriaRecurso filtro = new CategoriaRecurso("", "Laptop");
        CategoriaRecurso esperado = new CategoriaRecurso("CAT-000001", "Laptop windows");
        when(service.search(any(CategoriaRecurso.class))).thenReturn(List.of(esperado));

        enviar(Protocol.CATEGORIA_SEARCH, filtro);

        leerExitoso();
        ArgumentCaptor<CategoriaRecurso> captor = ArgumentCaptor.forClass(CategoriaRecurso.class);
        verify(service).search(captor.capture());
        assertEquals("Laptop", captor.getValue().getDescripcion());
    }

    @Test
    void categoriaCreateEnviaLaDescripcion() throws Exception {
        CategoriaRecurso nueva = new CategoriaRecurso(null, "Proyector");
        when(service.create(any(CategoriaRecurso.class))).thenReturn(null);

        enviar(Protocol.CATEGORIA_CREATE, nueva);

        assertNull(leerExitoso());
        ArgumentCaptor<CategoriaRecurso> captor = ArgumentCaptor.forClass(CategoriaRecurso.class);
        verify(service).create(captor.capture());
        assertEquals("Proyector", captor.getValue().getDescripcion());
    }

    @Test
    void categoriaUpdateLlamaAlService() throws Exception {
        CategoriaRecurso c = new CategoriaRecurso("CAT-000001", "Laptop modificada");
        when(service.update(any(CategoriaRecurso.class))).thenReturn(null);

        enviar(Protocol.CATEGORIA_UPDATE, c);

        assertNull(leerExitoso());
        ArgumentCaptor<CategoriaRecurso> captor = ArgumentCaptor.forClass(CategoriaRecurso.class);
        verify(service).update(captor.capture());
        assertEquals("Laptop modificada", captor.getValue().getDescripcion());
    }

    @Test
    void categoriaDeleteDevuelveElMensajeDeNegocio() throws Exception {
        CategoriaRecurso c = new CategoriaRecurso("CAT-000002", "Sala de Juntas");
        when(service.delete(any(CategoriaRecurso.class)))
                .thenReturn("No se puede eliminar: existen recursos asociados.");

        enviar(Protocol.CATEGORIA_DELETE, c);

        assertEquals("No se puede eliminar: existen recursos asociados.", leerExitoso());
    }

    // ===================== RecursoService =====================

    @Test
    void recursoFindByCategoriaLlamaAlServiceConLaCategoria() throws Exception {
        CategoriaRecurso categoria = new CategoriaRecurso("CAT-000001", "Laptop windows");
        Recurso esperado = new Recurso("238715", "Laptop #238715", categoria);
        when(service.findByCategoria(any(CategoriaRecurso.class)))
                .thenReturn(List.of(esperado));

        enviar(Protocol.RECURSO_FIND_BY_CATEGORIA, categoria);

        leerExitoso();
        ArgumentCaptor<CategoriaRecurso> captor = ArgumentCaptor.forClass(CategoriaRecurso.class);
        verify(service).findByCategoria(captor.capture());
        assertEquals("CAT-000001", captor.getValue().getId());
    }

    @Test
    void recursoCreateEnviaElRecursoCompleto() throws Exception {
        CategoriaRecurso categoria = new CategoriaRecurso("CAT-000001", "Laptop windows");
        Recurso nuevo = new Recurso("999999", "Laptop nueva", categoria);
        when(service.create(any(Recurso.class))).thenReturn(null);

        enviar(Protocol.RECURSO_CREATE, nuevo);

        assertNull(leerExitoso());
        ArgumentCaptor<Recurso> captor = ArgumentCaptor.forClass(Recurso.class);
        verify(service).create(captor.capture());
        assertEquals("999999", captor.getValue().getId());
        assertEquals("CAT-000001", captor.getValue().getCategoria().getId());
    }

    @Test
    void recursoUpdateLlamaAlService() throws Exception {
        CategoriaRecurso categoria = new CategoriaRecurso("CAT-000001", "Laptop windows");
        Recurso r = new Recurso("238715", "Laptop renombrada", categoria);
        when(service.update(any(Recurso.class))).thenReturn(null);

        enviar(Protocol.RECURSO_UPDATE, r);

        assertNull(leerExitoso());
        ArgumentCaptor<Recurso> captor = ArgumentCaptor.forClass(Recurso.class);
        verify(service).update(captor.capture());
        assertEquals("Laptop renombrada", captor.getValue().getDescripcion());
    }

    @Test
    void recursoDeleteDevuelveElMensajeDeNegocio() throws Exception {
        CategoriaRecurso categoria = new CategoriaRecurso("CAT-000001", "Laptop windows");
        Recurso r = new Recurso("238715", "Laptop #238715", categoria);
        when(service.delete(any(Recurso.class))).thenReturn("Recurso no encontrado");

        enviar(Protocol.RECURSO_DELETE, r);

        assertEquals("Recurso no encontrado", leerExitoso());
    }

    // ===================== ReservaService =====================

    @Test
    void reservaFindByFuncionarioLlamaAlServiceConElFuncionario() throws Exception {
        Funcionario f = new Funcionario("111", "Juan Perez", "TI", "3323");
        Reserva esperada = new Reserva("RES-000001", "Reunion", "2026-02-10", "09:00", "10:00", f);
        when(service.findByFuncionario(eq(f))).thenReturn(List.of(esperada));

        enviar(Protocol.RESERVA_FIND_BY_FUNCIONARIO, f);

        leerExitoso();
        verify(service).findByFuncionario(eq(f));
    }

    @Test
    void reservaCreateEnviaLaReservaCompleta() throws Exception {
        Funcionario f = new Funcionario("111", "Juan Perez", "TI", "3323");
        CategoriaRecurso categoria = new CategoriaRecurso("CAT-000001", "Laptop windows");
        Recurso recurso = new Recurso("238715", "Laptop #238715", categoria);
        Reserva reserva = new Reserva("RES-000001", "Reunion", "2026-02-10", "09:00", "10:00", f);
        reserva.setRecursos(List.of(recurso));
        when(service.create(any(Reserva.class))).thenReturn(null);

        enviar(Protocol.RESERVA_CREATE, reserva);

        assertNull(leerExitoso());
        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(service).create(captor.capture());
        assertEquals("Reunion", captor.getValue().getActividad());
        assertEquals(1, captor.getValue().getRecursos().size());
    }

    @Test
    void reservaCancelarDevuelveElMensajeDeNegocio() throws Exception {
        Funcionario f = new Funcionario("111", "Juan Perez", "TI", "3323");
        Reserva r = new Reserva("RES-000001", "Reunion", "2020-01-01", "09:00", "10:00", f);
        when(service.cancelar(any(Reserva.class)))
                .thenReturn("Solo se pueden cancelar reservas con fecha futura");

        enviar(Protocol.RESERVA_CANCELAR, r);

        assertEquals("Solo se pueden cancelar reservas con fecha futura", leerExitoso());
    }

    // ===================== CalendarioService =====================

    @Test
    void calendarioGetEnviaFechaYCategoriaEnOrden() throws Exception {
        CategoriaRecurso categoria = new CategoriaRecurso("CAT-000001", "Laptop windows");
        String[][] matriz = {{"06:00", null}, {"07:00", "Reunion - Juan"}};
        when(service.getCalendario(eq("2026-02-10"), any(CategoriaRecurso.class)))
                .thenReturn(matriz);

        enviar(Protocol.CALENDARIO_GET, "2026-02-10", categoria);

        Object resultado = leerExitoso();
        assertEquals("06:00", ((String[][]) resultado)[0][0]);
        ArgumentCaptor<CategoriaRecurso> captor = ArgumentCaptor.forClass(CategoriaRecurso.class);
        verify(service).getCalendario(eq("2026-02-10"), captor.capture());
        assertEquals("CAT-000001", captor.getValue().getId());
    }

    // ===================== ActividadesService =====================

    @Test
    void actividadesGetSemanaLlamaAlServiceConLaFecha() throws Exception {
        Map<String, Map<String, String>> matriz = Map.of("09:00", Map.of("LUNES", "Reunion - Juan"));
        when(service.getActividadesSemana("2026-02-10")).thenReturn(matriz);

        enviar(Protocol.ACTIVIDADES_GET_SEMANA, "2026-02-10");

        assertEquals(matriz, leerExitoso());
        verify(service).getActividadesSemana("2026-02-10");
    }

    @Test
    void actividadesGetSemanaConFallaInesperadaNoFiltraMensajeTecnico() throws Exception {
        when(service.getActividadesSemana("fecha-invalida"))
                .thenThrow(new RuntimeException("detalle interno sensible: stacktrace de SQL"));

        enviar(Protocol.ACTIVIDADES_GET_SEMANA, "fecha-invalida");

        assertEquals(Protocol.ERROR_ERROR, clientIn.readInt());
        assertNull(clientIn.readObject());
    }

    // ===================== EstadisticasService =====================

    @Test
    void estadisticasRecursosEnviaDesdeYHastaEnOrden() throws Exception {
        Map<String, Integer> resultado = Map.of("Laptop windows", 3);
        when(service.getEstadisticasRecursos("2026-01-01", "2026-01-31")).thenReturn(resultado);

        enviar(Protocol.ESTADISTICAS_RECURSOS, "2026-01-01", "2026-01-31");

        assertEquals(resultado, leerExitoso());
        verify(service).getEstadisticasRecursos("2026-01-01", "2026-01-31");
    }

    @Test
    void estadisticasActividadesEnviaDesdeYHastaEnOrden() throws Exception {
        Map<String, Integer> resultado = Map.of("2026-01-05", 2);
        when(service.getEstadisticasActividades("2026-01-01", "2026-01-31")).thenReturn(resultado);

        enviar(Protocol.ESTADISTICAS_ACTIVIDADES, "2026-01-01", "2026-01-31");

        assertEquals(resultado, leerExitoso());
        verify(service).getEstadisticasActividades("2026-01-01", "2026-01-31");
    }

    // ===================== ReservaIAService =====================

    @Test
    void reservaIaExtraerLlamaAlServiceConLaFrase() throws Exception {
        ReservaExtraccion extraccion = new ReservaExtraccion();
        extraccion.setActividad("Reunion");
        extraccion.setFecha("2026-02-10");
        when(service.extraerReserva("reunion mañana a las 9")).thenReturn(extraccion);

        enviar(Protocol.RESERVA_IA_EXTRAER, "reunion mañana a las 9");

        Object resultado = leerExitoso();
        assertNotNull(resultado);
        assertEquals("Reunion", ((ReservaExtraccion) resultado).getActividad());
        assertEquals("2026-02-10", ((ReservaExtraccion) resultado).getFecha());
        verify(service).extraerReserva("reunion mañana a las 9");
    }

    @Test
    void reservaIaExtraerConFallaInesperadaNoFiltraMensajeTecnico() throws Exception {
        when(service.extraerReserva("frase rara"))
                .thenThrow(new Exception("Debe configurar groq.api.key en config.properties"));

        enviar(Protocol.RESERVA_IA_EXTRAER, "frase rara");

        assertEquals(Protocol.ERROR_ERROR, clientIn.readInt());
        assertNull(clientIn.readObject());
    }
}
