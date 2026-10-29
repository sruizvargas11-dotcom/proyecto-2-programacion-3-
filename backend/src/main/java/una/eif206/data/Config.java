package una.eif206.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.UnaryOperator;

/**
 * Resuelve claves de configuracion (DB_URL, DB_USER, DB_PASSWORD) primero
 * desde variables de entorno y, si faltan, desde backend/config.properties
 * (archivo local, NO se sube al repositorio; ver config.properties.example).
 * Nunca imprime ni incluye en excepciones el VALOR de una clave, solo su
 * NOMBRE cuando falta.
 */
public final class Config {

    private static final String ARCHIVO = "config.properties";

    /**
     * Punto de extension solo para pruebas: permite simular variables de
     * entorno sin tocar el proceso real (System.getenv() no se puede mutar
     * de forma confiable desde una prueba). Por defecto delega en System::getenv.
     */
    static UnaryOperator<String> entorno = System::getenv;

    private static volatile Properties propiedadesCache;

    private Config() {
    }

    public static String get(String clave) {
        String valor = entorno.apply(clave);
        if (valor != null && !valor.isEmpty()) {
            return valor;
        }
        valor = propiedades().getProperty(clave);
        if (valor == null || valor.isEmpty()) {
            throw new IllegalStateException(
                    "Falta configurar " + clave + " (variable de entorno o " + ARCHIVO + ")");
        }
        return valor;
    }

    private static Properties propiedades() {
        Properties cache = propiedadesCache;
        if (cache != null) {
            return cache;
        }
        synchronized (Config.class) {
            if (propiedadesCache == null) {
                propiedadesCache = cargarArchivo();
            }
            return propiedadesCache;
        }
    }

    private static Properties cargarArchivo() {
        Properties propiedades = new Properties();
        Path archivo = Path.of(ARCHIVO);
        if (Files.isRegularFile(archivo)) {
            try (InputStream is = Files.newInputStream(archivo)) {
                propiedades.load(is);
            } catch (IOException ignored) {
                // Sin archivo legible: se mantiene vacio: Config.get(...) sera quien
                // reporte con un mensaje claro cual clave especifica falta.
            }
        }
        return propiedades;
    }

    /** Solo para pruebas: fuerza a volver a leer el archivo en la siguiente llamada a get(...). */
    static void invalidarCache() {
        propiedadesCache = null;
    }
}
