package una.eif206.service;

import una.eif206.data.Data;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Recurso;

import java.util.List;
import java.util.stream.Collectors;

public class RecursoService {

    private final Data data;

    public RecursoService(Data data) {
        this.data = data;
    }

    public List<Recurso> findAll() {
        return data.getRecursos();
    }

    public List<Recurso> findByCategoria(CategoriaRecurso categoria) {
        return data.getRecursos().stream()
                .filter(r -> r.getCategoria() != null &&
                        r.getCategoria().getId().equals(categoria.getId()))
                .collect(Collectors.toList());
    }

    public String create(Recurso e) {
        if (e.getCategoria() == null) {
            return "Debe asignar una categoria al recurso";
        }
        Recurso result = data.getRecursos().stream()
                .filter(i -> i.getId().equals(e.getId()))
                .findFirst().orElse(null);
        if (result != null) return "Recurso ya existe";
        data.getRecursos().add(e);
        return null;
    }

    public String update(Recurso r) {
        Recurso result = data.getRecursos().stream()
                .filter(i -> i.getId().equals(r.getId()))
                .findFirst().orElse(null);
        if (result == null) return "Recurso no encontrado";
        result.setDescripcion(r.getDescripcion());
        result.setCategoria(r.getCategoria());
        return null;
    }

    public String delete(Recurso e) {
        if (!data.getRecursos().remove(e)) return "Recurso no encontrado";
        return null;
    }
}
