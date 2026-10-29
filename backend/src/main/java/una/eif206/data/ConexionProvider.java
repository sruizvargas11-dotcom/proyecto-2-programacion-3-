package una.eif206.data;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Fuente de conexiones JDBC, inyectable. Por defecto cada Servicio abre una
 * conexion real contra Oracle usando Config; en pruebas se reemplaza por un
 * proveedor que devuelve una Connection simulada (Mockito), sin tocar Oracle.
 */
@FunctionalInterface
public interface ConexionProvider {
    Connection obtenerConexion() throws SQLException, ClassNotFoundException;
}
