package una.eif206.model;

import una.eif206.model.enums.UsuarioRol;

public class Funcionario extends Usuario {

    private static final long serialVersionUID = 1L;

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