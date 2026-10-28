package una.eif206.data;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.service.CategoriaService;
import una.eif206.service.FuncionarioService;
import una.eif206.service.RecursoService;
import una.eif206.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PersistenciaXMLTest {

    private XMLHelper xmlHelper;
    private Data data;

    @BeforeEach
    void setUp() throws IOException {
        File tmp = File.createTempFile("test-data", ".xml");
        tmp.deleteOnExit();
        xmlHelper = new XMLHelper(tmp.getAbsolutePath());
        data = new Data();
    }

    @Test
    void guardarYRecargarFuncionario() throws Exception {
        new FuncionarioService(data).create(new Funcionario("777", "Persistido", "TI", "1111"));

        xmlHelper.store(data);
        Data recargada = xmlHelper.load();

        Funcionario encontrado = new FuncionarioService(recargada).findById("777");
        assertNotNull(encontrado);
        assertEquals("Persistido", encontrado.getNombre());
        assertEquals("TI", encontrado.getDepartamento());
    }

    @Test
    void guardarYRecargarCategoria() throws Exception {
        CategoriaRecurso categoria = new CategoriaRecurso("", "Categoria Persistida");
        new CategoriaService(data).create(categoria);

        xmlHelper.store(data);
        Data recargada = xmlHelper.load();

        CategoriaRecurso encontrada = new CategoriaService(recargada).findById(categoria.getId());
        assertNotNull(encontrada);
        assertEquals("Categoria Persistida", encontrada.getDescripcion());
    }

    @Test
    void guardarYRecargarRecurso() throws Exception {
        CategoriaRecurso categoria = data.getCategorias().get(0);
        new RecursoService(data).create(new Recurso("R-800", "Recurso Persistido", categoria));

        xmlHelper.store(data);
        Data recargada = xmlHelper.load();

        Recurso encontrado = new RecursoService(recargada).findAll().stream()
                .filter(r -> r.getId().equals("R-800")).findFirst().orElse(null);
        assertNotNull(encontrado);
        assertEquals("Recurso Persistido", encontrado.getDescripcion());
        assertNotNull(encontrado.getCategoria());
        assertEquals(categoria.getId(), encontrado.getCategoria().getId());
    }

    @Test
    void guardarYRecargarReserva() throws Exception {
        Funcionario funcionario = data.getFuncionarios().get(0);
        CategoriaRecurso categoria = data.getCategorias().get(1);
        Recurso recurso = new RecursoService(data).findByCategoria(categoria).get(0);

        ReservaService reservaService = new ReservaService(data);
        Reserva reserva = new Reserva(reservaService.generarId(), "Reunion Persistida",
                "2026-07-01", "09:00", "10:00", funcionario);
        reserva.setRecursos(List.of(recurso));
        reservaService.create(reserva);

        xmlHelper.store(data);
        Data recargada = xmlHelper.load();

        Reserva encontrada = new ReservaService(recargada).findAll().stream()
                .filter(r -> r.getId().equals(reserva.getId())).findFirst().orElse(null);
        assertNotNull(encontrada);
        assertEquals("Reunion Persistida", encontrada.getActividad());
        assertNotNull(encontrada.getFuncionario());
        assertEquals(funcionario.getId(), encontrada.getFuncionario().getId());
        assertEquals(1, encontrada.getRecursos().size());
        assertEquals(recurso.getId(), encontrada.getRecursos().get(0).getId());
    }

    @Test
    void dataVaciaGuardaYRecargaSinErrores() throws Exception {
        xmlHelper.store(data);
        Data recargada = xmlHelper.load();

        assertEquals(1, recargada.getAdmins().size());
        assertEquals(2, recargada.getFuncionarios().size());
        assertEquals(3, recargada.getCategorias().size());
        assertEquals(3, recargada.getRecursos().size());
        assertEquals(0, recargada.getReservas().size());
    }

    @Test
    void modificacionPersisteDespuesDeRecargar() throws Exception {
        new FuncionarioService(data).update(new Funcionario("111", "Nombre Modificado", "Ventas", "5555"));

        xmlHelper.store(data);
        Data recargada = xmlHelper.load();

        Funcionario encontrado = new FuncionarioService(recargada).findById("111");
        assertEquals("Nombre Modificado", encontrado.getNombre());
        assertEquals("Ventas", encontrado.getDepartamento());
    }

    @Test
    void eliminacionPersisteDespuesDeRecargar() throws Exception {
        FuncionarioService funcionarioService = new FuncionarioService(data);
        funcionarioService.delete(funcionarioService.findById("222"));

        xmlHelper.store(data);
        Data recargada = xmlHelper.load();

        assertNull(new FuncionarioService(recargada).findById("222"));
        assertEquals(1, recargada.getFuncionarios().size());
    }
}
