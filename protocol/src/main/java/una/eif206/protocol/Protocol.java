package una.eif206.protocol;

/**
 * Constantes del protocolo de red cliente-servidor (se usan a partir de la Etapa 3).
 * Cada metodo de {@link IService} tiene su propia constante int de operacion,
 * para que el Worker pueda hacer switch(operacion) y el cliente sepa que escribir.
 */
public final class Protocol {

    private Protocol() {}

    public static final String HOST = "localhost";
    public static final int PORT = 7070;

    // Handshake / control
    public static final int SYNC = 1;
    public static final int ASYNC = 2;
    public static final int DELIVER_MESSAGE = 3;
    public static final int LOGOUT = 4;

    // Status de respuesta
    public static final int ERROR_NO_ERROR = 0;
    public static final int ERROR_ERROR = 1;

    // ===================== Operaciones: UsuarioService =====================
    public static final int LOGIN = 100;
    public static final int CAMBIAR_CLAVE = 101;

    // ===================== Operaciones: FuncionarioService =====================
    public static final int FUNCIONARIO_FIND_ALL = 200;
    public static final int FUNCIONARIO_SEARCH = 201;
    public static final int FUNCIONARIO_SEARCH_TEXTO = 202;
    public static final int FUNCIONARIO_CREATE = 203;
    public static final int FUNCIONARIO_UPDATE = 204;
    public static final int FUNCIONARIO_DELETE = 205;
    public static final int FUNCIONARIO_FIND_BY_ID = 206;

    // ===================== Operaciones: CategoriaService =====================
    public static final int CATEGORIA_FIND_ALL = 300;
    public static final int CATEGORIA_FIND_BY_ID = 301;
    public static final int CATEGORIA_SEARCH = 302;
    public static final int CATEGORIA_CREATE = 303;
    public static final int CATEGORIA_UPDATE = 304;
    public static final int CATEGORIA_DELETE = 305;

    // ===================== Operaciones: RecursoService =====================
    public static final int RECURSO_FIND_ALL = 400;
    public static final int RECURSO_FIND_BY_CATEGORIA = 401;
    public static final int RECURSO_CREATE = 402;
    public static final int RECURSO_UPDATE = 403;
    public static final int RECURSO_DELETE = 404;

    // ===================== Operaciones: ReservaService =====================
    public static final int RESERVA_FIND_ALL = 500;
    public static final int RESERVA_FIND_BY_FUNCIONARIO = 501;
    public static final int RESERVA_GENERAR_ID = 502;
    public static final int RESERVA_CREATE = 503;
    public static final int RESERVA_BUSCAR_RECURSO_DISPONIBLE = 504;
    public static final int RESERVA_ASIGNAR_RECURSOS_DISPONIBLES = 505;
    public static final int RESERVA_CANCELAR = 506;

    // ===================== Operaciones: CalendarioService =====================
    public static final int CALENDARIO_GET = 600;

    // ===================== Operaciones: ActividadesService =====================
    public static final int ACTIVIDADES_GET_SEMANA = 700;

    // ===================== Operaciones: EstadisticasService =====================
    public static final int ESTADISTICAS_RECURSOS = 800;
    public static final int ESTADISTICAS_ACTIVIDADES = 801;

    // ===================== Operaciones: ReservaIAService =====================
    public static final int RESERVA_IA_EXTRAER = 900;
}
