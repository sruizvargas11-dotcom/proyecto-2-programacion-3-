package una.eif206.service;

import una.eif206.data.Data;
import una.eif206.model.CategoriaRecurso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CategoriaServiceTest {

    private CategoriaService categoriaService;

    @BeforeEach
    void setUp() {
        categoriaService = new CategoriaService(new Data());
    }

    @Test
    void crearCategoriaValida() {
        int before = categoriaService.findAll().size();
        CategoriaRecurso categoria = new CategoriaRecurso("", "Sala de Reuniones");
        String error = categoriaService.create(categoria);
        assertNull(error);
        assertEquals(before + 1, categoriaService.findAll().size());
    }

    @Test
    void crearCategoriaDuplicada() {
        CategoriaRecurso c1 = new CategoriaRecurso("", "Sala VIP");
        CategoriaRecurso c2 = new CategoriaRecurso("", "Sala VIP");
        categoriaService.create(c1);
        categoriaService.create(c2);

        assertNotEquals(c1.getId(), c2.getId());
        assertTrue(categoriaService.findAll().stream()
                .filter(c -> c.getDescripcion().equals("Sala VIP")).count() >= 2);
    }

    @Test
    void crearCategoriaIdAutogenerado() {
        CategoriaRecurso categoria = new CategoriaRecurso("id-que-no-deberia-quedar", "Categoria X");
        categoriaService.create(categoria);
        assertTrue(categoria.getId().matches("CAT-\\d{6}"));
        assertNotEquals("id-que-no-deberia-quedar", categoria.getId());
    }

    @Test
    void modificarCategoriaExistente() {
        CategoriaRecurso categoria = new CategoriaRecurso("", "Bodega");
        categoriaService.create(categoria);

        String error = categoriaService.update(new CategoriaRecurso(categoria.getId(), "Bodega Actualizada"));

        assertNull(error);
        assertEquals("Bodega Actualizada", categoriaService.findById(categoria.getId()).getDescripcion());
    }

    @Test
    void eliminarCategoriaExitosa() {
        CategoriaRecurso categoria = new CategoriaRecurso("", "Categoria Temporal");
        categoriaService.create(categoria);

        String error = categoriaService.delete(categoria);

        assertNull(error);
        assertNull(categoriaService.findById(categoria.getId()));
    }

    @Test
    void eliminarCategoriaConRecursosAsociados() {
        CategoriaRecurso categoria = categoriaService.findAll().get(0);

        String error = categoriaService.delete(categoria);

        assertNotNull(error);
        assertNotNull(categoriaService.findById(categoria.getId()));
    }

    @Test
    void buscarCategoriaPorId() {
        CategoriaRecurso encontrada = categoriaService.findById("CAT-000002");
        assertNotNull(encontrada);
        assertEquals("Sala de Juntas", encontrada.getDescripcion());
        assertNull(categoriaService.findById("CAT-999999"));
    }

    @Test
    void obtenerTodasCategorias() {
        List<CategoriaRecurso> lista = categoriaService.findAll();
        assertEquals(3, lista.size());
    }
}
