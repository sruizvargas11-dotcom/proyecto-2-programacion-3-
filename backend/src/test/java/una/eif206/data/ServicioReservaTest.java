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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void crearConIdVacioLoGeneraEnLaMismaConexionYLoUsaEnElInsert() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement csOcupados = mock(CallableStatement.class);
        ResultSet rsOcupados = mock(ResultSet.class);
        CallableStatement csSiguienteId = mock(CallableStatement.class);
        CallableStatement csInsertar = mock(CallableStatement.class);
        CallableStatement csInsertarRecurso = mock(CallableStatement.class);
        when(conexion.prepareCall("{?=call listarRecursosOcupados(?,?,?)}")).thenReturn(csOcupados);
        when(csOcupados.getObject(1)).thenReturn(rsOcupados);
        when(rsOcupados.next()).thenReturn(false);
        when(conexion.prepareCall("{?=call siguienteIdReserva()}")).thenReturn(csSiguienteId);
        when(csSiguienteId.getString(1)).thenReturn("RES-000099");
        when(conexion.prepareCall("{call insertarReserva(?,?,?,?,?,?)}")).thenReturn(csInsertar);
        when(conexion.prepareCall("{call insertarReservaRecurso(?,?)}")).thenReturn(csInsertarRecurso);

        Reserva reserva = new Reserva("", "Reunion", "2026-02-10", "09:00", "10:00",
                new Funcionario("111", "Juan Perez", "TI", "3323"));
        reserva.setRecursos(List.of(new Recurso("238715", "Laptop #238715",
                new CategoriaRecurso("CAT-000001", "Laptops"))));

        new ServicioReserva(() -> conexion).crear(reserva);

        verify(csSiguienteId).execute();
        verify(csInsertar).setString(1, "RES-000099");
        verify(conexion).commit();
        assertEquals("RES-000099", reserva.getId());
    }

    @Test
    void crearConRecursoSoloPorCategoriaEligeElPrimeroLibre() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement csOcupados = mock(CallableStatement.class);
        ResultSet rsOcupados = mock(ResultSet.class);
        CallableStatement csPorCategoria = mock(CallableStatement.class);
        ResultSet rsPorCategoria = mock(ResultSet.class);
        CallableStatement csInsertar = mock(CallableStatement.class);
        CallableStatement csInsertarRecurso = mock(CallableStatement.class);
        when(conexion.prepareCall("{?=call listarRecursosOcupados(?,?,?)}")).thenReturn(csOcupados);
        when(csOcupados.getObject(1)).thenReturn(rsOcupados);
        when(rsOcupados.next()).thenReturn(false);
        when(conexion.prepareCall("{?=call listarRecursoPorCategoria(?)}")).thenReturn(csPorCategoria);
        when(csPorCategoria.getObject(1)).thenReturn(rsPorCategoria);
        when(rsPorCategoria.next()).thenReturn(true, true, false);
        when(rsPorCategoria.getString("ID")).thenReturn("R1", "R2");
        when(rsPorCategoria.getString("DESCRIPCION")).thenReturn("Laptop 1", "Laptop 2");
        when(rsPorCategoria.getString("CATEGORIA_ID")).thenReturn("CAT-000001", "CAT-000001");
        when(rsPorCategoria.getString("CATEGORIA_DESCRIPCION")).thenReturn("Laptops", "Laptops");
        when(rsPorCategoria.getInt("DISPONIBLE")).thenReturn(0, 1);
        when(conexion.prepareCall("{call insertarReserva(?,?,?,?,?,?)}")).thenReturn(csInsertar);
        when(conexion.prepareCall("{call insertarReservaRecurso(?,?)}")).thenReturn(csInsertarRecurso);

        Reserva reserva = new Reserva("RES-000002", "Reunion", "2026-02-10", "09:00", "10:00",
                new Funcionario("111", "Juan Perez", "TI", "3323"));
        reserva.setRecursos(List.of(new Recurso(null, null, new CategoriaRecurso("CAT-000001", "Laptops"))));

        new ServicioReserva(() -> conexion).crear(reserva);

        // R1 vino con DISPONIBLE=0: se descarta y se elige R2, el primero libre.
        verify(csInsertarRecurso).setString(2, "R2");
        verify(csInsertarRecurso, never()).setString(2, "R1");
        verify(conexion).commit();
        assertEquals("R2", reserva.getRecursos().get(0).getId());
    }

    @Test
    void crearSinRecursoLibreEnCategoriaLanzaGlobalExceptionYHaceRollback() throws Exception {
        Connection conexion = mock(Connection.class);
        CallableStatement csOcupados = mock(CallableStatement.class);
        ResultSet rsOcupados = mock(ResultSet.class);
        CallableStatement csPorCategoria = mock(CallableStatement.class);
        ResultSet rsPorCategoria = mock(ResultSet.class);
        when(conexion.prepareCall("{?=call listarRecursosOcupados(?,?,?)}")).thenReturn(csOcupados);
        when(csOcupados.getObject(1)).thenReturn(rsOcupados);
        when(rsOcupados.next()).thenReturn(false);
        when(conexion.prepareCall("{?=call listarRecursoPorCategoria(?)}")).thenReturn(csPorCategoria);
        when(csPorCategoria.getObject(1)).thenReturn(rsPorCategoria);
        when(rsPorCategoria.next()).thenReturn(false);

        Reserva reserva = new Reserva("RES-000003", "Reunion", "2026-02-10", "09:00", "10:00",
                new Funcionario("111", "Juan Perez", "TI", "3323"));
        reserva.setRecursos(List.of(new Recurso(null, null, new CategoriaRecurso("CAT-000001", "Laptops"))));

        ServicioReserva servicio = new ServicioReserva(() -> conexion);
        GlobalException ex = assertThrows(GlobalException.class, () -> servicio.crear(reserva));

        assertNotNull(ex.getMessage());
        assertEquals("No hay recursos disponibles para: Laptops", ex.getMessage());
        verify(conexion).rollback();
        verify(conexion, never()).commit();
        verify(conexion).setAutoCommit(true);
    }
}
