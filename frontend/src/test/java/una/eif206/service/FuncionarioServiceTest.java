package una.eif206.service;

import una.eif206.data.Data;
import una.eif206.model.Funcionario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FuncionarioServiceTest {

    private FuncionarioService funcionarioService;

    @BeforeEach
    void setUp() {
        funcionarioService = new FuncionarioService(new Data());
    }

    @Test
    void crearFuncionarioValido() {
        int before = funcionarioService.findAll().size();
        String error = funcionarioService.create(new Funcionario("333", "Nuevo Empleado", "TI", "1234"));
        assertNull(error);
        assertEquals(before + 1, funcionarioService.findAll().size());
    }

    @Test
    void crearFuncionarioClaveIgualAlId() {
        Funcionario nuevo = new Funcionario("F100", "Nuevo Funcionario", "TI", "1234");
        nuevo.setClave("");
        String error = funcionarioService.create(nuevo);
        assertNull(error);
        assertEquals("F100", nuevo.getClave());
    }

    @Test
    void crearFuncionarioDuplicado() {
        String error = funcionarioService.create(new Funcionario("111", "Otro Nombre", "TI", "0000"));
        assertNotNull(error);
    }

    @Test
    void crearFuncionarioCamposVacios() {
        String error = funcionarioService.create(new Funcionario("", "", "TI", "0000"));
        assertNotNull(error);
    }

    @Test
    void obtenerTodosFuncionarios() {
        List<Funcionario> lista = funcionarioService.findAll();
        assertEquals(2, lista.size());
        assertTrue(lista.stream().anyMatch(f -> f.getId().equals("111")));
        assertTrue(lista.stream().anyMatch(f -> f.getId().equals("222")));
    }

    @Test
    void modificarFuncionarioExistente() {
        String error = funcionarioService.update(new Funcionario("111", "Nombre Actualizado", "Ventas", "9999"));
        assertNull(error);
        Funcionario actualizado = funcionarioService.findById("111");
        assertEquals("Nombre Actualizado", actualizado.getNombre());
        assertEquals("Ventas", actualizado.getDepartamento());
        assertEquals("9999", actualizado.getTelefono());
    }

    @Test
    void modificarFuncionarioInexistente() {
        String error = funcionarioService.update(new Funcionario("999", "No Existe", "TI", "0000"));
        assertNotNull(error);
    }

    @Test
    void eliminarFuncionarioExistente() {
        Funcionario existente = funcionarioService.findById("111");
        String error = funcionarioService.delete(existente);
        assertNull(error);
        assertNull(funcionarioService.findById("111"));
    }

    @Test
    void eliminarFuncionarioInexistente() {
        Funcionario inexistente = new Funcionario("999", "No Existe", "TI", "0000");
        String error = funcionarioService.delete(inexistente);
        assertNotNull(error);
    }

    @Test
    void buscarPorIdExacto() {
        funcionarioService.create(new Funcionario("F999", "Empleado Especial", "TI", "0000"));

        List<Funcionario> resultado = funcionarioService.search("F999");

        assertTrue(resultado.stream().anyMatch(f -> f.getId().equals("F999")));
    }
}
