package una.eif206.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba ServicioEstadisticas con Connection/CallableStatement/ResultSet simulados (nunca toca Oracle). */
@ExtendWith(MockitoExtension.class)
class ServicioEstadisticasTest {

    @Test
    void estadisticasRecursosMapeaCategoriaYCantidad() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(conexion.prepareCall("{?=call estadisticaRecursosCategoria(?,?)}")).thenReturn(cs);
        when(cs.getObject(1)).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("CATEGORIA_DESCRIPCION")).thenReturn("Laptops");
        when(rs.getInt("CANTIDAD")).thenReturn(5);

        Map<String, Integer> resultado = new ServicioEstadisticas(() -> conexion)
                .estadisticasRecursos("2026-05-01", "2026-05-31");

        assertEquals(5, resultado.get("Laptops"));
        verify(cs).setDate(2, Date.valueOf("2026-05-01"));
        verify(cs).setDate(3, Date.valueOf("2026-05-31"));
    }

    @Test
    void estadisticasActividadesConvierteFechaDeSemanaAIso() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(conexion.prepareCall("{?=call estadisticaActividadesSemana(?,?)}")).thenReturn(cs);
        when(cs.getObject(1)).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getDate("SEMANA_INICIO")).thenReturn(Date.valueOf("2026-06-01"));
        when(rs.getInt("CANTIDAD")).thenReturn(3);

        Map<String, Integer> resultado = new ServicioEstadisticas(() -> conexion)
                .estadisticasActividades("2026-06-01", "2026-06-07");

        assertEquals(3, resultado.get("2026-06-01"));
    }
}
