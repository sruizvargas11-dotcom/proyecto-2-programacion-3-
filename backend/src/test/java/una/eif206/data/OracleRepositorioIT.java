package una.eif206.data;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.Usuario;
import una.eif206.model.enums.EstadoReserva;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba OracleRepositorio contra el Oracle real indicado por DB_URL/DB_USER/
 * DB_PASSWORD (nunca se imprimen esos valores). Solo corre si DB_URL esta
 * definida; de lo contrario se saltea (igual que un OracleSmokeIT clasico).
 *
 * Todos los ids que crea esta prueba llevan el prefijo TST (o, para las
 * categorias/reservas cuyo id lo genera la secuencia, una descripcion/
 * actividad con prefijo TST) y cada prueba limpia sus propias filas al
 * terminar, con una conexion JDBC cruda (no hay un "eliminarReserva" en el
 * script: una reserva real nunca se borra, solo se cancela; aqui se borra
 * porque es basura de prueba, no dato de negocio).
 */
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class OracleRepositorioIT {

    private final Repositorio repositorio = new OracleRepositorio();

    private static String nuevoSufijo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void loginFuncionarioYCambiarClave() throws Exception {
        String id = "TST-LOGIN-" + nuevoSufijo();
        Funcionario f = new Funcionario(id, "TST Login", "TI", "0000");
        try {
            repositorio.insertarFuncionario(f);

            Usuario logueado = repositorio.login(id, id);
            assertNotNull(logueado);
            assertEquals(id, logueado.getId());

            repositorio.cambiarClave(id, "claveNueva");

            assertNotNull(repositorio.login(id, "claveNueva"));
            assertNull(repositorio.login(id, id));
        } finally {
            eliminarFuncionario(id);
        }
    }

    @Test
    void funcionarioCrudRealContraOracle() throws Exception {
        String id = "TST-FUNC-" + nuevoSufijo();
        try {
            repositorio.insertarFuncionario(new Funcionario(id, "TST Original", "TI", "1111"));
            assertNotNull(repositorio.buscarFuncionarioPorId(id));

            repositorio.modificarFuncionario(new Funcionario(id, "TST Modificado", "Ventas", "2222"));
            Funcionario modificado = repositorio.buscarFuncionarioPorId(id);
            assertEquals("TST Modificado", modificado.getNombre());
            assertEquals("Ventas", modificado.getDepartamento());

            repositorio.eliminarFuncionario(id);
            assertNull(repositorio.buscarFuncionarioPorId(id));
        } finally {
            eliminarFuncionario(id);
        }
    }

    @Test
    void categoriaYRecursoCrudRealContraOracle() throws Exception {
        String categoriaId = null;
        String recursoId = "TST-REC-" + nuevoSufijo();
        try {
            categoriaId = repositorio.insertarCategoria("TST Categoria " + nuevoSufijo());
            assertNotNull(repositorio.buscarCategoriaPorId(categoriaId));

            repositorio.modificarCategoria(new CategoriaRecurso(categoriaId, "TST Categoria modificada"));
            assertEquals("TST Categoria modificada", repositorio.buscarCategoriaPorId(categoriaId).getDescripcion());

            Recurso recurso = new Recurso(recursoId, "TST Recurso", repositorio.buscarCategoriaPorId(categoriaId));
            repositorio.insertarRecurso(recurso);
            assertNotNull(repositorio.buscarRecursoPorId(recursoId));
            assertTrue(repositorio.listarRecursosPorCategoria(categoriaId).stream()
                    .anyMatch(r -> r.getId().equals(recursoId)));

            repositorio.eliminarRecurso(recursoId);
            assertNull(repositorio.buscarRecursoPorId(recursoId));

            repositorio.eliminarCategoria(categoriaId);
            assertNull(repositorio.buscarCategoriaPorId(categoriaId));
        } finally {
            eliminarRecurso(recursoId);
            eliminarCategoria(categoriaId);
        }
    }

    @Test
    void reservaTransaccionCompletaContraOracle() throws Exception {
        String funcionarioId = "TST-RF-" + nuevoSufijo();
        String recursoId = "TST-RR-" + nuevoSufijo();
        String categoriaId = null;
        String reservaId = null;
        try {
            repositorio.insertarFuncionario(new Funcionario(funcionarioId, "TST Reservante", "TI", "3333"));
            categoriaId = repositorio.insertarCategoria("TST Categoria reserva " + nuevoSufijo());
            CategoriaRecurso categoria = repositorio.buscarCategoriaPorId(categoriaId);
            repositorio.insertarRecurso(new Recurso(recursoId, "TST Recurso reserva", categoria));

            reservaId = repositorio.siguienteIdReserva();
            Funcionario funcionario = repositorio.buscarFuncionarioPorId(funcionarioId);
            Recurso recurso = repositorio.buscarRecursoPorId(recursoId);
            Reserva reserva = new Reserva(reservaId, "TST Reunion", "2031-03-10", "09:00", "10:00", funcionario);
            reserva.setRecursos(List.of(recurso));

            repositorio.crearReserva(reserva);

            Reserva encontrada = repositorio.buscarReservaPorId(reservaId);
            assertNotNull(encontrada);
            assertEquals("TST Reunion", encontrada.getActividad());
            assertEquals(funcionarioId, encontrada.getFuncionario().getId());
            assertEquals(1, encontrada.getRecursos().size());
            assertEquals(recursoId, encontrada.getRecursos().get(0).getId());

            String idCreada = reservaId;
            assertTrue(repositorio.listarReservasPorFuncionario(funcionarioId).stream()
                    .anyMatch(r -> r.getId().equals(idCreada)));
            assertTrue(repositorio.listarIdsRecursosOcupados("2031-03-10", "09:00", "10:00").contains(recursoId));

            repositorio.cancelarReserva(reservaId);
            assertEquals(EstadoReserva.CANCELADA, repositorio.buscarReservaPorId(reservaId).getEstado());
        } finally {
            eliminarReserva(reservaId);
            eliminarRecurso(recursoId);
            eliminarCategoria(categoriaId);
            eliminarFuncionario(funcionarioId);
        }
    }

    @Test
    void reservaDuplicadaEnMismoHorarioFallaContraOracle() throws Exception {
        String funcionarioId = "TST-DUP-" + nuevoSufijo();
        String recursoId = "TST-DUPR-" + nuevoSufijo();
        String categoriaId = null;
        String reservaId = null;
        try {
            repositorio.insertarFuncionario(new Funcionario(funcionarioId, "TST Duplicado", "TI", "4444"));
            categoriaId = repositorio.insertarCategoria("TST Categoria duplicado " + nuevoSufijo());
            CategoriaRecurso categoria = repositorio.buscarCategoriaPorId(categoriaId);
            repositorio.insertarRecurso(new Recurso(recursoId, "TST Recurso duplicado", categoria));

            Funcionario funcionario = repositorio.buscarFuncionarioPorId(funcionarioId);
            Recurso recurso = repositorio.buscarRecursoPorId(recursoId);

            reservaId = repositorio.siguienteIdReserva();
            Reserva primera = new Reserva(reservaId, "TST Reunion A", "2031-04-01", "09:00", "10:00", funcionario);
            primera.setRecursos(List.of(recurso));
            repositorio.crearReserva(primera);

            String segundoId = repositorio.siguienteIdReserva();
            Reserva segunda = new Reserva(segundoId, "TST Reunion B", "2031-04-01", "09:30", "10:30", funcionario);
            segunda.setRecursos(List.of(recurso));

            GlobalException ex = assertThrows(GlobalException.class, () -> repositorio.crearReserva(segunda));
            assertEquals("No hay disponibilidad para el recurso en ese horario", ex.getMessage());
            assertNull(repositorio.buscarReservaPorId(segundoId));
        } finally {
            eliminarReserva(reservaId);
            eliminarRecurso(recursoId);
            eliminarCategoria(categoriaId);
            eliminarFuncionario(funcionarioId);
        }
    }

    @Test
    void estadisticasRealesContraOracle() throws Exception {
        String funcionarioId = "TST-STAT-" + nuevoSufijo();
        String recursoId = "TST-STATR-" + nuevoSufijo();
        String categoriaId = null;
        String reservaId = null;
        try {
            repositorio.insertarFuncionario(new Funcionario(funcionarioId, "TST Estadisticas", "TI", "5555"));
            categoriaId = repositorio.insertarCategoria("TST Categoria stats " + nuevoSufijo());
            CategoriaRecurso categoria = repositorio.buscarCategoriaPorId(categoriaId);
            repositorio.insertarRecurso(new Recurso(recursoId, "TST Recurso stats", categoria));

            Funcionario funcionario = repositorio.buscarFuncionarioPorId(funcionarioId);
            Recurso recurso = repositorio.buscarRecursoPorId(recursoId);
            reservaId = repositorio.siguienteIdReserva();
            Reserva reserva = new Reserva(reservaId, "TST Capacitacion", "2031-05-05", "09:00", "10:00", funcionario);
            reserva.setRecursos(List.of(recurso));
            repositorio.crearReserva(reserva);

            assertEquals(1, repositorio.estadisticasRecursos("2031-05-05", "2031-05-05")
                    .getOrDefault(categoria.getDescripcion(), 0));
            assertTrue(repositorio.estadisticasActividades("2031-05-01", "2031-05-07")
                    .values().stream().anyMatch(v -> v >= 1));
        } finally {
            eliminarReserva(reservaId);
            eliminarRecurso(recursoId);
            eliminarCategoria(categoriaId);
            eliminarFuncionario(funcionarioId);
        }
    }

    // ===================== limpieza con JDBC crudo =====================
    // No existe un "eliminarReserva" en el script (una reserva real nunca se
    // borra, solo se cancela); para no dejar basura de prueba en el esquema,
    // esta limpieza borra directamente las filas que creo esta clase.

    private void eliminarReserva(String id) {
        if (id == null) return;
        ejecutarSql("DELETE FROM RESERVA_RECURSO WHERE RESERVA_ID = ?", id);
        ejecutarSql("DELETE FROM RESERVA WHERE ID = ?", id);
    }

    private void eliminarRecurso(String id) {
        if (id == null) return;
        ejecutarSql("DELETE FROM RECURSO WHERE ID = ?", id);
    }

    private void eliminarCategoria(String id) {
        if (id == null) return;
        ejecutarSql("DELETE FROM CATEGORIA_RECURSO WHERE ID = ?", id);
    }

    private void eliminarFuncionario(String id) {
        if (id == null) return;
        ejecutarSql("DELETE FROM USUARIO WHERE ID = ?", id);
    }

    private void ejecutarSql(String sql, Object... parametros) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            try (Connection c = DriverManager.getConnection(
                    Config.get("DB_URL"), Config.get("DB_USER"), Config.get("DB_PASSWORD"));
                 PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < parametros.length; i++) {
                    ps.setObject(i + 1, parametros[i]);
                }
                ps.executeUpdate();
            }
        } catch (ClassNotFoundException | SQLException ignored) {
            // Limpieza de mejor esfuerzo: un fallo aqui no debe tapar el resultado de la prueba.
        }
    }
}
