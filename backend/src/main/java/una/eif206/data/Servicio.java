package una.eif206.data;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Clase base de los ServicioX (patron de la seccion 4 de docs/BACKEND-P2.md,
 * ya corregido: parametros reales -no literales-, try-with-resources,
 * execute() para procedimientos, nunca printStackTrace hacia el cliente).
 */
public abstract class Servicio {

    /**
     * Tipo usado al registrar el parametro de salida de un REF CURSOR.
     * Unico lugar a cambiar si el driver ojdbc8 rechaza el tipo JDBC estandar
     * contra un Oracle XE 11.2 real: reemplazar por oracle.jdbc.OracleTypes.CURSOR.
     */
    protected static final int TIPO_CURSOR = Types.REF_CURSOR;

    protected Connection conexion;
    protected final ConexionProvider conexionProvider;

    protected Servicio() {
        this(Servicio::conexionReal);
    }

    protected Servicio(ConexionProvider conexionProvider) {
        this.conexionProvider = conexionProvider;
    }

    /**
     * Abre la conexion real exactamente como el profe (mismas tres variables,
     * mismos valores por defecto, misma fuente System.getenv): solo se agrega
     * la verificacion de DB_PASSWORD para no autenticar con clave vacia.
     */
    private static Connection conexionReal() throws SQLException, ClassNotFoundException {
        Class.forName("oracle.jdbc.OracleDriver");
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:oracle:thin:@localhost:1521:XE");
        String usuario = System.getenv().getOrDefault("DB_USER", "system");
        String clave = System.getenv("DB_PASSWORD");
        if (clave == null || clave.isEmpty()) {
            throw new SQLException("Defina DB_PASSWORD como variable de entorno");
        }
        return DriverManager.getConnection(url, usuario, clave);
    }

    protected void conectar() throws GlobalException, NoDataException {
        try {
            conexion = conexionProvider.obtenerConexion();
        } catch (ClassNotFoundException e) {
            throw new GlobalException("No se ha localizado el driver");
        } catch (SQLException e) {
            throw new NoDataException("La base de datos no se encuentra disponible");
        }
    }

    protected void desconectar() throws GlobalException {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
            }
        } catch (SQLException e) {
            throw new GlobalException("Error al cerrar la conexion");
        }
    }

    protected static void registrarCursorSalida(CallableStatement cs, int indice) throws SQLException {
        cs.registerOutParameter(indice, TIPO_CURSOR);
    }

    /** Traduce un SQLException de operacion (insert/update/delete/select) a un mensaje para el usuario. */
    protected GlobalException errorSql(SQLException e) {
        return new GlobalException(mensajeError(e));
    }

    static String mensajeError(SQLException e) {
        switch (e.getErrorCode()) {
            case 1:
                return "Ya existe un registro con ese identificador";
            case 2291:
                return "Referencia invalida (el registro relacionado no existe)";
            case 2292:
                return "No se puede eliminar: tiene registros asociados";
            case 12899:
                return "Dato demasiado largo";
            case 1017:
                return "Credenciales de base de datos invalidas";
            case 12541:
            case 17002:
                return "La base de datos no se encuentra disponible";
            default:
                return "Error de base de datos";
        }
    }
}
