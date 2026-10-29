package una.eif206.data;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.Usuario;
import una.eif206.model.enums.EstadoReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrato de Repositorio ejercitado contra MemoriaRepositorio. OracleRepositorio
 * se ejercita por separado en OracleRepositorioIT (requiere DB_URL), pero
 * ambas implementaciones deben comportarse igual para estas operaciones.
 */
class RepositorioContractTest {

    private Repositorio repositorio;

    @BeforeEach
    void setUp() {
        repositorio = new MemoriaRepositorio();
    }

    @Test
    void loginYCambiarClave() throws Exception {
        Usuario admin = repositorio.login("admin", "admin123");
        assertNotNull(admin);

        repositorio.cambiarClave("admin", "nuevaClave");

        assertNotNull(repositorio.login("admin", "nuevaClave"));
        assertNull(repositorio.login("admin", "admin123"));
    }

    @Test
    void funcionarioCrudCompleto() throws Exception {
        Funcionario f = new Funcionario("TST-F1", "Persona de prueba", "TI", "0000");
        repositorio.insertarFuncionario(f);

        assertNotNull(repositorio.buscarFuncionarioPorId("TST-F1"));
        assertTrue(repositorio.buscarFuncionariosPorTexto("prueba").stream()
                .anyMatch(x -> x.getId().equals("TST-F1")));

        f.setNombre("Nombre actualizado");
        repositorio.modificarFuncionario(f);
        assertEquals("Nombre actualizado", repositorio.buscarFuncionarioPorId("TST-F1").getNombre());

        repositorio.eliminarFuncionario("TST-F1");
        assertNull(repositorio.buscarFuncionarioPorId("TST-F1"));
    }

    @Test
    void categoriaCrudCompleto() throws Exception {
        String id = repositorio.insertarCategoria("Categoria de prueba");
        assertNotNull(repositorio.buscarCategoriaPorId(id));

        repositorio.modificarCategoria(new CategoriaRecurso(id, "Descripcion actualizada"));
        assertEquals("Descripcion actualizada", repositorio.buscarCategoriaPorId(id).getDescripcion());

        repositorio.eliminarCategoria(id);
        assertNull(repositorio.buscarCategoriaPorId(id));
    }

    @Test
    void recursoCrudCompleto() throws Exception {
        String categoriaId = repositorio.insertarCategoria("Categoria recurso");
        Recurso r = new Recurso("TST-R1", "Recurso de prueba", repositorio.buscarCategoriaPorId(categoriaId));
        repositorio.insertarRecurso(r);

        assertNotNull(repositorio.buscarRecursoPorId("TST-R1"));
        assertTrue(repositorio.listarRecursosPorCategoria(categoriaId).stream()
                .anyMatch(x -> x.getId().equals("TST-R1")));

        repositorio.eliminarRecurso("TST-R1");
        assertNull(repositorio.buscarRecursoPorId("TST-R1"));
        repositorio.eliminarCategoria(categoriaId);
    }

    @Test
    void reservaCrearBuscarCancelar() throws Exception {
        Funcionario f = repositorio.buscarFuncionarioPorId("111");
        Recurso r = repositorio.listarRecursos().get(0);
        String fecha = LocalDate.now().plusDays(7).toString();
        String id = repositorio.siguienteIdReserva();
        Reserva reserva = new Reserva(id, "Reunion de prueba", fecha, "09:00", "10:00", f);
        reserva.setRecursos(List.of(r));

        repositorio.crearReserva(reserva);

        Reserva encontrada = repositorio.buscarReservaPorId(id);
        assertNotNull(encontrada);
        assertTrue(repositorio.listarReservasPorFuncionario(f.getId()).stream().anyMatch(x -> x.getId().equals(id)));
        assertTrue(repositorio.listarIdsRecursosOcupados(fecha, "09:00", "10:00").contains(r.getId()));

        repositorio.cancelarReserva(id);
        assertEquals(EstadoReserva.CANCELADA, repositorio.buscarReservaPorId(id).getEstado());
    }

    @Test
    void reservaDuplicadaEnMismoHorarioFalla() throws Exception {
        Funcionario f = repositorio.buscarFuncionarioPorId("111");
        Recurso r = repositorio.listarRecursos().get(0);
        String fecha = LocalDate.now().plusDays(10).toString();

        Reserva primera = new Reserva(repositorio.siguienteIdReserva(), "Reunion A", fecha, "09:00", "10:00", f);
        primera.setRecursos(List.of(r));
        repositorio.crearReserva(primera);

        Reserva segunda = new Reserva(repositorio.siguienteIdReserva(), "Reunion B", fecha, "09:30", "10:30", f);
        segunda.setRecursos(List.of(r));

        assertThrows(GlobalException.class, () -> repositorio.crearReserva(segunda));
    }

    @Test
    void estadisticas() throws Exception {
        Funcionario f = repositorio.buscarFuncionarioPorId("111");
        CategoriaRecurso categoria = repositorio.listarCategorias().get(0);
        Recurso r = repositorio.listarRecursosPorCategoria(categoria.getId()).get(0);

        Reserva reserva = new Reserva(repositorio.siguienteIdReserva(), "Uso", "2026-05-01", "09:00", "10:00", f);
        reserva.setRecursos(List.of(r));
        repositorio.crearReserva(reserva);

        assertEquals(1, repositorio.estadisticasRecursos("2026-05-01", "2026-05-01").get(categoria.getDescripcion()));
        assertFalse(repositorio.estadisticasActividades("2026-05-01", "2026-05-01").isEmpty());
    }
}
