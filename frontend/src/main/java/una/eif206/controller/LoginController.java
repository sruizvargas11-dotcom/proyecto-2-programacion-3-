package una.eif206.controller;

import una.eif206.model.Usuario;
import una.eif206.protocol.IService;
import una.eif206.util.Sesion;
import una.eif206.view.CambiarClaveView;
import una.eif206.view.LoginView;

public class LoginController {

    private final LoginView vista;
    private final IService service;
    private final Runnable onLoginExitoso;

    public LoginController(LoginView vista, IService service, Runnable onLoginExitoso) {
        this.vista = vista;
        this.service = service;
        this.onLoginExitoso = onLoginExitoso;
        vista.setController(this);
    }

    public void login(String id, String clave) {
        try {
            Usuario u = service.login(id, clave);
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
            Usuario u = service.login(id, clave);
            if (u == null) {
                vista.mostrarError("Ingrese usuario y clave correctos primero");
                return;
            }
            CambiarClaveView ccv = new CambiarClaveView();
            new CambiarClaveController(ccv, service, u);
            ccv.setVisible(true);
        } catch (Exception ex) {
            vista.mostrarError("Error: " + ex.getMessage());
        }
    }
}
