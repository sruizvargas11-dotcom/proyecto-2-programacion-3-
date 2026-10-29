package una.eif206.data;

import una.eif206.model.Admin;
import una.eif206.model.Funcionario;
import una.eif206.model.Usuario;
import una.eif206.model.enums.UsuarioRol;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Login y cambio de clave contra la tabla USUARIO (patron de la seccion 4 de
 * docs/BACKEND-P2.md). Nombres de procedimientos exactos a
 * script_reservas_oracle11g.sql: loginUsuario, cambiarClave.
 */
public class ServicioUsuario extends Servicio {

    private static final String LOGIN = "{?=call loginUsuario(?,?)}";
    private static final String CAMBIAR_CLAVE = "{call cambiarClave(?,?)}";

    public ServicioUsuario() {
        super();
    }

    public ServicioUsuario(ConexionProvider conexionProvider) {
        super(conexionProvider);
    }

    public Usuario login(String id, String clave) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(LOGIN)) {
            registrarCursorSalida(cs, 1);
            cs.setString(2, id);
            cs.setString(3, clave);
            cs.execute();
            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                return rs.next() ? mapearUsuario(rs) : null;
            }
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    public void cambiarClave(String id, String claveNueva) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(CAMBIAR_CLAVE)) {
            cs.setString(1, id);
            cs.setString(2, claveNueva);
            cs.execute();
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    private static Usuario mapearUsuario(ResultSet rs) throws SQLException {
        String id = rs.getString("ID");
        String nombre = rs.getString("NOMBRE");
        String clave = rs.getString("CLAVE");
        UsuarioRol rol = UsuarioRol.valueOf(rs.getString("ROL"));
        if (rol == UsuarioRol.ADMIN) {
            Admin admin = new Admin(id, clave);
            admin.setNombre(nombre);
            return admin;
        }
        Funcionario f = new Funcionario(id, nombre, rs.getString("DEPARTAMENTO"), rs.getString("TELEFONO"));
        f.setClave(clave);
        return f;
    }
}
