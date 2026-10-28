package una.eif206.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import una.eif206.model.enums.UsuarioRol;

@XmlAccessorType(XmlAccessType.FIELD)
public class Admin extends Usuario {

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