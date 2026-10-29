package una.eif206.service;

import una.eif206.data.GlobalException;
import una.eif206.data.NoDataException;
import una.eif206.data.Repositorio;
import una.eif206.model.CategoriaRecurso;

import java.util.List;

public class CategoriaService {

    private final Repositorio repositorio;

    public CategoriaService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    public List<CategoriaRecurso> findAll() {
        return repositorio.listarCategorias();
    }

    public CategoriaRecurso findById(String id) {
        return repositorio.buscarCategoriaPorId(id);
    }

    public List<CategoriaRecurso> search(CategoriaRecurso e) {
        return repositorio.buscarCategoriasPorTexto(e.getDescripcion());
    }

    public String create(CategoriaRecurso e) {
        try {
            String id = repositorio.insertarCategoria(e.getDescripcion());
            e.setId(id);
        } catch (GlobalException | NoDataException ex) {
            return ex.getMessage();
        }
        return null;
    }

    public String update(CategoriaRecurso c) {
        if (findById(c.getId()) == null) {
            return "Categoria no encontrada";
        }
        try {
            repositorio.modificarCategoria(c);
        } catch (GlobalException | NoDataException ex) {
            return ex.getMessage();
        }
        return null;
    }

    public String delete(CategoriaRecurso e) {
        if (findById(e.getId()) == null) {
            return "Categoria no encontrada";
        }
        if (!repositorio.listarRecursosPorCategoria(e.getId()).isEmpty()) {
            return "No se puede eliminar: existen recursos asociados.";
        }
        try {
            repositorio.eliminarCategoria(e.getId());
        } catch (GlobalException | NoDataException ex) {
            return ex.getMessage();
        }
        return null;
    }
}
