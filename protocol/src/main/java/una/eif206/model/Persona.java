package una.eif206.model;

import java.io.Serializable;

abstract class Persona implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String id;
    protected String nombre;

    public Persona(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public void setId(String id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}