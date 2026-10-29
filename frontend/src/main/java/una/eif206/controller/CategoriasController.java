package una.eif206.controller;

import una.eif206.model.CategoriaRecurso;
import una.eif206.protocol.IService;
import una.eif206.view.CategoriasView;

import java.util.ArrayList;
import java.util.List;

public class CategoriasController {

    private final CategoriasView vista;
    private final IService service;
    private List<CategoriaRecurso> listaActual = new ArrayList<>();
    private CategoriaRecurso current;

    public CategoriasController(CategoriasView vista, IService service) {
        this.vista = vista;
        this.service = service;
        vista.setController(this);
        cargarListado();
    }

    private void cargarListado() {
        listaActual = service.findAllCategorias();
        vista.cargarTabla(listaActual);
    }

    public void create(CategoriaRecurso e) {
        try {
            String error = service.create(e);
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
            String error = service.update(e);
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
            String error = service.delete(current);
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
        listaActual = service.search(c);
        vista.cargarTabla(listaActual);
    }
}
