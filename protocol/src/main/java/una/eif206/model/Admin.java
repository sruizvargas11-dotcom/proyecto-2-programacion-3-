package una.eif206.model;

import una.eif206.model.enums.UsuarioRol;

public class Admin extends Usuario {

    private static final long serialVersionUID = 1L;

    public Admin(String id) {
        super(id, id, UsuarioRol.ADMIN);
    }

    public Admin(String id, String clave) {
        super(id, id, clave, UsuarioRol.ADMIN);
    }

    public Admin() {
        super();
    }
}