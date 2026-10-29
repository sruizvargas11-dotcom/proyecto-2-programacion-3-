package una.eif206.service;

import una.eif206.data.GlobalException;
import una.eif206.data.NoDataException;
import una.eif206.data.Repositorio;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Recurso;

import java.util.List;

public class RecursoService {

    private final Repositorio repositorio;

    public RecursoService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    public List<Recurso> findAll() {
        return repositorio.listarRecursos();
    }

    public List<Recurso> findByCategoria(CategoriaRecurso categoria) {
        return repositorio.listarRecursosPorCategoria(categoria.getId());
    }

    public String create(Recurso e) {
        if (e.getCategoria() == null) {
            return "Debe asignar una categoria al recurso";
        }
        if (repositorio.buscarRecursoPorId(e.getId()) != null) {
            return "Recurso ya existe";
        }
        try {
            repositorio.insertarRecurso(e);
        } catch (GlobalException | NoDataException ex) {
            return ex.getMessage();
        }
        return null;
    }

    public String update(Recurso r) {
        if (repositorio.buscarRecursoPorId(r.getId()) == null) {
            return "Recurso no encontrado";
        }
        try {
            repositorio.modificarRecurso(r);
        } catch (GlobalException | NoDataException ex) {
            return ex.getMessage();
        }
        return null;
    }

    public String delete(Recurso e) {
        if (repositorio.buscarRecursoPorId(e.getId()) == null) {
            return "Recurso no encontrado";
        }
        try {
            repositorio.eliminarRecurso(e.getId());
        } catch (GlobalException | NoDataException ex) {
            return ex.getMessage();
        }
        return null;
    }
}
