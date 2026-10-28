package una.eif206.controller;

import una.eif206.model.Usuario;
import una.eif206.service.UsuarioService;
import una.eif206.util.Sesion;
import una.eif206.view.CambiarClaveView;
import una.eif206.view.LoginView;

public class LoginController {

    private final LoginView vista;
    private final UsuarioService usuarioService;
    private final Runnable onLoginExitoso;

    public LoginController(LoginView vista, UsuarioService usuarioService, Runnable onLoginExitoso) {
        this.vista = vista;
        this.usuarioService = usuarioService;
        this.onLoginExitoso = onLoginExitoso;
        vista.setController(this);
    }

    public void login(String id, String clave) {
        try {
            Usuario u = usuarioService.login(id, clave);
            if (u == null) {
                vista.mostrarError("Usuario o clave incorrectos");
                return;
            }
            Sesion.setUsuario(u);
            vista.dispose();
            onLoginExitoso.run();
        } catch (Exception ex) {
            vista.mostrarError("Error: " + ex.getMessage());
        }
    }

    public void abrirCambiarClave(String id, String clave) {
        try {
            Usuario u = usuarioService.login(id, clave);
            if (u == null) {
                vista.mostrarError("Ingrese usuario y clave correctos primero");
                return;
            }
            CambiarClaveView ccv = new CambiarClaveView();
            new CambiarClaveController(ccv, usuarioService, u);
            ccv.setVisible(true);
        } catch (Exception ex) {
            vista.mostrarError("Error: " + ex.getMessage());
        }
    }
}
