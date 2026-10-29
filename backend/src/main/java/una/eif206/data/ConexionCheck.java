package una.eif206.data;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Prueba manual de conectividad contra Oracle, sin tocar el resto de la app.
 * No imprime credenciales en ningun caso (ni URL con password, ni usuario).
 *
 * Uso (desde backend/, con las variables de entorno ya configuradas):
 *   mvn -pl backend exec:java -Dexec.mainClass=una.eif206.data.ConexionCheck
 *
 * Con el argumento --limpiar borra las filas de prueba en vez de insertarlas:
 *   mvn -pl backend exec:java -Dexec.mainClass=una.eif206.data.ConexionCheck -Dexec.args=--limpiar
 */
public final class ConexionCheck {

    private ConexionCheck() {
    }

    public static void main(String[] args) {
        System.out.println("=== ConexionCheck ===");

        if (!probarDriver()) {
            return;
        }

        DriverManager.setLoginTimeout(10);

        String url;
        String usuario;
        String clave;
        try {
            url = Config.get("DB_URL");
            usuario = Config.get("DB_USER");
            clave = Config.get("DB_PASSWORD");
        } catch (IllegalStateException e) {
            System.out.println("[ERROR] " + e.getMessage());
            return;
        }

        boolean limpiar = args.length > 0 && "--limpiar".equals(args[0]);

        try (Connection conexion = DriverManager.getConnection(url, usuario, clave)) {
            System.out.println("[OK] Conexion establecida.");
            imprimirVersiones(conexion);
            contarObjetosInvalidos(conexion);
            probarTipoCursor(conexion);

            if (limpiar) {
                limpiarPruebas(conexion);
            } else {
                probarIdaYVuelta(conexion);
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] No se pudo conectar: " + Servicio.mensajeError(e)
                    + " (codigo Oracle " + e.getErrorCode() + ")");
        }
    }

    private static boolean probarDriver() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            System.out.println("[OK] Driver oracle.jdbc.OracleDriver localizado.");
            return true;
        } catch (ClassNotFoundException e) {
            System.out.println("[ERROR] No se ha localizado el driver (falta ojdbc8 en el classpath).");
            return false;
        }
    }

    private static void imprimirVersiones(Connection conexion) throws SQLException {
        DatabaseMetaData meta = conexion.getMetaData();
        System.out.println("[INFO] Driver: " + meta.getDriverName() + " " + meta.getDriverVersion());
        System.out.println("[INFO] Base de datos: " + meta.getDatabaseProductName()
                + " " + meta.getDatabaseProductVersion());
    }

    private static void contarObjetosInvalidos(Connection conexion) {
        String sql = "SELECT COUNT(*) FROM user_objects WHERE status = 'INVALID'";
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                System.out.println("[INFO] Objetos INVALID en el esquema: " + rs.getInt(1));
            }
        } catch (SQLException e) {
            System.out.println("[AVISO] No se pudo consultar user_objects: " + Servicio.mensajeError(e));
        }
    }

    /**
     * Prueba si el driver acepta Types.REF_CURSOR (estandar JDBC) como tipo
     * de salida de un REF CURSOR contra esta instancia; si falla, prueba con
     * oracle.jdbc.OracleTypes.CURSOR (propietario de Oracle). El resultado
     * indica cual de los dos usar en Servicio.TIPO_CURSOR.
     */
    private static void probarTipoCursor(Connection conexion) {
        if (probarTipoCursor(conexion, Types.REF_CURSOR, "Types.REF_CURSOR (estandar JDBC)")) {
            return;
        }
        probarTipoCursor(conexion, oracle.jdbc.OracleTypes.CURSOR, "oracle.jdbc.OracleTypes.CURSOR (propietario)");
    }

    private static boolean probarTipoCursor(Connection conexion, int tipo, String nombre) {
        String bloque = "BEGIN OPEN ? FOR SELECT 1 FROM DUAL; END;";
        try (CallableStatement cs = conexion.prepareCall(bloque)) {
            cs.registerOutParameter(1, tipo);
            cs.execute();
            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                rs.next();
            }
            System.out.println("[OK] Cursor funciona con " + nombre + ".");
            return true;
        } catch (SQLException e) {
            System.out.println("[AVISO] Cursor NO funciona con " + nombre + ": " + Servicio.mensajeError(e));
            return false;
        }
    }

    // ===================== Prueba de ida y vuelta (db/01_prueba_conexion.sql) =====================

    /** Fila de PRUEBA_CONEXION, usada solo por este chequeo manual. */
    record FilaPrueba(long id, String texto, Date creado) {
    }

    private static void probarIdaYVuelta(Connection conexion) {
        System.out.println("[INFO] Prueba de ida y vuelta (insertarPrueba -> listarPrueba):");
        try {
            long id = insertarPrueba(conexion, "hola desde Java");
            System.out.println("[OK] insertarPrueba ejecutado, fila nueva con ID " + id + ".");
        } catch (SQLException e) {
            avisarFaltaScriptPrueba(e);
            return;
        }

        try {
            List<FilaPrueba> filas = listarPruebas(conexion);
            System.out.println("[INFO] Filas en PRUEBA_CONEXION (" + filas.size() + "):");
            for (FilaPrueba fila : filas) {
                System.out.println("  id=" + fila.id() + " texto=" + fila.texto() + " creado=" + fila.creado());
            }
            System.out.println("[INFO] No se borro ninguna fila. Revisar con "
                    + "SELECT * FROM PRUEBA_CONEXION; en SQL Developer, o correr "
                    + "ConexionCheck con el argumento --limpiar para borrarlas.");
        } catch (SQLException e) {
            avisarFaltaScriptPrueba(e);
        }
    }

    private static void limpiarPruebas(Connection conexion) {
        try {
            List<FilaPrueba> filas = listarPruebas(conexion);
            int borradas = eliminarPruebas(conexion, filas);
            System.out.println("[OK] Se borraron " + borradas + " fila(s) de PRUEBA_CONEXION.");
        } catch (SQLException e) {
            avisarFaltaScriptPrueba(e);
        }
    }

    private static void avisarFaltaScriptPrueba(SQLException e) {
        // ORA-00942 (tabla/vista no existe) o ORA-06550 (identificador no declarado,
        // el caso tipico al invocar un procedimiento/funcion que no se creo).
        if (e.getErrorCode() == 942 || e.getErrorCode() == 6550) {
            System.out.println("[ERROR] Falta la tabla o el procedimiento de prueba. "
                    + "Ejecutar primero db/01_prueba_conexion.sql en SQL Developer.");
        } else {
            System.out.println("[ERROR] " + Servicio.mensajeError(e) + " (codigo Oracle " + e.getErrorCode() + ")");
        }
    }

    /**
     * Logica de ida y vuelta, extraida para poder probarla con un Connection
     * simulado (Mockito) sin tocar Oracle. Usa parametros bind, nunca concatena.
     */
    static long insertarPrueba(Connection conexion, String texto) throws SQLException {
        try (CallableStatement cs = conexion.prepareCall("{call insertarPrueba(?,?)}")) {
            cs.setString(1, texto);
            cs.registerOutParameter(2, Types.NUMERIC);
            cs.execute();
            return cs.getLong(2);
        }
    }

    static List<FilaPrueba> listarPruebas(Connection conexion) throws SQLException {
        List<FilaPrueba> filas = new ArrayList<>();
        try (CallableStatement cs = conexion.prepareCall("{?=call listarPrueba()}")) {
            registrarCursorSalida(cs, 1);
            cs.execute();
            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                while (rs.next()) {
                    filas.add(new FilaPrueba(rs.getLong("ID"), rs.getString("TEXTO"), rs.getDate("CREADO")));
                }
            }
        }
        return filas;
    }

    static int eliminarPruebas(Connection conexion, List<FilaPrueba> filas) throws SQLException {
        int borradas = 0;
        try (CallableStatement cs = conexion.prepareCall("{call eliminarPrueba(?)}")) {
            for (FilaPrueba fila : filas) {
                cs.setLong(1, fila.id());
                cs.execute();
                borradas++;
            }
        }
        return borradas;
    }

    private static void registrarCursorSalida(CallableStatement cs, int indice) throws SQLException {
        cs.registerOutParameter(indice, Servicio.TIPO_CURSOR);
    }
}
