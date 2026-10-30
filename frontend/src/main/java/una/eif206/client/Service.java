package una.eif206.client;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.ReservaExtraccion;
import una.eif206.model.Usuario;
import una.eif206.protocol.IService;
import una.eif206.protocol.Protocol;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class Service implements IService {

    private static final int TIMEOUT_CONEXION_MS = 5000;
    private static final int TIMEOUT_RESPUESTA_MS = 60000;

    private final String host;
    private final int puerto;
    private final Socket socket;
    private final ObjectOutputStream salida;
    private final ObjectInputStream entrada;
    private final String sid;
    private boolean conectado;
    /** Se ejecuta (en el hilo de Swing) si se cae la conexion con el Backend. */
    private Runnable alPerderConexion;

    public Service(String host, int puerto) throws IOException {
        this.host = host;
        this.puerto = puerto;
        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, puerto), TIMEOUT_CONEXION_MS);
            socket.setSoTimeout(TIMEOUT_RESPUESTA_MS);

            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(socket.getInputStream());

            salida.writeInt(Protocol.SYNC);
            salida.flush();
            sid = (String) entrada.readObject();
            conectado = true;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            cerrarSocket();
            throw new IOException("No se pudo conectar al servidor " + host + ":" + puerto);
        }
    }

    public String getSid() { return sid; }
    public String getHost() { return host; }
    public int getPuerto() { return puerto; }

    private synchronized Object enviar(int operacion, Object... argumentos) {
        if (!conectado) {
            throw new IllegalStateException("No hay conexion con el servidor " + host + ":" + puerto);
        }
        int status;
        Object resultado;
        try {
            salida.reset();
            salida.writeInt(operacion);
            for (Object argumento : argumentos) {
                salida.writeObject(argumento);
            }
            salida.flush();

            status = entrada.readInt();
            resultado = entrada.readObject();
        } catch (IOException | ClassNotFoundException e) {
            conectado = false;
            cerrarSocket();
            if (alPerderConexion != null) SwingUtilities.invokeLater(alPerderConexion);
            throw new IllegalStateException("Se perdio la conexion con el servidor " + host + ":" + puerto);
        }

        if (status == Protocol.ERROR_ERROR) {
            String mensaje = (resultado instanceof String) ? (String) resultado : "Error en el servidor";
            throw new IllegalStateException(mensaje);
        }
        return resultado;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> lista(Object resultado) {
        return (List<T>) resultado;
    }

    public synchronized void cerrar() {
        if (conectado) {
            try {
                salida.writeInt(Protocol.LOGOUT);
                salida.flush();
            } catch (IOException ignored) {
                // si ya se cayo la conexion, no hay a quien avisar
            }
        }
        conectado = false;
        cerrarSocket();
    }

    public boolean isConectado() { return conectado; }

    public void setAlPerderConexion(Runnable accion) { this.alPerderConexion = accion; }

    private void cerrarSocket() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // cierre de mejor esfuerzo
        }
    }

    // ===================== UsuarioService =====================

    @Override
    public Usuario login(String id, String clave) {
        return (Usuario) enviar(Protocol.LOGIN, id, clave);
    }

    @Override
    public String cambiarClave(Usuario u, String claveActual, String claveNueva) {
        return (String) enviar(Protocol.CAMBIAR_CLAVE, u, claveActual, claveNueva);
    }

    // ===================== FuncionarioService =====================

    @Override
    public List<Funcionario> findAllFuncionarios() {
        return lista(enviar(Protocol.FUNCIONARIO_FIND_ALL));
    }

    @Override
    public List<Funcionario> search(Funcionario e) {
        return lista(enviar(Protocol.FUNCIONARIO_SEARCH, e));
    }

    @Override
    public List<Funcionario> search(String texto) {
        return lista(enviar(Protocol.FUNCIONARIO_SEARCH_TEXTO, texto));
    }

    @Override
    public String create(Funcionario e) {
        return (String) enviar(Protocol.FUNCIONARIO_CREATE, e);
    }

    @Override
    public String update(Funcionario f) {
        return (String) enviar(Protocol.FUNCIONARIO_UPDATE, f);
    }

    @Override
    public String delete(Funcionario e) {
        return (String) enviar(Protocol.FUNCIONARIO_DELETE, e);
    }

    @Override
    public Funcionario findFuncionarioById(String id) {
        return (Funcionario) enviar(Protocol.FUNCIONARIO_FIND_BY_ID, id);
    }

    // ===================== CategoriaService =====================

    @Override
    public List<CategoriaRecurso> findAllCategorias() {
        return lista(enviar(Protocol.CATEGORIA_FIND_ALL));
    }

    @Override
    public CategoriaRecurso findCategoriaById(String id) {
        return (CategoriaRecurso) enviar(Protocol.CATEGORIA_FIND_BY_ID, id);
    }

    @Override
    public List<CategoriaRecurso> search(CategoriaRecurso e) {
        return lista(enviar(Protocol.CATEGORIA_SEARCH, e));
    }

    @Override
    public String create(CategoriaRecurso e) {
        return (String) enviar(Protocol.CATEGORIA_CREATE, e);
    }

    @Override
    public String update(CategoriaRecurso c) {
        return (String) enviar(Protocol.CATEGORIA_UPDATE, c);
    }

    @Override
    public String delete(CategoriaRecurso e) {
        return (String) enviar(Protocol.CATEGORIA_DELETE, e);
    }

    // ===================== RecursoService =====================

    @Override
    public List<Recurso> findAllRecursos() {
        return lista(enviar(Protocol.RECURSO_FIND_ALL));
    }

    @Override
    public List<Recurso> findByCategoria(CategoriaRecurso categoria) {
        return lista(enviar(Protocol.RECURSO_FIND_BY_CATEGORIA, categoria));
    }

    @Override
    public String create(Recurso e) {
        return (String) enviar(Protocol.RECURSO_CREATE, e);
    }

    @Override
    public String update(Recurso r) {
        return (String) enviar(Protocol.RECURSO_UPDATE, r);
    }

    @Override
    public String delete(Recurso e) {
        return (String) enviar(Protocol.RECURSO_DELETE, e);
    }

    // ===================== ReservaService =====================

    @Override
    public List<Reserva> findAllReservas() {
        return lista(enviar(Protocol.RESERVA_FIND_ALL));
    }

    @Override
    public List<Reserva> findByFuncionario(Funcionario f) {
        return lista(enviar(Protocol.RESERVA_FIND_BY_FUNCIONARIO, f));
    }

    @Override
    public String create(Reserva e) {
        return (String) enviar(Protocol.RESERVA_CREATE, e);
    }

    @Override
    public String cancelar(Reserva r) {
        return (String) enviar(Protocol.RESERVA_CANCELAR, r);
    }


    @Override
    public String generarId() {
        throw new UnsupportedOperationException("El id de la reserva lo genera el servidor al crearla");
    }

    @Override
    public Recurso buscarRecursoDisponible(CategoriaRecurso categoria, String fecha, String horaInicio, String horaFin) {
        throw new UnsupportedOperationException("Los recursos los asigna el servidor al crear la reserva");
    }

    @Override
    public String asignarRecursosDisponibles(List<CategoriaRecurso> categorias, String fecha,
                                             String horaInicio, String horaFin, List<Recurso> recursosAsignados) {
        throw new UnsupportedOperationException("Los recursos los asigna el servidor al crear la reserva");
    }

    // ===================== CalendarioService =====================

    @Override
    public String[][] getCalendario(String fecha, CategoriaRecurso categoria) {
        return (String[][]) enviar(Protocol.CALENDARIO_GET, fecha, categoria);
    }

    // ===================== ActividadesService =====================

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, String>> getActividadesSemana(String fechaReferencia) throws Exception {
        return (Map<String, Map<String, String>>) enviar(Protocol.ACTIVIDADES_GET_SEMANA, fechaReferencia);
    }

    // ===================== EstadisticasService =====================

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Integer> getEstadisticasRecursos(String desde, String hasta) throws Exception {
        return (Map<String, Integer>) enviar(Protocol.ESTADISTICAS_RECURSOS, desde, hasta);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Integer> getEstadisticasActividades(String desde, String hasta) throws Exception {
        return (Map<String, Integer>) enviar(Protocol.ESTADISTICAS_ACTIVIDADES, desde, hasta);
    }

    // ===================== ReservaIAService =====================

    @Override
    public ReservaExtraccion extraerReserva(String frase) throws Exception {
        return (ReservaExtraccion) enviar(Protocol.RESERVA_IA_EXTRAER, frase);
    }
}