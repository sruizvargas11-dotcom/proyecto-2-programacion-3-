package una.eif206.model;

import java.io.Serializable;

public class CategoriaRecurso implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String descripcion;

    public CategoriaRecurso(String id, String descripcion) {
        this.id = id;
        this.descripcion = descripcion;
    }

    public CategoriaRecurso() { this("", ""); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String d) { this.descripcion = d; }

    @Override
    public String toString() { return descripcion; }
}