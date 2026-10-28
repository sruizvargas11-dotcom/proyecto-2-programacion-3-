package una.eif206.controller;

import una.eif206.model.Funcionario;
import una.eif206.service.FuncionarioService;
import una.eif206.view.FuncionariosView;

import java.util.ArrayList;
import java.util.List;

public class FuncionariosController {

    private final FuncionariosView vista;
    private final FuncionarioService funcionarioService;
    private List<Funcionario> listaActual = new ArrayList<>();
    private Funcionario current;

    public FuncionariosController(FuncionariosView vista, FuncionarioService funcionarioService) {
        this.vista = vista;
        this.funcionarioService = funcionarioService;
        vista.setController(this);
        cargarListado();
    }

    private void cargarListado() {
        listaActual = funcionarioService.findAll();
        vista.cargarTabla(listaActual);
    }

    public void create(Funcionario e) {
        try {
            String error = funcionarioService.create(e);
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
            String error = funcionarioService.update(f);
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
            String error = funcionarioService.delete(current);
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
        listaActual = funcionarioService.search(texto);
        vista.cargarTabla(listaActual);
    }
}
