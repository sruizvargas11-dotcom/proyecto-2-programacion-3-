package una.eif206.service;

import una.eif206.data.GlobalException;
import una.eif206.data.NoDataException;
import una.eif206.data.Repositorio;
import una.eif206.model.Usuario;

public class UsuarioService {

    private final Repositorio repositorio;

    public UsuarioService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    public Usuario login(String id, String clave) {
        return repositorio.login(id, clave);
    }

    public String cambiarClave(Usuario u, String claveActual, String claveNueva) {
        if (u == null || u.getClave() == null || !u.getClave().equals(claveActual)) {
            return "Clave actual incorrecta";
        }
        try {
            repositorio.cambiarClave(u.getId(), claveNueva);
        } catch (GlobalException | NoDataException e) {
            return e.getMessage();
        }
        u.setClave(claveNueva);
        return null;
    }
}
