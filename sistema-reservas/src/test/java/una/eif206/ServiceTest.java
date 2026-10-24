package una.eif206;

import una.eif206.data.Data;
import una.eif206.model.Admin;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.Usuario;
import una.eif206.model.enums.EstadoReserva;
import una.eif206.service.CalendarioService;
import una.eif206.service.CategoriaService;
import una.eif206.service.EstadisticasService;
import una.eif206.service.FuncionarioService;
import una.eif206.service.RecursoService;
import una.eif206.service.ReservaService;
import una.eif206.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceTest {

    private UsuarioService usuarioService;
    private FuncionarioService funcionarioService;
    private CategoriaService categoriaService;
    private RecursoService recursoService;
    private ReservaService reservaService;
    private EstadisticasService estadisticasService;
    private CalendarioService calendarioService;

    @BeforeEach
    void resetData() {
        Data data = new Data();
        usuarioService = new UsuarioService(data);
        funcionarioService = new FuncionarioService(data);
        categoriaService = new CategoriaService(data);
        recursoService = new RecursoService(data);
        reservaService = new ReservaService(data);
        estadisticasService = new EstadisticasService(data);
        calendarioService = new CalendarioService(data);
    }

    @Test
    void testLoginExitoso() {
        Usuario usuario = usuarioService.login("admin", "admin123");
        assertTrue(usuario instanceof Admin);
    }

    @Test
    void testLoginFallido() {
        assertNull(usuarioService.login("admin", "wrongpass"));
    }

    @Test
    void testLoginFuncionario() {
        Usuario usuario = usuarioService.login("111", "111");
        assertTrue(usuario instanceof Funcionario);
    }

    @Test
    void testCambiarClaveExitosa() {
        Usuario usuario = usuarioService.login("admin", "admin123");
        String error = usuarioService.cambiarClave(usuario, "admin123", "nuevaClave");
        assertNull(error);
        assertEquals("nuevaClave", usuario.getClave());
    }

    @Test
    void testCambiarClaveIncorrecta() {
        Usuario usuario = usuarioService.login("admin", "admin123");
        String error = usuarioService.cambiarClave(usuario, "claveMala", "nuevaClave");
        assertNotNull(error);
    }

    @Test
    void testCreateFuncionario() {
        int before = funcionarioService.findAll().size();
        funcionarioService.create(new Funcionario("333", "Nuevo Empleado", "TI", "1234"));
        assertEquals(before + 1, funcionarioService.findAll().size());
    }

    @Test
    void testCreateFuncionarioDuplicado() {
        Funcionario duplicado = new Funcionario("111", "Otro Nombre", "TI", "0000");
        String error = funcionarioService.create(duplicado);
        assertNotNull(error);
    }

    @Test
    void testDeleteFuncionario() {
        Funcionario existente = funcionarioService.findById("111");
        funcionarioService.delete(existente);
        assertNull(funcionarioService.findById("111"));
    }

    @Test
    void testUpdateFuncionario() {
        funcionarioService.update(new Funcionario("111", "Nombre Actualizado", "Ventas", "9999"));
        Funcionario actualizado = funcionarioService.findById("111");
        assertEquals("Nombre Actualizado", actualizado.getNombre());
        assertEquals("9999", actualizado.getTelefono());
    }

    @Test
    void testSearchFuncionarios() {
        List<Funcionario> resultado = funcionarioService.search(new Funcionario("", "juan", "", ""));
        assertFalse(resultado.isEmpty());
        assertTrue(resultado.stream().anyMatch(f -> f.getId().equals("111")));
    }

    @Test
    void testCreateCategoria() {
        int before = categoriaService.findAll().size();
        CategoriaRecurso categoria = new CategoriaRecurso("", "Categoria Nueva");
        categoriaService.create(categoria);
        assertEquals(before + 1, categoriaService.findAll().size());
        assertTrue(categoria.getId().matches("CAT-\\d{6}"));
    }

    @Test
    void testCreateRecurso() {
        int before = recursoService.findAll().size();
        CategoriaRecurso categoria = categoriaService.findAll().get(0);
        recursoService.create(new Recurso("999", "Recurso de prueba", categoria));
        assertEquals(before + 1, recursoService.findAll().size());
    }

    @Test
    void testGenerarIdCategoria() {
        Data data = new Data();
        assertTrue(data.generarIdCategoria().matches("CAT-\\d{6}"));
    }

    // ===================== RESERVAS =====================

    @Test
    void testCreateReserva() {
        Funcionario funcionario = funcionarioService.findById("111");
        Reserva reserva = new Reserva(reservaService.generarId(), "Reunion de proyecto",
                "2026-02-10", "09:00", "10:00", funcionario);
        reserva.setRecursos(List.of(recursoService.findAll().get(0)));

        String error = reservaService.create(reserva);

        assertNull(error);
        assertTrue(reservaService.findAll().stream()
                .anyMatch(r -> r.getId().equals(reserva.getId())));
    }

    @Test
    void testCreateReservaDuplicada() {
        Funcionario funcionario = funcionarioService.findById("111");
        String id = reservaService.generarId();
        Reserva reserva = new Reserva(id, "Reunion", "2026-02-10", "09:00", "10:00", funcionario);
        reserva.setRecursos(List.of(recursoService.findAll().get(0)));
        assertNull(reservaService.create(reserva));

        Reserva duplicada = new Reserva(id, "Otra reunion", "2026-02-11", "11:00", "12:00", funcionario);
        duplicada.setRecursos(List.of(recursoService.findAll().get(0)));
        assertNotNull(reservaService.create(duplicada));
    }

    @Test
    void testCancelarReserva() {
        Funcionario funcionario = funcionarioService.findById("111");
        Reserva reserva = new Reserva(reservaService.generarId(), "Reunion",
                LocalDate.now().plusDays(7).toString(), "09:00", "10:00", funcionario);
        reserva.setRecursos(List.of(recursoService.findAll().get(0)));
        reservaService.create(reserva);

        reservaService.cancelar(reserva);

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
    }

    @Test
    void testFindReservasByFuncionario() {
        Funcionario f1 = funcionarioService.findById("111");
        Funcionario f2 = funcionarioService.findById("222");

        Reserva r1 = new Reserva(reservaService.generarId(), "Reunion A", "2026-02-10", "09:00", "10:00", f1);
        r1.setRecursos(List.of(recursoService.findAll().get(0)));
        Reserva r2 = new Reserva(reservaService.generarId(), "Reunion B", "2026-02-10", "11:00", "12:00", f2);
        r2.setRecursos(List.of(recursoService.findAll().get(0)));
        reservaService.create(r1);
        reservaService.create(r2);

        List<Reserva> reservasF1 = reservaService.findByFuncionario(f1);
        List<Reserva> reservasF2 = reservaService.findByFuncionario(f2);

        assertTrue(reservasF1.stream().anyMatch(r -> r.getId().equals(r1.getId())));
        assertTrue(reservasF1.stream().noneMatch(r -> r.getId().equals(r2.getId())));
        assertTrue(reservasF2.stream().anyMatch(r -> r.getId().equals(r2.getId())));
        assertTrue(reservasF2.stream().noneMatch(r -> r.getId().equals(r1.getId())));
    }

    @Test
    void testSolapamientoMismoRecurso() {
        Funcionario funcionario = funcionarioService.findById("111");
        CategoriaRecurso categoria = categoriaService.findAll().stream()
                .filter(c -> c.getId().equals("CAT-000002")).findFirst().orElseThrow();
        Recurso recurso = recursoService.findByCategoria(categoria).get(0);

        Reserva r1 = new Reserva(reservaService.generarId(), "Reunion A", "2026-02-10", "09:00", "11:00", funcionario);
        r1.setRecursos(List.of(recurso));
        reservaService.create(r1);

        Reserva r2 = new Reserva(reservaService.generarId(), "Reunion B", "2026-02-10", "10:00", "12:00", funcionario);
        r2.setRecursos(List.of(recurso));

        assertNotNull(reservaService.create(r2));
    }

    @Test
    void testSinSolapamientoHoraLibre() {
        Funcionario funcionario = funcionarioService.findById("111");
        CategoriaRecurso categoria = categoriaService.findAll().stream()
                .filter(c -> c.getId().equals("CAT-000002")).findFirst().orElseThrow();
        Recurso recurso = recursoService.findByCategoria(categoria).get(0);

        Reserva r1 = new Reserva(reservaService.generarId(), "Reunion A", "2026-02-10", "09:00", "11:00", funcionario);
        r1.setRecursos(List.of(recurso));
        reservaService.create(r1);

        Reserva r2 = new Reserva(reservaService.generarId(), "Reunion B", "2026-02-10", "11:00", "13:00", funcionario);
        r2.setRecursos(List.of(recurso));

        assertNull(reservaService.create(r2));

        assertTrue(reservaService.findAll().stream().anyMatch(r -> r.getId().equals(r2.getId())));
    }

    @Test
    void testGenerarIdReserva() {
        String id = reservaService.generarId();
        assertNotNull(id);
        assertFalse(id.isEmpty());
    }

    // ===================== CATEGORIAS =====================

    @Test
    void testUpdateCategoria() {
        CategoriaRecurso categoria = new CategoriaRecurso("", "Sala VIP");
        categoriaService.create(categoria);

        categoriaService.update(new CategoriaRecurso(categoria.getId(), "Sala VIP Actualizada"));

        CategoriaRecurso actualizada = categoriaService.findAll().stream()
                .filter(c -> c.getId().equals(categoria.getId())).findFirst().orElseThrow();
        assertEquals("Sala VIP Actualizada", actualizada.getDescripcion());
    }

    @Test
    void testSearchCategorias() {
        categoriaService.create(new CategoriaRecurso("", "Sala de Conferencias"));
        categoriaService.create(new CategoriaRecurso("", "Sala de Descanso"));

        List<CategoriaRecurso> resultado = categoriaService.search(new CategoriaRecurso("", "conferencias"));

        assertTrue(resultado.stream().anyMatch(c -> c.getDescripcion().equals("Sala de Conferencias")));
        assertTrue(resultado.stream().noneMatch(c -> c.getDescripcion().equals("Sala de Descanso")));
    }

    @Test
    void testDeleteCategoria() {
        CategoriaRecurso categoria = new CategoriaRecurso("", "Categoria Temporal");
        categoriaService.create(categoria);

        categoriaService.delete(categoria);

        assertTrue(categoriaService.findAll().stream().noneMatch(c -> c.getId().equals(categoria.getId())));
    }

    // ===================== RECURSOS =====================

    @Test
    void testUpdateRecurso() {
        CategoriaRecurso categoria = categoriaService.findAll().get(0);
        Recurso recurso = new Recurso("R-500", "Recurso Original", categoria);
        recursoService.create(recurso);

        recursoService.update(new Recurso("R-500", "Recurso Modificado", categoria));

        Recurso actualizado = recursoService.findAll().stream()
                .filter(r -> r.getId().equals("R-500")).findFirst().orElseThrow();
        assertEquals("Recurso Modificado", actualizado.getDescripcion());
    }

    @Test
    void testFindRecursosByCategoria() {
        CategoriaRecurso categoriaA = new CategoriaRecurso("", "Categoria A");
        CategoriaRecurso categoriaB = new CategoriaRecurso("", "Categoria B");
        categoriaService.create(categoriaA);
        categoriaService.create(categoriaB);

        recursoService.create(new Recurso("R-A1", "Recurso A1", categoriaA));
        recursoService.create(new Recurso("R-B1", "Recurso B1", categoriaB));

        List<Recurso> recursosA = recursoService.findByCategoria(categoriaA);

        assertTrue(recursosA.stream().anyMatch(r -> r.getId().equals("R-A1")));
        assertTrue(recursosA.stream().noneMatch(r -> r.getId().equals("R-B1")));
    }

    // ===================== ESTADISTICAS =====================

    @Test
    void testGetEstadisticasRecursos() throws Exception {
        Funcionario funcionario = funcionarioService.findById("111");
        CategoriaRecurso categoria = categoriaService.findAll().get(0);
        Recurso recurso = recursoService.findByCategoria(categoria).get(0);

        Reserva reserva = new Reserva(reservaService.generarId(), "Uso de laptop", "2026-03-01",
                "09:00", "10:00", funcionario);
        reserva.setRecursos(List.of(recurso));
        reservaService.create(reserva);

        Map<String, Integer> estadisticas = estadisticasService.getEstadisticasRecursos("2026-03-01", "2026-03-01");

        assertTrue(estadisticas.getOrDefault(categoria.getDescripcion(), 0) > 0);
    }

    @Test
    void testGetEstadisticasActividades() throws Exception {
        Funcionario funcionario = funcionarioService.findById("111");
        Reserva reserva = new Reserva(reservaService.generarId(), "Capacitacion", "2026-03-05",
                "09:00", "10:00", funcionario);
        reserva.setRecursos(List.of(recursoService.findAll().get(0)));
        reservaService.create(reserva);

        Map<String, Integer> estadisticas = estadisticasService.getEstadisticasActividades("2026-03-01", "2026-03-10");

        String semana = LocalDate.of(2026, 3, 5).with(DayOfWeek.MONDAY).toString();
        assertTrue(estadisticas.containsKey(semana));
        assertEquals(1, estadisticas.get(semana));
    }

    // ===================== CALENDARIZACION =====================

    @Test
    void testGetCalendario() {
        Funcionario funcionario = funcionarioService.findById("111");
        CategoriaRecurso categoria = categoriaService.findAll().stream()
                .filter(c -> c.getId().equals("CAT-000002")).findFirst().orElseThrow();
        Recurso recurso = recursoService.findByCategoria(categoria).get(0);

        Reserva reserva = new Reserva(reservaService.generarId(), "Reunion Directiva", "2026-03-10",
                "09:00", "10:00", funcionario);
        reserva.setRecursos(List.of(recurso));
        reservaService.create(reserva);

        String[][] matriz = calendarioService.getCalendario("2026-03-10", categoria);

        assertTrue(matriz.length > 0);
        boolean contieneActividad = false;
        for (String[] fila : matriz) {
            for (String celda : fila) {
                if (celda != null && celda.contains("Reunion Directiva")) {
                    contieneActividad = true;
                }
            }
        }
        assertTrue(contieneActividad);
    }
}
