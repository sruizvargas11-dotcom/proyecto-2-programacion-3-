package una.eif206.net;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.Usuario;
import una.eif206.protocol.IService;
import una.eif206.protocol.Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Atiende un cliente SYNC: un hilo, un socket de peticiones/respuestas y
 * (una vez que el cliente abre su segundo socket) un socket ASYNC para
 * notificaciones. Solo el hilo del Worker escribe en syncOut, asi que no
 * hace falta sincronizar esa escritura.
 *
 * <p>El {@link IService} que recibe el constructor es una UNICA instancia
 * compartida por todos los Worker (un hilo por cliente). Eso es seguro porque
 * {@code Service} no guarda estado propio: solo delega en los *Service del
 * P1, que a su vez solo guardan una referencia final a un {@code Repositorio}.
 * {@code OracleRepositorio} crea un ServicioX nuevo en cada llamada (ver su
 * javadoc), asi que la conexion JDBC de cada operacion es local a esa llamada
 * y nunca se comparte entre hilos.</p>
 */
public class Worker implements Runnable {

    private final String sid;
    private final Socket syncSocket;
    private final ObjectOutputStream syncOut;
    private final ObjectInputStream syncIn;
    private final IService service;
    private final Server server;

    /** Protege join() y enviarAlertaReserva(): dos creaciones casi simultaneas en otros Worker pueden intentar escribir en el mismo asyncOut a la vez. */
    private final Object asyncLock = new Object();

    private volatile Socket asyncSocket;
    private volatile ObjectOutputStream asyncOut;

    Worker(String sid, Socket syncSocket, ObjectOutputStream syncOut, ObjectInputStream syncIn,
           IService service, Server server) {
        this.sid = sid;
        this.syncSocket = syncSocket;
        this.syncOut = syncOut;
        this.syncIn = syncIn;
        this.service = service;
        this.server = server;
    }

    String getSid() {
        return sid;
    }

    /** Visibilidad de paquete: solo para que las pruebas de integracion verifiquen el join(). */
    boolean tieneCanalAsync() {
        return asyncSocket != null;
    }

    /** Asigna el socket ASYNC que el cliente abrio para recibir notificaciones. */
    void join(Socket asyncSocket, ObjectOutputStream asyncOut) {
        synchronized (asyncLock) {
            this.asyncSocket = asyncSocket;
            this.asyncOut = asyncOut;
        }
    }

    /**
     * Envia la alerta de una reserva creada por OTRO cliente, por el canal
     * ASYNC de este Worker. Si este Worker todavia no abrio su canal ASYNC
     * no hace nada (no es un fallo: simplemente no hay donde notificarle).
     * Sincronizado porque dos Worker distintos (cada creacion corre en el
     * hilo del Worker que la pidio) pueden llamar a este metodo del MISMO
     * Worker destino casi al mismo tiempo. reset() antes de escribir porque
     * una Reserva (o el Funcionario/Recurso que trae) puede haber viajado
     * antes por este mismo socket ASYNC.
     *
     * @throws IOException si la escritura falla (cliente desconectado): el
     *                      llamador debe quitar este Worker de la lista sin
     *                      propagar el error a los demas destinatarios.
     */
    void enviarAlertaReserva(Reserva reserva) throws IOException {
        synchronized (asyncLock) {
            ObjectOutputStream canal = asyncOut;
            if (canal == null) {
                return;
            }
            canal.reset();
            canal.writeInt(Protocol.DELIVER_MESSAGE);
            canal.writeObject(reserva);
            canal.flush();
        }
    }

    @Override
    public void run() {
        try {
            boolean activo = true;
            while (activo) {
                int operacion = syncIn.readInt();
                activo = procesarOperacion(operacion);
            }
        } catch (IOException | ClassNotFoundException e) {
            // cliente desconectado o stream corrupto: se cierra abajo
        } finally {
            cerrar();
        }
    }

    /** @return false si el Worker debe terminar su ciclo (LOGOUT). */
    private boolean procesarOperacion(int operacion) throws IOException, ClassNotFoundException {
        switch (operacion) {
            case Protocol.LOGOUT:
                log(operacion, "LOGOUT");
                return false;

            // ===================== UsuarioService =====================

            case Protocol.LOGIN: {
                String id = (String) syncIn.readObject();
                String clave = (String) syncIn.readObject();
                log(operacion, "LOGIN");
                responder(() -> service.login(id, clave));
                return true;
            }
            case Protocol.CAMBIAR_CLAVE: {
                Usuario u = (Usuario) syncIn.readObject();
                String claveActual = (String) syncIn.readObject();
                String claveNueva = (String) syncIn.readObject();
                log(operacion, "CAMBIAR_CLAVE");
                responder(() -> service.cambiarClave(u, claveActual, claveNueva));
                return true;
            }

            // ===================== FuncionarioService =====================

            case Protocol.FUNCIONARIO_FIND_ALL:
                log(operacion, "FUNCIONARIO_FIND_ALL");
                responder(service::findAllFuncionarios);
                return true;
            case Protocol.FUNCIONARIO_SEARCH: {
                Funcionario e = (Funcionario) syncIn.readObject();
                log(operacion, "FUNCIONARIO_SEARCH");
                responder(() -> service.search(e));
                return true;
            }
            case Protocol.FUNCIONARIO_SEARCH_TEXTO: {
                String texto = (String) syncIn.readObject();
                log(operacion, "FUNCIONARIO_SEARCH_TEXTO");
                responder(() -> service.search(texto));
                return true;
            }
            case Protocol.FUNCIONARIO_CREATE: {
                Funcionario e = (Funcionario) syncIn.readObject();
                log(operacion, "FUNCIONARIO_CREATE");
                responder(() -> service.create(e));
                return true;
            }
            case Protocol.FUNCIONARIO_UPDATE: {
                Funcionario f = (Funcionario) syncIn.readObject();
                log(operacion, "FUNCIONARIO_UPDATE");
                responder(() -> service.update(f));
                return true;
            }
            case Protocol.FUNCIONARIO_DELETE: {
                Funcionario e = (Funcionario) syncIn.readObject();
                log(operacion, "FUNCIONARIO_DELETE");
                responder(() -> service.delete(e));
                return true;
            }
            case Protocol.FUNCIONARIO_FIND_BY_ID: {
                String id = (String) syncIn.readObject();
                log(operacion, "FUNCIONARIO_FIND_BY_ID");
                responder(() -> service.findFuncionarioById(id));
                return true;
            }

            // ===================== CategoriaService =====================

            case Protocol.CATEGORIA_FIND_ALL:
                log(operacion, "CATEGORIA_FIND_ALL");
                responder(service::findAllCategorias);
                return true;
            case Protocol.CATEGORIA_FIND_BY_ID: {
                String id = (String) syncIn.readObject();
                log(operacion, "CATEGORIA_FIND_BY_ID");
                responder(() -> service.findCategoriaById(id));
                return true;
            }
            case Protocol.CATEGORIA_SEARCH: {
                CategoriaRecurso e = (CategoriaRecurso) syncIn.readObject();
                log(operacion, "CATEGORIA_SEARCH");
                responder(() -> service.search(e));
                return true;
            }
            case Protocol.CATEGORIA_CREATE: {
                CategoriaRecurso e = (CategoriaRecurso) syncIn.readObject();
                log(operacion, "CATEGORIA_CREATE");
                responder(() -> service.create(e));
                return true;
            }
            case Protocol.CATEGORIA_UPDATE: {
                CategoriaRecurso c = (CategoriaRecurso) syncIn.readObject();
                log(operacion, "CATEGORIA_UPDATE");
                responder(() -> service.update(c));
                return true;
            }
            case Protocol.CATEGORIA_DELETE: {
                CategoriaRecurso e = (CategoriaRecurso) syncIn.readObject();
                log(operacion, "CATEGORIA_DELETE");
                responder(() -> service.delete(e));
                return true;
            }

            // ===================== RecursoService =====================

            case Protocol.RECURSO_FIND_ALL:
                log(operacion, "RECURSO_FIND_ALL");
                responder(service::findAllRecursos);
                return true;
            case Protocol.RECURSO_FIND_BY_CATEGORIA: {
                CategoriaRecurso categoria = (CategoriaRecurso) syncIn.readObject();
                log(operacion, "RECURSO_FIND_BY_CATEGORIA");
                responder(() -> service.findByCategoria(categoria));
                return true;
            }
            case Protocol.RECURSO_CREATE: {
                Recurso e = (Recurso) syncIn.readObject();
                log(operacion, "RECURSO_CREATE");
                responder(() -> service.create(e));
                return true;
            }
            case Protocol.RECURSO_UPDATE: {
                Recurso r = (Recurso) syncIn.readObject();
                log(operacion, "RECURSO_UPDATE");
                responder(() -> service.update(r));
                return true;
            }
            case Protocol.RECURSO_DELETE: {
                Recurso e = (Recurso) syncIn.readObject();
                log(operacion, "RECURSO_DELETE");
                responder(() -> service.delete(e));
                return true;
            }

            // ===================== ReservaService =====================

            case Protocol.RESERVA_FIND_ALL:
                log(operacion, "RESERVA_FIND_ALL");
                responder(service::findAllReservas);
                return true;
            case Protocol.RESERVA_FIND_BY_FUNCIONARIO: {
                Funcionario f = (Funcionario) syncIn.readObject();
                log(operacion, "RESERVA_FIND_BY_FUNCIONARIO");
                responder(() -> service.findByFuncionario(f));
                return true;
            }
            case Protocol.RESERVA_CREATE: {
                Reserva e = (Reserva) syncIn.readObject();
                log(operacion, "RESERVA_CREATE");
                boolean exito = responder(() -> service.create(e));
                if (exito) {
                    server.broadcastReservaCreada(cargarReservaCompleta(e), this);
                }
                return true;
            }
            case Protocol.RESERVA_CANCELAR: {
                Reserva r = (Reserva) syncIn.readObject();
                log(operacion, "RESERVA_CANCELAR");
                responder(() -> service.cancelar(r));
                return true;
            }

            // ===================== CalendarioService =====================

            case Protocol.CALENDARIO_GET: {
                String fecha = (String) syncIn.readObject();
                CategoriaRecurso categoria = (CategoriaRecurso) syncIn.readObject();
                log(operacion, "CALENDARIO_GET");
                responder(() -> service.getCalendario(fecha, categoria));
                return true;
            }

            // ===================== ActividadesService =====================

            case Protocol.ACTIVIDADES_GET_SEMANA: {
                String fechaReferencia = (String) syncIn.readObject();
                log(operacion, "ACTIVIDADES_GET_SEMANA");
                responder(() -> service.getActividadesSemana(fechaReferencia));
                return true;
            }

            // ===================== EstadisticasService =====================

            case Protocol.ESTADISTICAS_RECURSOS: {
                String desde = (String) syncIn.readObject();
                String hasta = (String) syncIn.readObject();
                log(operacion, "ESTADISTICAS_RECURSOS");
                responder(() -> service.getEstadisticasRecursos(desde, hasta));
                return true;
            }
            case Protocol.ESTADISTICAS_ACTIVIDADES: {
                String desde = (String) syncIn.readObject();
                String hasta = (String) syncIn.readObject();
                log(operacion, "ESTADISTICAS_ACTIVIDADES");
                responder(() -> service.getEstadisticasActividades(desde, hasta));
                return true;
            }

            // ===================== ReservaIAService =====================

            case Protocol.RESERVA_IA_EXTRAER: {
                String frase = (String) syncIn.readObject();
                log(operacion, "RESERVA_IA_EXTRAER");
                responder(() -> service.extraerReserva(frase));
                return true;
            }

            default:
                log(operacion, "DESCONOCIDA");
                syncOut.writeInt(Protocol.ERROR_ERROR);
                syncOut.writeObject("Operacion no soportada todavia: " + operacion);
                syncOut.flush();
                return true;
        }
    }

    /**
     * Ejecuta la accion y escribe la respuesta: el resultado (puede ser un
     * mensaje de error de negocio como String, o null si todo salio bien) con
     * {@link Protocol#ERROR_NO_ERROR}, o {@link Protocol#ERROR_ERROR} sin
     * mensaje tecnico si la accion lanzo algo inesperado (RuntimeException o
     * cualquiera de los throws Exception de IService). Siempre se llama
     * reset() antes de escribir: el resultado puede ser (o contener) un
     * objeto de dominio ya enviado antes por este mismo socket y modificado
     * desde entonces (p.ej. con MemoriaRepositorio, que devuelve referencias
     * vivas), y sin reset() el ObjectOutputStream reenviaria solo el handle
     * cacheado de la version vieja en vez del estado actual.
     *
     * @return true si la operacion tuvo exito sin mensaje de error de
     *         negocio (resultado null con {@link Protocol#ERROR_NO_ERROR});
     *         solo lo usa RESERVA_CREATE para decidir si difunde la alerta.
     */
    private boolean responder(Callable<Object> accion) throws IOException {
        Object resultado;
        int status;
        try {
            resultado = accion.call();
            status = Protocol.ERROR_NO_ERROR;
        } catch (Exception e) {
            resultado = null;
            status = Protocol.ERROR_ERROR;
        }
        syncOut.reset();
        syncOut.writeInt(status);
        syncOut.writeObject(resultado);
        syncOut.flush();
        return status == Protocol.ERROR_NO_ERROR && resultado == null;
    }

    /**
     * Reconstruye la reserva con el Funcionario y los Recurso completos
     * (nombre/descripcion incluidos), sin depender de que el cliente que
     * crea la reserva haya enviado esos objetos completos: usa los mismos
     * findXxx de IService que ya expone el Worker, nunca el Repositorio
     * directamente.
     */
    private Reserva cargarReservaCompleta(Reserva creada) {
        Funcionario funcionarioCompleto = service.findFuncionarioById(creada.getFuncionario().getId());
        List<String> idsRecursos = creada.getRecursos().stream()
                .map(Recurso::getId)
                .collect(Collectors.toList());
        List<Recurso> recursosCompletos = service.findAllRecursos().stream()
                .filter(r -> idsRecursos.contains(r.getId()))
                .collect(Collectors.toList());

        Reserva completa = new Reserva(creada.getId(), creada.getActividad(), creada.getFecha(),
                creada.getHoraInicio(), creada.getHoraFin(), funcionarioCompleto);
        completa.setRecursos(recursosCompletos);
        completa.setEstado(creada.getEstado());
        return completa;
    }

    private void log(int operacion, String nombre) {
        System.out.println("[" + LocalTime.now() + "] sid=" + sid + " operacion=" + nombre + "(" + operacion + ")");
    }

    void cerrar() {
        server.removerWorker(this);
        cerrarSilenciosamente(asyncSocket);
        cerrarSilenciosamente(syncSocket);
    }

    private static void cerrarSilenciosamente(Socket socket) {
        if (socket == null || socket.isClosed()) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // cierre de mejor esfuerzo
        }
    }
}
