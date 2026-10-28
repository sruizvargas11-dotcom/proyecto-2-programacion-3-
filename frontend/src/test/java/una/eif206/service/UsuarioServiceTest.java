package una.eif206.service;

import una.eif206.data.Data;
import una.eif206.model.Admin;
import una.eif206.model.Funcionario;
import una.eif206.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UsuarioServiceTest {

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(new Data());
    }

    @Test
    void loginAdminCorrecto() {
        Usuario usuario = usuarioService.login("admin", "admin123");
        assertNotNull(usuario);
        assertTrue(usuario instanceof Admin);
    }

    @Test
    void loginFuncionarioCorrecto() {
        Usuario usuario = usuarioService.login("111", "111");
        assertNotNull(usuario);
        assertTrue(usuario instanceof Funcionario);
    }

    @Test
    void loginConClaveIncorrecta() {
        assertNull(usuarioService.login("admin", "claveMala"));
    }

    @Test
    void loginConIdInexistente() {
        assertNull(usuarioService.login("noexiste", "admin123"));
    }

    @Test
    void loginConCamposVacios() {
        assertNull(usuarioService.login("", ""));
    }

    @Test
    void cambiarClaveCorrecta() {
        Usuario usuario = usuarioService.login("admin", "admin123");
        String error = usuarioService.cambiarClave(usuario, "admin123", "claveNueva1");
        assertNull(error);
        assertEquals("claveNueva1", usuario.getClave());
    }

    @Test
    void cambiarClaveIncorrecta_claveActualMal() {
        Usuario usuario = usuarioService.login("admin", "admin123");
        String error = usuarioService.cambiarClave(usuario, "claveEquivocada", "claveNueva1");
        assertNotNull(error);
        assertEquals("admin123", usuario.getClave());
    }

    @Test
    void cambiarClaveIncorrecta_camposVacios() {
        Usuario usuario = usuarioService.login("admin", "admin123");
        String error = usuarioService.cambiarClave(usuario, "", "");
        assertNotNull(error);
        assertEquals("admin123", usuario.getClave());
    }

    @Test
    void loginConNuevaClaveDepuesDeCambio() {
        Usuario usuario = usuarioService.login("admin", "admin123");
        usuarioService.cambiarClave(usuario, "admin123", "claveNueva1");

        Usuario relogueado = usuarioService.login("admin", "claveNueva1");
        assertNotNull(relogueado);
    }

    @Test
    void loginConClaveViejaFallaDepuesDeCambio() {
        Usuario usuario = usuarioService.login("admin", "admin123");
        usuarioService.cambiarClave(usuario, "admin123", "claveNueva1");

        assertNull(usuarioService.login("admin", "admin123"));
    }
}
