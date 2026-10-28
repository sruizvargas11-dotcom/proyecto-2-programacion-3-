package una.eif206.service;

import una.eif206.data.Data;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Recurso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RecursoServiceTest {

    private RecursoService recursoService;
    private CategoriaService categoriaService;

    @BeforeEach
    void setUp() {
        Data data = new Data();
        recursoService = new RecursoService(data);
        categoriaService = new CategoriaService(data);
    }

    @Test
    void crearRecursoValido() {
        int before = recursoService.findAll().size();
        CategoriaRecurso categoria = categoriaService.findAll().get(0);
        String error = recursoService.create(new Recurso("R-900", "Recurso de prueba", categoria));
        assertNull(error);
        assertEquals(before + 1, recursoService.findAll().size());
    }

    @Test
    void crearRecursoDuplicado() {
        CategoriaRecurso categoria = categoriaService.findAll().get(0);
        String error = recursoService.create(new Recurso("238715", "Otro Nombre", categoria));
        assertNotNull(error);
    }

    @Test
    void modificarRecursoExistente() {
        CategoriaRecurso categoria = categoriaService.findAll().get(0);
        String error = recursoService.update(new Recurso("238715", "Laptop Renovada", categoria));
        assertNull(error);
        Recurso actualizado = recursoService.findAll().stream()
                .filter(r -> r.getId().equals("238715")).findFirst().orElseThrow();
        assertEquals("Laptop Renovada", actualizado.getDescripcion());
    }

    @Test
    void eliminarRecursoExistente() {
        Recurso existente = recursoService.findAll().stream()
                .filter(r -> r.getId().equals("238715")).findFirst().orElseThrow();
        String error = recursoService.delete(existente);
        assertNull(error);
        assertTrue(recursoService.findAll().stream().noneMatch(r -> r.getId().equals("238715")));
    }

    @Test
    void filtrarRecursosPorCategoria() {
        CategoriaRecurso categoriaLaptops = categoriaService.findAll().stream()
                .filter(c -> c.getId().equals("CAT-000001")).findFirst().orElseThrow();

        List<Recurso> recursos = recursoService.findByCategoria(categoriaLaptops);

        assertEquals(2, recursos.size());
        assertTrue(recursos.stream().allMatch(r -> r.getCategoria().getId().equals("CAT-000001")));
    }

    @Test
    void obtenerTodosRecursos() {
        assertEquals(3, recursoService.findAll().size());
    }

    @Test
    void crearRecursoSinCategoria() {
        String error = recursoService.create(new Recurso("R-901", "Recurso sin categoria", null));
        assertNotNull(error);
    }
}
