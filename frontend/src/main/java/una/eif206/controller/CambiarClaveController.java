package una.eif206.controller;

import una.eif206.model.Usuario;
import una.eif206.protocol.IService;
import una.eif206.view.CambiarClaveView;

public class CambiarClaveController {

    private final CambiarClaveView vista;
    private final IService service;
    private final Usuario usuario;

    public CambiarClaveController(CambiarClaveView vista, IService service, Usuario usuario) {
        this.vista = vista;
        this.service = service;
        this.usuario = usuario;
        vista.setController(this);
    }

    public boolean cambiarClave(String actual, String nueva) {
        try {
            if (usuario == null) {
                vista.mostrarError("Debe iniciar sesion primero");
                return false;
            }
            String error = service.cambiarClave(usuario, actual, nueva);
            if (error != null) {
                vista.mostrarError(error);
                return false;
            }
            vista.mostrarMensaje("Clave cambiada exitosamente");
            return true;
        } catch (Exception ex) {
            vista.mostrarError("Error inesperado: " + ex.getMessage());
            return false;
        }
    }
}
