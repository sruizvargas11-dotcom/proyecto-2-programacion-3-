package una.eif206.controller;

import una.eif206.model.Funcionario;
import una.eif206.protocol.IService;
import una.eif206.view.FuncionariosView;

import java.util.ArrayList;
import java.util.List;

public class FuncionariosController {

    private final FuncionariosView vista;
    private final IService service;
    private List<Funcionario> listaActual = new ArrayList<>();
    private Funcionario current;

    public FuncionariosController(FuncionariosView vista, IService service) {
        this.vista = vista;
        this.service = service;
        vista.setController(this);
        cargarListado();
    }

    private void cargarListado() {
        listaActual = service.findAllFuncionarios();
        vista.cargarTabla(listaActual);
    }

    public void create(Funcionario e) {
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

    public void update(Funcionario f) {
        try {
            if (current == null) {
                vista.mostrarError("Seleccione un funcionario de la tabla.");
                return;
            }
            String error = service.update(f);
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
                vista.mostrarError("Seleccione un funcionario de la tabla.");
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
        vista.setNombre(current.getNombre());
        vista.setTelefono(current.getTelefono());
    }

    public void search(String texto) {
        listaActual = service.search(texto);
        vista.cargarTabla(listaActual);
    }
}
