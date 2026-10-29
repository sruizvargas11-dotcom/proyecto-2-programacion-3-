package una.eif206.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {

    private static final Path ARCHIVO = Path.of("config.properties");

    private boolean archivoExistiaAntes;
    private byte[] contenidoOriginal;

    @BeforeEach
    void guardarEstadoPrevio() throws IOException {
        archivoExistiaAntes = Files.exists(ARCHIVO);
        if (archivoExistiaAntes) {
            contenidoOriginal = Files.readAllBytes(ARCHIVO);
        }
        Config.entorno = System::getenv;
        Config.invalidarCache();
    }

    @AfterEach
    void restaurarEstadoPrevio() throws IOException {
        if (archivoExistiaAntes) {
            Files.write(ARCHIVO, contenidoOriginal);
        } else {
            Files.deleteIfExists(ARCHIVO);
        }
        Config.entorno = System::getenv;
        Config.invalidarCache();
    }

    @Test
    void variableDeEntornoTienePrioridadSobreElArchivo() throws IOException {
        Files.writeString(ARCHIVO, "DB_URL=jdbc:oracle:thin:@valor-del-archivo\n");
        Config.invalidarCache();
        Config.entorno = Map.of("DB_URL", "jdbc:oracle:thin:@valor-del-entorno")::get;

        assertEquals("jdbc:oracle:thin:@valor-del-entorno", Config.get("DB_URL"));
    }

    @Test
    void sinVariableDeEntornoUsaElArchivo() throws IOException {
        Files.writeString(ARCHIVO, "DB_URL=jdbc:oracle:thin:@valor-del-archivo\n");
        Config.invalidarCache();
        Config.entorno = clave -> null;

        assertEquals("jdbc:oracle:thin:@valor-del-archivo", Config.get("DB_URL"));
    }

    @Test
    void valorFaltanteLanzaMensajeClaroConElNombreDeLaClave() throws IOException {
        Files.deleteIfExists(ARCHIVO);
        Config.invalidarCache();
        Config.entorno = clave -> null;

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> Config.get("DB_PASSWORD"));

        assertTrue(ex.getMessage().contains("DB_PASSWORD"), "el mensaje debe nombrar la clave que falta");
    }

    @Test
    void valorFaltanteNuncaExponeNingunValorEnElMensaje() throws IOException {
        Files.writeString(ARCHIVO, "DB_USER=usuario_app\n");
        Config.invalidarCache();
        Config.entorno = clave -> "DB_PASSWORD".equals(clave) ? "" : null;

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> Config.get("DB_PASSWORD"));

        assertFalse(ex.getMessage().toLowerCase().contains("usuario_app"),
                "el mensaje de error no debe filtrar valores de otras claves configuradas");
    }
}
