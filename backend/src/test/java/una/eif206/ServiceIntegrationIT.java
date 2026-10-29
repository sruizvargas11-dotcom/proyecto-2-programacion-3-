package una.eif206;

import una.eif206.data.MemoriaRepositorio;
import una.eif206.data.Repositorio;
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

public class ServiceIntegrationIT {

    private UsuarioService usuarioService;
    private FuncionarioService funcionarioService;
    private CategoriaService categoriaService;
    private RecursoService recursoService;
    private ReservaService reservaService;
    private EstadisticasService estadisticasService;
    private CalendarioService calendarioService;

    @BeforeEach
    void resetData() {
        Repositorio repositorio = new MemoriaRepositorio();
        usuarioService = new UsuarioService(repositorio);
        funcionarioService = new FuncionarioService(repositorio);
        categoriaService = new CategoriaService(repositorio);
        recursoService = new RecursoService(repositorio);
        reservaService = new ReservaService(repositorio);
        estadisticasService = new EstadisticasService(repositorio);
        calendarioService = new CalendarioService(repositorio);
    }

    @Test
    void testFlujoCompletoFuncionario() {
        funcionarioService.create(new Funcionario("444", "Ana Ramirez", "Ventas", "5555"));

        Funcionario creado = funcionarioService.findById("444");
        assertEquals("Ana Ramirez", creado.getNombre());

        funcionarioService.update(new Funcionario("444", "Ana Maria Ramirez", "Ventas", "6666"));
        Funcionario actualizado = funcionarioService.findById("444");
        assertEquals("Ana Maria Ramirez", actualizado.getNombre());
        assertEquals("6666", actualizado.getTelefono());

        funcionarioService.delete(actualizado);
        assertNull(funcionarioService.findById("444"));
    }

    @Test
    void testFlujoCompletoCategoria() {
        CategoriaRecurso categoria = new CategoriaRecurso("", "Proyector");
        categoriaService.create(categoria);

        List<CategoriaRecurso> encontradas = categoriaService.search(new CategoriaRecurso("", "proyector"));
        assertTrue(encontradas.stream().anyMatch(c -> c.getId().equals(categoria.getId())));

        categoriaService.delete(categoria);
        assertTrue(categoriaService.findAll().stream().noneMatch(c -> c.getId().equals(categoria.getId())));
    }

    @Test
    void testFlujoCompletoReserva() {
        Funcionario funcionario = funcionarioService.findById("111");
        String fecha = LocalDate.now().plusDays(7).toString();
        Reserva reserva = new Reserva(reservaService.generarId(), "Reunion de equipo",
                fecha, "10:00", "11:00", funcionario);
        reserva.setRecursos(List.of(recursoService.findAll().get(0)));

        reservaService.create(reserva);
        reservaService.cancelar(reserva);

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
    }

    @Test
    void testLoginYCambiarClave() {
        Usuario usuario = usuarioService.login("admin", "admin123");
        usuarioService.cambiarClave(usuario, "admin123", "nuevaClaveSegura");

        Usuario relogueado = usuarioService.login("admin", "nuevaClaveSegura");
        assertNotNull(relogueado);

        assertNull(usuarioService.login("admin", "admin123"));
    }

    @Test
    void testFlujoCompletoReservaConSolapamiento() {
        Funcionario funcionario = funcionarioService.findById("111");
        CategoriaRecurso categoria = categoriaService.findAll().stream()
                .filter(c -> c.getId().equals("CAT-000002")).findFirst().orElseThrow();
        Recurso recurso = recursoService.findByCategoria(categoria).get(0);
        String fecha = LocalDate.now().plusDays(7).toString();

        Reserva primera = new Reserva(reservaService.generarId(), "Reunion 1", fecha,
                "09:00", "11:00", funcionario);
        primera.setRecursos(List.of(recurso));
        reservaService.create(primera);

        Reserva solapada = new Reserva(reservaService.generarId(), "Reunion 2", fecha,
                "10:00", "12:00", funcionario);
        solapada.setRecursos(List.of(recurso));
        assertNotNull(reservaService.create(solapada));

        Reserva sinSolape = new Reserva(reservaService.generarId(), "Reunion 3", fecha,
                "11:00", "13:00", funcionario);
        sinSolape.setRecursos(List.of(recurso));
        reservaService.create(sinSolape);
        assertTrue(reservaService.findAll().stream().anyMatch(r -> r.getId().equals(sinSolape.getId())));

        reservaService.cancelar(primera);
        assertEquals(EstadoReserva.CANCELADA, primera.getEstado());
    }

    @Test
    void testFlujoEstadisticas() throws Exception {
        Funcionario funcionario = funcionarioService.findById("111");
        CategoriaRecurso categoria = categoriaService.findAll().get(0);
        Recurso recurso = recursoService.findByCategoria(categoria).get(0);

        Reserva r1 = new Reserva(reservaService.generarId(), "Capacitacion", "2026-05-01", "09:00", "10:00", funcionario);
        r1.setRecursos(List.of(recurso));
        reservaService.create(r1);

        Reserva r2 = new Reserva(reservaService.generarId(), "Capacitacion", "2026-05-02", "09:00", "10:00", funcionario);
        r2.setRecursos(List.of(recurso));
        reservaService.create(r2);

        Reserva r3 = new Reserva(reservaService.generarId(), "Revision de proyecto", "2026-05-03", "09:00", "10:00", funcionario);
        r3.setRecursos(List.of(recursoService.findByCategoria(categoria).get(1)));
        reservaService.create(r3);

        Map<String, Integer> estadisticasRecursos = estadisticasService.getEstadisticasRecursos("2026-05-01", "2026-05-03");
        assertEquals(3, estadisticasRecursos.getOrDefault(categoria.getDescripcion(), 0));

        Map<String, Integer> estadisticasActividades = estadisticasService.getEstadisticasActividades("2026-05-01", "2026-05-03");
        String lunes = LocalDate.parse("2026-05-01").with(DayOfWeek.MONDAY).toString();
        assertEquals(3, estadisticasActividades.getOrDefault(lunes, 0));
    }

    @Test
    void testFlujoCalendarizacion() {
        Funcionario funcionario = funcionarioService.findById("111");
        CategoriaRecurso categoria = categoriaService.findAll().stream()
                .filter(c -> c.getId().equals("CAT-000002")).findFirst().orElseThrow();
        Recurso recurso = recursoService.findByCategoria(categoria).get(0);

        Reserva reserva = new Reserva(reservaService.generarId(), "Sesion de Planificacion", "2026-06-01",
                "10:00", "11:00", funcionario);
        reserva.setRecursos(List.of(recurso));
        reservaService.create(reserva);

        String[][] matriz = calendarioService.getCalendario("2026-06-01", categoria);

        int filaDiezHoras = -1;
        for (int i = 0; i < matriz.length; i++) {
            if ("10:00".equals(matriz[i][0])) {
                filaDiezHoras = i;
                break;
            }
        }
        assertTrue(filaDiezHoras >= 0);
        assertTrue(matriz[filaDiezHoras][1] != null && matriz[filaDiezHoras][1].contains("Sesion de Planificacion"));
    }
}
