package una.eif206.data;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Recurso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba ServicioRecurso con Connection/CallableStatement/ResultSet simulados (nunca toca Oracle). */
@ExtendWith(MockitoExtension.class)
class ServicioRecursoTest {

    @Test
    void listarPorCategoriaMapeaCategoriaYDisponible() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(conexion.prepareCall("{?=call listarRecursoPorCategoria(?)}")).thenReturn(cs);
        when(cs.getObject(1)).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("ID")).thenReturn("238715");
        when(rs.getString("DESCRIPCION")).thenReturn("Laptop #238715");
        when(rs.getString("CATEGORIA_ID")).thenReturn("CAT-000001");
        when(rs.getString("CATEGORIA_DESCRIPCION")).thenReturn("Laptops");
        when(rs.getInt("DISPONIBLE")).thenReturn(1);

        List<Recurso> lista = new ServicioRecurso(() -> conexion).listarPorCategoria("CAT-000001");

        assertEquals(1, lista.size());
        assertEquals("Laptops", lista.get(0).getCategoria().getDescripcion());
        assertTrue(lista.get(0).isDisponible());
    }

    @Test
    void insertarUsaElIdDeLaCategoria() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        when(conexion.prepareCall("{call insertarRecurso(?,?,?)}")).thenReturn(cs);

        Recurso r = new Recurso("R-900", "Recurso de prueba", new CategoriaRecurso("CAT-000001", "Laptops"));
        new ServicioRecurso(() -> conexion).insertar(r);

        verify(cs).setString(1, "R-900");
        verify(cs).setString(2, "Recurso de prueba");
        verify(cs).setString(3, "CAT-000001");
    }

    @Test
    void buscarDevuelveNullSiNoExiste() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(conexion.prepareCall("{?=call buscarRecurso(?)}")).thenReturn(cs);
        when(cs.getObject(1)).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertNull(new ServicioRecurso(() -> conexion).buscar("no-existe"));
    }
}
