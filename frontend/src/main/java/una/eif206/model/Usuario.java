package una.eif206.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import una.eif206.model.enums.UsuarioRol;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({Admin.class, Funcionario.class})
public class Usuario extends Persona {

    private String clave;
    private UsuarioRol rol;

    public Usuario(String id, String nombre, String clave, UsuarioRol rol) {
        super(id, nombre);
        this.clave = clave;
        this.rol = rol;
    }

    public Usuario(String id, String nombre, UsuarioRol rol) {
        this(id, nombre, id, rol);
    }

    public Usuario() {
        super("", "");
        this.clave = "";
        this.rol = null;
    }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public UsuarioRol getRol() { return rol; }
    public void setRol(UsuarioRol rol) { this.rol = rol; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario)) return false;
        Usuario u = (Usuario) o;
        return getId() != null && getId().equals(u.getId());
    }

    @Override
    public String toString() { return nombre; }
}