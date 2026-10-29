package una.eif206.view;

import una.eif206.ApplicationLogin;
import una.eif206.controller.LoginController;

import javax.swing.*;
import java.awt.*;

public class LoginView extends JDialog {

    private JTextField idFld;
    private JPasswordField claveFld;
    private JButton ingresarFld;
    private JButton cambiarFld;

    LoginController controller;

    public LoginView() {
        setTitle("Iniciar Sesion");
        setSize(300, 200);
        setModal(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        idFld       = new JTextField(15);
        claveFld    = new JPasswordField(15);
        ingresarFld = new JButton("Ingresar");
        cambiarFld  = new JButton("Cambiar clave");

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.add(new JLabel("Usuario:")); panel.add(idFld);
        panel.add(new JLabel("Clave:"));   panel.add(claveFld);
        panel.add(ingresarFld);            panel.add(cambiarFld);
        add(panel);

        ingresarFld.addActionListener(e ->
                controller.login(idFld.getText(), new String(claveFld.getPassword())));

        cambiarFld.addActionListener(e ->
                controller.abrirCambiarClave(idFld.getText(), new String(claveFld.getPassword())));
    }

    public void setController(LoginController c) { this.controller = c; }

    public void mostrarError(String mensaje) {
        idFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
        claveFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}