package una.eif206.util;

import una.eif206.protocol.Protocol;

import java.io.InputStream;
import java.util.Properties;

public class ConfigCliente {

    private static final String ARCHIVO = "/servidor.properties";
    private static final Properties propiedades = cargar();

    private static Properties cargar() {
        Properties p = new Properties();
        try (InputStream is = ConfigCliente.class.getResourceAsStream(ARCHIVO)) {
            if (is != null) p.load(is);
        } catch (Exception ignored) {
        }
        return p;
    }

    public static String getHost() {
        return propiedades.getProperty("host", Protocol.HOST).trim();
    }

    public static int getPuerto() {
        try {
            return Integer.parseInt(propiedades.getProperty("puerto", "" + Protocol.PORT).trim());
        } catch (NumberFormatException e) {
            return Protocol.PORT;
        }
    }

    public static boolean usarServidor() {
        return !"prueba".equalsIgnoreCase(propiedades.getProperty("modo", "servidor").trim());
    }
}