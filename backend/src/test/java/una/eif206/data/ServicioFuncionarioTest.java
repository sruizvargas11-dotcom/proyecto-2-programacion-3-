package una.eif206.data;

import una.eif206.model.Funcionario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba ServicioFuncionario con Connection/CallableStatement simulados (nunca toca Oracle). */
@ExtendWith(MockitoExtension.class)
class ServicioFuncionarioTest {

    @Test
    void insertarEnviaTodosLosParametrosEnOrden() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        when(conexion.prepareCall("{call insertarFuncionario(?,?,?,?,?)}")).thenReturn(cs);

        Funcionario f = new Funcionario("333", "Nuevo", "TI", "1234");
        new ServicioFuncionario(() -> conexion).insertar(f);

        verify(cs).setString(1, "333");
        verify(cs).setString(2, "Nuevo");
        verify(cs).setString(3, "333");
        verify(cs).setString(4, "TI");
        verify(cs).setString(5, "1234");
        verify(cs).execute();
    }

    @Test
    void insertarDuplicadoLanzaGlobalExceptionConMensajeMapeado() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        when(conexion.prepareCall("{call insertarFuncionario(?,?,?,?,?)}")).thenReturn(cs);
        doThrow(new SQLException("duplicado", "23000", 1)).when(cs).execute();

        GlobalException ex = assertThrows(GlobalException.class,
                () -> new ServicioFuncionario(() -> conexion).insertar(new Funcionario("111", "X", "TI", "0")));
        assertEquals("Ya existe un registro con ese identificador", ex.getMessage());
    }

    @Test
    void buscarMapeaLaFilaEncontrada() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(conexion.prepareCall("{?=call buscarFuncionario(?)}")).thenReturn(cs);
        when(cs.getObject(1)).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("ID")).thenReturn("111");
        when(rs.getString("NOMBRE")).thenReturn("Juan Perez");
        when(rs.getString("DEPARTAMENTO")).thenReturn("TI");
        when(rs.getString("TELEFONO")).thenReturn("3323");
        when(rs.getString("CLAVE")).thenReturn("111");

        Funcionario f = new ServicioFuncionario(() -> conexion).buscar("111");

        assertNotNull(f);
        assertEquals("Juan Perez", f.getNombre());
    }

    @Test
    void modificarEnviaLosCuatroParametros() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        when(conexion.prepareCall("{call modificarFuncionario(?,?,?,?)}")).thenReturn(cs);

        new ServicioFuncionario(() -> conexion).modificar(new Funcionario("111", "Nuevo Nombre", "Ventas", "9999"));

        verify(cs).setString(1, "111");
        verify(cs).setString(2, "Nuevo Nombre");
        verify(cs).setString(3, "Ventas");
        verify(cs).setString(4, "9999");
    }
}
