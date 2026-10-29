package una.eif206.data;

import una.eif206.model.CategoriaRecurso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba ServicioCategoria con Connection/CallableStatement/ResultSet simulados (nunca toca Oracle). */
@ExtendWith(MockitoExtension.class)
class ServicioCategoriaTest {

    @Test
    void listarMapeaFilasDelCursor() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(conexion.prepareCall("{?=call listarCategoria()}")).thenReturn(cs);
        when(cs.getObject(1)).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("ID")).thenReturn("CAT-000001", "CAT-000002");
        when(rs.getString("DESCRIPCION")).thenReturn("Laptops", "Salas");

        List<CategoriaRecurso> lista = new ServicioCategoria(() -> conexion).listar();

        assertEquals(2, lista.size());
        assertEquals("CAT-000001", lista.get(0).getId());
        verify(cs).registerOutParameter(1, Types.REF_CURSOR);
        verify(conexion).close();
    }

    @Test
    void buscarDevuelveNullSiNoHayFila() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(conexion.prepareCall("{?=call buscarCategoria(?)}")).thenReturn(cs);
        when(cs.getObject(1)).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertNull(new ServicioCategoria(() -> conexion).buscar("CAT-999999"));
    }

    @Test
    void insertarRegistraOutParameterYDevuelveIdGenerado() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        when(conexion.prepareCall("{call insertarCategoria(?,?)}")).thenReturn(cs);
        when(cs.getString(2)).thenReturn("CAT-000004");

        String id = new ServicioCategoria(() -> conexion).insertar("Nueva categoria");

        assertEquals("CAT-000004", id);
        verify(cs).setString(1, "Nueva categoria");
        verify(cs).registerOutParameter(2, Types.VARCHAR);
        verify(cs).execute();
    }

    @Test
    void eliminarPropagaErrorSqlComoGlobalException() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        when(conexion.prepareCall("{call eliminarCategoria(?)}")).thenReturn(cs);
        doThrow(new SQLException("fk violada", "23000", 2292)).when(cs).execute();

        ServicioCategoria servicio = new ServicioCategoria(() -> conexion);

        GlobalException ex = assertThrows(GlobalException.class, () -> servicio.eliminar("CAT-000001"));
        assertEquals("No se puede eliminar: tiene registros asociados", ex.getMessage());
        verify(conexion).close();
    }
}
