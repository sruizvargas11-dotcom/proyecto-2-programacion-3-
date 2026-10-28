package una.eif206.util;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.net.URL;


public class IconLoader {

    private static final int TAMANO = 18;

    public static ImageIcon load(String nombre) {
        URL url = IconLoader.class.getResource("/icons/" + nombre + ".png");
        if (url == null) return null;
        ImageIcon original = new ImageIcon(url);
        Image escalada = original.getImage().getScaledInstance(TAMANO, TAMANO, Image.SCALE_SMOOTH);
        return new ImageIcon(escalada);
    }
}
