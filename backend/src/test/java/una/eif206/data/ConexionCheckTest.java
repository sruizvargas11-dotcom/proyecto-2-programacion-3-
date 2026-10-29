package una.eif206.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba la logica de ida y vuelta de ConexionCheck (insertarPrueba/listarPrueba/
 * eliminarPrueba de db/01_prueba_conexion.sql) con un Connection/CallableStatement/
 * ResultSet simulados via Mockito, obtenidos a traves de ConexionProvider: no
 * toca Oracle.
 */
@ExtendWith(MockitoExtension.class)
class ConexionCheckTest {

    @Test
    void insertarPruebaUsaElTextoExactoYLosParametrosCorrectos() throws Exception {
        Connection conexionFalsa = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        when(conexionFalsa.prepareCall("{call insertarPrueba(?,?)}")).thenReturn(cs);
        when(cs.getLong(2)).thenReturn(42L);
        ConexionProvider proveedor = () -> conexionFalsa;

        long id = ConexionCheck.insertarPrueba(proveedor.obtenerConexion(), "hola desde Java");

        assertEquals(42L, id);
        verify(conexionFalsa).prepareCall("{call insertarPrueba(?,?)}");
        verify(cs).setString(1, "hola desde Java");
        verify(cs).registerOutParameter(2, Types.NUMERIC);
        verify(cs).execute();
        verify(cs).close();
    }

    @Test
    void listarPruebasUsaElTextoExactoYElTipoDeCursorCentralizadoEnServicio() throws Exception {
        Connection conexionFalsa = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(conexionFalsa.prepareCall("{?=call listarPrueba()}")).thenReturn(cs);
        when(cs.getObject(1)).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getLong("ID")).thenReturn(7L);
        when(rs.getString("TEXTO")).thenReturn("hola desde Java");
        when(rs.getDate("CREADO")).thenReturn(Date.valueOf("2026-01-01"));
        ConexionProvider proveedor = () -> conexionFalsa;

        List<ConexionCheck.FilaPrueba> filas = ConexionCheck.listarPruebas(proveedor.obtenerConexion());

        assertEquals(1, filas.size());
        assertEquals(7L, filas.get(0).id());
        assertEquals("hola desde Java", filas.get(0).texto());
        assertEquals(Date.valueOf("2026-01-01"), filas.get(0).creado());
        verify(conexionFalsa).prepareCall("{?=call listarPrueba()}");
        verify(cs).registerOutParameter(1, Servicio.TIPO_CURSOR);
        verify(cs).execute();
        verify(cs).close();
        verify(rs).close();
    }

    @Test
    void eliminarPruebasLlamaElProcedimientoUnaVezPorFilaConSuId() throws Exception {
        Connection conexionFalsa = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        when(conexionFalsa.prepareCall("{call eliminarPrueba(?)}")).thenReturn(cs);
        ConexionProvider proveedor = () -> conexionFalsa;
        List<ConexionCheck.FilaPrueba> filas = List.of(
                new ConexionCheck.FilaPrueba(1L, "a", Date.valueOf("2026-01-01")),
                new ConexionCheck.FilaPrueba(2L, "b", Date.valueOf("2026-01-02")));

        int borradas = ConexionCheck.eliminarPruebas(proveedor.obtenerConexion(), filas);

        assertEquals(2, borradas);
        verify(conexionFalsa).prepareCall("{call eliminarPrueba(?)}");
        verify(cs).setLong(1, 1L);
        verify(cs).setLong(1, 2L);
        verify(cs, times(2)).execute();
        verify(cs).close();
    }
}
