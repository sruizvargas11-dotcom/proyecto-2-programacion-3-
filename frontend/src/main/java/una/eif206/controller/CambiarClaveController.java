package una.eif206.controller;

import una.eif206.model.Usuario;
import una.eif206.service.UsuarioService;
import una.eif206.view.CambiarClaveView;

public class CambiarClaveController {

    private final CambiarClaveView vista;
    private final UsuarioService usuarioService;
    private final Usuario usuario;

    public CambiarClaveController(CambiarClaveView vista, UsuarioService usuarioService, Usuario usuario) {
        this.vista = vista;
        this.usuarioService = usuarioService;
        this.usuario = usuario;
        vista.setController(this);
    }

    public boolean cambiarClave(String actual, String nueva) {
        try {
            if (usuario == null) {
                vista.mostrarError("Debe iniciar sesion primero");
                return false;
            }
            String error = usuarioService.cambiarClave(usuario, actual, nueva);
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
