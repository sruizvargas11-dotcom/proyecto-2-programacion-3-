package una.eif206.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import una.eif206.model.enums.UsuarioRol;

@XmlAccessorType(XmlAccessType.FIELD)
public class Funcionario extends Usuario {

    private String departamento;
    private String telefono;

    public Funcionario(String id, String nombre, String departamento, String telefono) {
        super(id, nombre, id, UsuarioRol.FUNCIONARIO);
        this.departamento = departamento;
        this.telefono = telefono;
    }

    public Funcionario() {
        super();
        this.departamento = "";
        this.telefono = "";
    }

    public String getDepartamento() { return departamento; }
    public void setDepartamento(String d) { this.departamento = d; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String t) { this.telefono = t; }

    @Override
    public String toString() { return nombre; }
}