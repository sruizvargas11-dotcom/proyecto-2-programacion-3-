package una.eif206.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class Highlighter extends MouseAdapter implements MouseListener {

    private Color color;
    private Color original;

    public Highlighter(Color color) {
        this.color = color;
    }

    public void mouseEntered(MouseEvent evt) {
        Component source = (Component) evt.getSource();
        original = source.getBackground();
        source.setBackground(color);
    }

    public void mouseExited(MouseEvent evt) {
        Component source = (Component) evt.getSource();
        source.setBackground(original);
    }
}