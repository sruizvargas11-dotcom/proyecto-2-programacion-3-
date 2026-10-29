package una.eif206.data;

import una.eif206.model.Admin;
import una.eif206.model.Funcionario;
import una.eif206.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba ServicioUsuario con Connection/CallableStatement/ResultSet simulados (nunca toca Oracle). */
@ExtendWith(MockitoExtension.class)
class ServicioUsuarioTest {

    @Test
    void loginAdminConstruyeAdmin() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(conexion.prepareCall("{?=call loginUsuario(?,?)}")).thenReturn(cs);
        when(cs.getObject(1)).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("ID")).thenReturn("admin");
        when(rs.getString("NOMBRE")).thenReturn("admin");
        when(rs.getString("CLAVE")).thenReturn("admin123");
        when(rs.getString("ROL")).thenReturn("ADMIN");

        Usuario usuario = new ServicioUsuario(() -> conexion).login("admin", "admin123");

        assertInstanceOf(Admin.class, usuario);
        assertEquals("admin123", usuario.getClave());
    }

    @Test
    void loginFuncionarioConstruyeFuncionarioConDatos() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(conexion.prepareCall("{?=call loginUsuario(?,?)}")).thenReturn(cs);
        when(cs.getObject(1)).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("ID")).thenReturn("222");
        when(rs.getString("NOMBRE")).thenReturn("Maria Lopez");
        when(rs.getString("CLAVE")).thenReturn("222");
        when(rs.getString("ROL")).thenReturn("FUNCIONARIO");
        when(rs.getString("DEPARTAMENTO")).thenReturn("RRHH");
        when(rs.getString("TELEFONO")).thenReturn("4444");

        Usuario usuario = new ServicioUsuario(() -> conexion).login("222", "222");

        assertInstanceOf(Funcionario.class, usuario);
        assertEquals("RRHH", ((Funcionario) usuario).getDepartamento());
    }

    @Test
    void loginSinCoincidenciaDevuelveNull() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(conexion.prepareCall("{?=call loginUsuario(?,?)}")).thenReturn(cs);
        when(cs.getObject(1)).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertNull(new ServicioUsuario(() -> conexion).login("noexiste", "x"));
    }

    @Test
    void cambiarClaveEnviaLosParametrosCorrectos() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        when(conexion.prepareCall("{call cambiarClave(?,?)}")).thenReturn(cs);

        new ServicioUsuario(() -> conexion).cambiarClave("admin", "nuevaClave");

        verify(cs).setString(1, "admin");
        verify(cs).setString(2, "nuevaClave");
        verify(cs).execute();
    }
}
