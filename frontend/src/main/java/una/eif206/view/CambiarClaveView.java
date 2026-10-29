package una.eif206.view;

import una.eif206.ApplicationLogin;
import una.eif206.controller.CambiarClaveController;

import javax.swing.*;
import java.awt.*;

public class CambiarClaveView extends JDialog {

    private JPasswordField claveActualFld;
    private JPasswordField claveNuevaFld;
    private JPasswordField claveNueva2Fld;
    private JButton okFld;
    private JButton cancelarFld;
    CambiarClaveController controller;

    public CambiarClaveView() {
        setTitle("Cambiar Clave");
        setSize(320, 200);
        setModal(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        claveActualFld = new JPasswordField(15);
        claveNuevaFld  = new JPasswordField(15);
        claveNueva2Fld = new JPasswordField(15);
        okFld          = new JButton("OK");
        cancelarFld    = new JButton("Cancelar");

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.add(new JLabel("Clave Actual:"));  panel.add(claveActualFld);
        panel.add(new JLabel("Clave Nueva:"));   panel.add(claveNuevaFld);
        panel.add(new JLabel("Repetir Clave:")); panel.add(claveNueva2Fld);
        panel.add(okFld);                        panel.add(cancelarFld);
        add(panel);

        okFld.addActionListener(e -> {
            String actual = new String(claveActualFld.getPassword());
            String nueva  = new String(claveNuevaFld.getPassword());
            String nueva2 = new String(claveNueva2Fld.getPassword());
            if (!nueva.equals(nueva2)) {
                claveNuevaFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
                claveNueva2Fld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
                mostrarError("Las claves nuevas no coinciden");
                return;
            }
            claveNuevaFld.setBackground(null);
            claveNueva2Fld.setBackground(null);
            if (controller.cambiarClave(actual, nueva)) {
                dispose();
            } else {
                claveActualFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
            }
        });

        cancelarFld.addActionListener(e -> dispose());
    }

    public void setController(CambiarClaveController c) { this.controller = c; }

    public void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Informacion", JOptionPane.INFORMATION_MESSAGE);
    }
}
