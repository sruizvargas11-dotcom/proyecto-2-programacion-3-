package una.eif206.service;

import una.eif206.data.Data;
import una.eif206.model.Admin;
import una.eif206.model.Funcionario;
import una.eif206.model.Usuario;

public class UsuarioService {

    private final Data data;

    public UsuarioService(Data data) {
        this.data = data;
    }

    public Usuario login(String id, String clave) {
        for (Admin a : data.getAdmins()) {
            if (a.getId().equals(id) && a.getClave().equals(clave)) return a;
        }
        for (Funcionario f : data.getFuncionarios()) {
            if (f.getId().equals(id) && f.getClave().equals(clave)) return f;
        }
        return null;
    }

    public String cambiarClave(Usuario u, String claveActual, String claveNueva) {
        if (!u.getClave().equals(claveActual)) {
            return "Clave actual incorrecta";
        }
        u.setClave(claveNueva);
        return null;
    }
}
