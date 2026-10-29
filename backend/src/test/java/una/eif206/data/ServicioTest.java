package una.eif206.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba la capa base de conexion (Servicio) con un Connection/CallableStatement
 * simulados via Mockito, inyectados a traves de ConexionProvider: nunca toca
 * Oracle. Las pruebas por entidad (ServicioCategoria, ServicioReserva, ...)
 * se agregan cuando esas clases existan; esta cubre lo que SI existe hoy:
 * conectar/desconectar, el registro del cursor de salida y el mapa de errores.
 */
@ExtendWith(MockitoExtension.class)
class ServicioTest {

    /** Subclase minima solo para poder instanciar Servicio (es abstracta). */
    private static class ServicioDePrueba extends Servicio {
        ServicioDePrueba(ConexionProvider proveedor) {
            super(proveedor);
        }
    }

    @Test
    void conectarUsaLaConexionDelProveedorInyectado() throws Exception {
        Connection conexionFalsa = mock(Connection.class);
        ServicioDePrueba servicio = new ServicioDePrueba(() -> conexionFalsa);

        servicio.conectar();

        assertSame(conexionFalsa, servicio.conexion);
    }

    @Test
    void conectarLanzaGlobalExceptionSiNoHayDriver() {
        ServicioDePrueba servicio = new ServicioDePrueba(() -> {
            throw new ClassNotFoundException("sin driver");
        });

        GlobalException ex = assertThrows(GlobalException.class, servicio::conectar);

        assertEquals("No se ha localizado el driver", ex.getMessage());
    }

    @Test
    void conectarLanzaNoDataExceptionSiFallaLaConexion() {
        ServicioDePrueba servicio = new ServicioDePrueba(() -> {
            throw new SQLException("listener caido", "08001", 12541);
        });

        NoDataException ex = assertThrows(NoDataException.class, servicio::conectar);

        assertEquals("La base de datos no se encuentra disponible", ex.getMessage());
    }

    @Test
    void desconectarCierraLaConexionAbierta() throws Exception {
        Connection conexionFalsa = mock(Connection.class);
        when(conexionFalsa.isClosed()).thenReturn(false);
        ServicioDePrueba servicio = new ServicioDePrueba(() -> conexionFalsa);
        servicio.conectar();

        servicio.desconectar();

        verify(conexionFalsa, times(1)).close();
    }

    @Test
    void desconectarNoHaceNadaSiYaEstaCerrada() throws Exception {
        Connection conexionFalsa = mock(Connection.class);
        when(conexionFalsa.isClosed()).thenReturn(true);
        ServicioDePrueba servicio = new ServicioDePrueba(() -> conexionFalsa);
        servicio.conectar();

        assertDoesNotThrow(servicio::desconectar);
        verify(conexionFalsa, never()).close();
    }

    @Test
    void registrarCursorSalidaUsaElTipoCentralizadoEnLaClaseBase() throws Exception {
        CallableStatement cs = mock(CallableStatement.class);

        Servicio.registrarCursorSalida(cs, 1);

        verify(cs).registerOutParameter(1, Types.REF_CURSOR);
    }

    @Test
    void errorSqlEnvuelveElMensajeMapeado() {
        ServicioDePrueba servicio = new ServicioDePrueba(() -> mock(Connection.class));

        GlobalException ex = servicio.errorSql(sqlEx(1));

        assertEquals("Ya existe un registro con ese identificador", ex.getMessage());
    }

    @Test
    void mapaDeErroresOracleCubreTodosLosCodigosDocumentados() {
        assertEquals("Ya existe un registro con ese identificador", Servicio.mensajeError(sqlEx(1)));
        assertEquals("Referencia invalida (el registro relacionado no existe)", Servicio.mensajeError(sqlEx(2291)));
        assertEquals("No se puede eliminar: tiene registros asociados", Servicio.mensajeError(sqlEx(2292)));
        assertEquals("Dato demasiado largo", Servicio.mensajeError(sqlEx(12899)));
        assertEquals("Credenciales de base de datos invalidas", Servicio.mensajeError(sqlEx(1017)));
        assertEquals("La base de datos no se encuentra disponible", Servicio.mensajeError(sqlEx(12541)));
        assertEquals("La base de datos no se encuentra disponible", Servicio.mensajeError(sqlEx(17002)));
        assertEquals("Error de base de datos", Servicio.mensajeError(sqlEx(99999)));
    }

    private static SQLException sqlEx(int codigoOracle) {
        return new SQLException("mensaje interno de Oracle", "ORA", codigoOracle);
    }
}
