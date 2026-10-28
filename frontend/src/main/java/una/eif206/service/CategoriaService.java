package una.eif206.service;

import una.eif206.data.Data;
import una.eif206.model.CategoriaRecurso;

import java.util.List;
import java.util.stream.Collectors;

public class CategoriaService {

    private final Data data;

    public CategoriaService(Data data) {
        this.data = data;
    }

    public List<CategoriaRecurso> findAll() {
        return data.getCategorias();
    }

    public CategoriaRecurso findById(String id) {
        return data.getCategorias().stream()
                .filter(i -> i.getId().equals(id))
                .findFirst().orElse(null);
    }

    public List<CategoriaRecurso> search(CategoriaRecurso e) {
        return data.getCategorias().stream()
                .filter(i -> i.getDescripcion().toLowerCase().contains(e.getDescripcion().toLowerCase()))
                .collect(Collectors.toList());
    }

    public String create(CategoriaRecurso e) {
        e.setId(data.generarIdCategoria());
        data.getCategorias().add(e);
        return null;
    }

    public String update(CategoriaRecurso c) {
        CategoriaRecurso result = data.getCategorias().stream()
                .filter(i -> i.getId().equals(c.getId()))
                .findFirst().orElse(null);
        if (result == null) return "Categoria no encontrada";
        result.setDescripcion(c.getDescripcion());
        return null;
    }

    public String delete(CategoriaRecurso e) {
        boolean tieneRecursosAsociados = data.getRecursos().stream()
                .anyMatch(r -> r.getCategoria() != null && r.getCategoria().getId().equals(e.getId()));
        if (tieneRecursosAsociados) {
            return "No se puede eliminar: existen recursos asociados.";
        }
        if (!data.getCategorias().remove(e)) return "Categoria no encontrada";
        return null;
    }
}
