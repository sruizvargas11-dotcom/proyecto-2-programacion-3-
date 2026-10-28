package una.eif206.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlIDREF;

@XmlAccessorType(XmlAccessType.FIELD)
public class Recurso {

    @XmlID
    private String id;
    private String descripcion;
    @XmlIDREF
    private CategoriaRecurso categoria;
    private boolean disponible;

    public Recurso(String id, String descripcion, CategoriaRecurso categoria) {
        this.id = id;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.disponible = true;
    }

    public Recurso() {

        this.id = "";
        this.descripcion = "";
        this.categoria = null;
        this.disponible = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String d) { this.descripcion = d; }
    public CategoriaRecurso getCategoria() { return categoria; }
    public void setCategoria(CategoriaRecurso c) { this.categoria = c; }
    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean d) { this.disponible = d; }

    @Override
    public String toString() { return descripcion; }
}