package una.eif206.service;

import una.eif206.data.GlobalException;
import una.eif206.data.NoDataException;
import una.eif206.data.Repositorio;
import una.eif206.model.Funcionario;
import una.eif206.model.enums.UsuarioRol;

import java.util.List;

public class FuncionarioService {

    private final Repositorio repositorio;

    public FuncionarioService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    public List<Funcionario> findAll() {
        return repositorio.listarFuncionarios();
    }

    public List<Funcionario> search(Funcionario e) {
        return repositorio.buscarFuncionariosPorTexto(e.getNombre());
    }

    /**
     * Busca por ID exacto; si no hay coincidencia, busca por id o nombre (contiene).
     */
    public List<Funcionario> search(String texto) {
        Funcionario porId = findById(texto);
        if (porId != null) {
            return List.of(porId);
        }
        return repositorio.buscarFuncionariosPorTexto(texto);
    }

    public String create(Funcionario e) {
        if (e.getId() == null || e.getId().trim().isEmpty()
                || e.getNombre() == null || e.getNombre().trim().isEmpty()) {
            return "El ID y el nombre son obligatorios";
        }
        if (findById(e.getId()) != null) {
            return "Funcionario ya existe";
        }
        if (e.getClave() == null || e.getClave().isEmpty()) {
            e.setClave(e.getId());
        }
        if (e.getRol() == null) {
            e.setRol(UsuarioRol.FUNCIONARIO);
        }
        try {
            repositorio.insertarFuncionario(e);
        } catch (GlobalException | NoDataException ex) {
            return ex.getMessage();
        }
        return null;
    }

    public String update(Funcionario f) {
        if (findById(f.getId()) == null) {
            return "Funcionario no encontrado";
        }
        try {
            repositorio.modificarFuncionario(f);
        } catch (GlobalException | NoDataException ex) {
            return ex.getMessage();
        }
        return null;
    }

    public String delete(Funcionario e) {
        if (findById(e.getId()) == null) {
            return "Funcionario no encontrado";
        }
        try {
            repositorio.eliminarFuncionario(e.getId());
        } catch (GlobalException | NoDataException ex) {
            return ex.getMessage();
        }
        return null;
    }

    public Funcionario findById(String id) {
        return repositorio.buscarFuncionarioPorId(id);
    }
}
