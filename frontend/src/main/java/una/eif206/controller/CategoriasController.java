package una.eif206.controller;

import una.eif206.model.CategoriaRecurso;
import una.eif206.service.CategoriaService;
import una.eif206.view.CategoriasView;

import java.util.ArrayList;
import java.util.List;

public class CategoriasController {

    private final CategoriasView vista;
    private final CategoriaService categoriaService;
    private List<CategoriaRecurso> listaActual = new ArrayList<>();
    private CategoriaRecurso current;

    public CategoriasController(CategoriasView vista, CategoriaService categoriaService) {
        this.vista = vista;
        this.categoriaService = categoriaService;
        vista.setController(this);
        cargarListado();
    }

    private void cargarListado() {
        listaActual = categoriaService.findAll();
        vista.cargarTabla(listaActual);
    }

    public void create(CategoriaRecurso e) {
        try {
            String error = categoriaService.create(e);
            if (error != null) {
                vista.mostrarError(error);
                return;
            }
            vista.mostrarMensaje("REGISTRO APLICADO");
            vista.limpiarFormulario();
            current = null;
            cargarListado();
        } catch (Exception ex) {
            vista.mostrarError("Error inesperado: " + ex.getMessage());
        }
    }

    public void update(CategoriaRecurso e) {
        try {
            if (current == null) {
                vista.mostrarError("Seleccione una categoria de la tabla.");
                return;
            }
            String error = categoriaService.update(e);
            if (error != null) {
                vista.mostrarError(error);
                return;
            }
            vista.mostrarMensaje("REGISTRO MODIFICADO");
            vista.limpiarFormulario();
            current = null;
            cargarListado();
        } catch (Exception ex) {
            vista.mostrarError("Error inesperado: " + ex.getMessage());
        }
    }

    public void delete() {
        try {
            if (current == null) {
                vista.mostrarError("Seleccione una categoria de la tabla.");
                return;
            }
            String error = categoriaService.delete(current);
            if (error != null) {
                vista.mostrarError(error);
                return;
            }
            vista.limpiarFormulario();
            current = null;
            cargarListado();
        } catch (Exception ex) {
            vista.mostrarError("Error inesperado: " + ex.getMessage());
        }
    }

    public void clear() {
        current = null;
        vista.limpiarFormulario();
        cargarListado();
    }

    public void edit(int row) {
        current = listaActual.get(row);
        vista.setId(current.getId());
        vista.setDescripcion(current.getDescripcion());
    }

    public void search(String descripcion) {
        CategoriaRecurso c = new CategoriaRecurso();
        c.setDescripcion(descripcion);
        listaActual = categoriaService.search(c);
        vista.cargarTabla(listaActual);
    }
}
