package una.eif206.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import una.eif206.client.FakeService;
import una.eif206.model.CategoriaRecurso;
import una.eif206.view.CategoriasView;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de unidad del CRUD de categorias contra FakeService. */
class CategoriasControllerTest {

    static class CategoriasViewDePrueba extends CategoriasView {
        String ultimoError;
        String ultimoMensaje;
        List<CategoriaRecurso> tabla;

        @Override public void mostrarError(String m) { ultimoError = m; }
        @Override public void mostrarMensaje(String m) { ultimoMensaje = m; }
        @Override public void cargarTabla(List<CategoriaRecurso> lista) { tabla = lista; super.cargarTabla(lista); }
    }

    private CategoriasViewDePrueba vista;
    private CategoriasController controller;

    @BeforeEach
    void setUp() {
        vista = new CategoriasViewDePrueba();
        controller = new CategoriasController(vista, new FakeService());
    }

    @Test
    void alIniciar_cargaLasCategorias() {
        assertEquals(2, vista.tabla.size());
    }

    @Test
    void crear_muestraMensajeYRecargaLaTabla() {
        controller.create(new CategoriaRecurso("", "Proyector"));
        assertNull(vista.ultimoError);
        assertEquals("REGISTRO APLICADO", vista.ultimoMensaje);
        assertEquals(3, vista.tabla.size());
    }

    @Test
    void modificarSinSeleccionar_daError() {
        controller.update(new CategoriaRecurso("CAT-000001", "Otra"));
        assertEquals("Seleccione una categoria de la tabla.", vista.ultimoError);
    }

    @Test
    void borrarCategoriaConRecursos_daError() {
        controller.edit(0);
        controller.delete();
        assertNotNull(vista.ultimoError);
        assertEquals(2, vista.tabla.size());
    }

    @Test
    void borrarCategoriaSinRecursos_laQuita() {
        controller.create(new CategoriaRecurso("", "Proyector"));
        controller.edit(2);
        controller.delete();
        assertNull(vista.ultimoError);
        assertEquals(2, vista.tabla.size());
    }
}
