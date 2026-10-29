package una.eif206.data;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Prueba ServicioReserva con Connection/CallableStatement/ResultSet
 * simulados: cubre la transaccion de crear() (autoCommit/commit/rollback) y
 * el chequeo de disponibilidad, sin tocar Oracle.
 */
@ExtendWith(MockitoExtension.class)
class ServicioReservaTest {

    private static Reserva nuevaReserva() {
        Funcionario f = new Funcionario("111", "Juan Perez", "TI", "3323");
        Recurso r = new Recurso("238715", "Laptop #238715", new CategoriaRecurso("CAT-000001", "Laptops"));
        Reserva reserva = new Reserva("RES-000001", "Reunion", "2026-02-10", "09:00", "10:00", f);
        reserva.setRecursos(List.of(r));
        return reserva;
    }

    @Test
    void crearSinOcupacionInsertaYHaceCommit() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement csOcupados = mock(CallableStatement.class);
        ResultSet rsOcupados = mock(ResultSet.class);
        CallableStatement csInsertar = mock(CallableStatement.class);
        CallableStatement csInsertarRecurso = mock(CallableStatement.class);
        when(conexion.prepareCall("{?=call listarRecursosOcupados(?,?,?)}")).thenReturn(csOcupados);
        when(csOcupados.getObject(1)).thenReturn(rsOcupados);
        when(rsOcupados.next()).thenReturn(false);
        when(conexion.prepareCall("{call insertarReserva(?,?,?,?,?,?)}")).thenReturn(csInsertar);
        when(conexion.prepareCall("{call insertarReservaRecurso(?,?)}")).thenReturn(csInsertarRecurso);

        new ServicioReserva(() -> conexion).crear(nuevaReserva());

        verify(conexion).setAutoCommit(false);
        verify(csInsertar).execute();
        verify(csInsertarRecurso).setString(1, "RES-000001");
        verify(csInsertarRecurso).setString(2, "238715");
        verify(conexion).commit();
        verify(conexion, never()).rollback();
        verify(conexion).setAutoCommit(true);
    }

    @Test
    void crearConRecursoOcupadoLanzaGlobalExceptionYHaceRollback() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement csOcupados = mock(CallableStatement.class);
        ResultSet rsOcupados = mock(ResultSet.class);
        when(conexion.prepareCall("{?=call listarRecursosOcupados(?,?,?)}")).thenReturn(csOcupados);
        when(csOcupados.getObject(1)).thenReturn(rsOcupados);
        when(rsOcupados.next()).thenReturn(true, false);
        when(rsOcupados.getString("RECURSO_ID")).thenReturn("238715");

        ServicioReserva servicio = new ServicioReserva(() -> conexion);
        GlobalException ex = assertThrows(GlobalException.class, () -> servicio.crear(nuevaReserva()));

        assertEquals("No hay disponibilidad para el recurso en ese horario", ex.getMessage());
        verify(conexion).rollback();
        verify(conexion, never()).commit();
        verify(conexion).setAutoCommit(true);
    }

    @Test
    void siguienteIdLeeElOutParameterVarchar() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        when(conexion.prepareCall("{?=call siguienteIdReserva()}")).thenReturn(cs);
        when(cs.getString(1)).thenReturn("RES-000042");

        String id = new ServicioReserva(() -> conexion).siguienteId();

        assertEquals("RES-000042", id);
        verify(cs).registerOutParameter(1, Types.VARCHAR);
    }

    @Test
    void cancelarEnviaElId() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement cs = mock(CallableStatement.class);
        when(conexion.prepareCall("{call cancelarReserva(?)}")).thenReturn(cs);

        new ServicioReserva(() -> conexion).cancelar("RES-000001");

        verify(cs).setString(1, "RES-000001");
        verify(cs).execute();
    }
}
