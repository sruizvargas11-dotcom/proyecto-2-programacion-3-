package una.eif206.controller;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Recurso;
import una.eif206.service.CategoriaService;
import una.eif206.service.RecursoService;
import una.eif206.view.RecursosView;

import java.util.ArrayList;
import java.util.List;

public class RecursosController {

    private final RecursosView vista;
    private final RecursoService recursoService;
    private final CategoriaService categoriaService;
    private List<Recurso> listaActual = new ArrayList<>();
    private Recurso current;

    public RecursosController(RecursosView vista, RecursoService recursoService, CategoriaService categoriaService) {
        this.vista = vista;
        this.recursoService = recursoService;
        this.categoriaService = categoriaService;
        vista.setController(this);
        vista.cargarCategorias(categoriaService.findAll());
        cargarListado();
    }

    private void cargarListado() {
        listaActual = recursoService.findAll();
        vista.cargarTabla(listaActual);
    }

    public void create(Recurso e) {
        try {
            String error = recursoService.create(e);
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

    public void update(Recurso e) {
        try {
            if (current == null) {
                vista.mostrarError("Seleccione un recurso de la tabla.");
                return;
            }
            String error = recursoService.update(e);
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
                vista.mostrarError("Seleccione un recurso de la tabla.");
                return;
            }
            String error = recursoService.delete(current);
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
        vista.setCategoria(current.getCategoria());
    }

    public void filtrarPorCategoria(CategoriaRecurso categoria) {
        if (categoria == null) {
            listaActual = recursoService.findAll();
        } else {
            listaActual = recursoService.findByCategoria(categoria);
        }
        vista.cargarTabla(listaActual);
    }
}
