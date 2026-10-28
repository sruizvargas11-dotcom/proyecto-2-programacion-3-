package una.eif206.view;

import una.eif206.ApplicationLogin;
import una.eif206.controller.FuncionariosController;
import una.eif206.model.Funcionario;
import una.eif206.util.Highlighter;
import una.eif206.util.IconLoader;
import una.eif206.util.PdfReporter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class FuncionariosView {

    private JPanel panel;
    private JTextField idFld;
    private JTextField nombreFld;
    private JTextField telefonoFld;
    private JTextField busquedaFld;
    private JButton guardarFld;
    private JButton modificarFld;
    private JButton cancelarFld;
    private JButton buscarFld;
    private JButton borrarFld;
    private JButton imprimirFld;
    private JTable tabla;

    private final DefaultTableModel modeloTabla = new DefaultTableModel(new Object[]{"ID", "Nombre", "Teléfono"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) { return false; }
    };
    private List<Funcionario> filas = new ArrayList<>();

    FuncionariosController controller;

    public FuncionariosView() {
        panel        = new JPanel(new BorderLayout(5, 5));
        idFld        = new JTextField(10);
        nombreFld    = new JTextField(20);
        telefonoFld  = new JTextField(15);
        busquedaFld  = new JTextField(15);
        guardarFld   = new JButton("Guardar");
        modificarFld = new JButton("Modificar");
        cancelarFld  = new JButton("Limpiar");
        buscarFld    = new JButton("Buscar");
        borrarFld    = new JButton("Borrar");
        imprimirFld  = new JButton("Imprimir PDF");
        tabla        = new JTable();
        tabla.setModel(modeloTabla);

        JPanel form = new JPanel(new GridLayout(5, 2, 5, 5));
        form.add(new JLabel("ID:"));       form.add(idFld);
        form.add(new JLabel("Nombre:"));   form.add(nombreFld);
        form.add(new JLabel("Telefono:")); form.add(telefonoFld);
        form.add(new JLabel("Buscar:"));   form.add(busquedaFld);

        guardarFld.setIcon(IconLoader.load("guardar"));
        modificarFld.setIcon(IconLoader.load("modificar"));
        cancelarFld.setIcon(IconLoader.load("limpiar"));
        buscarFld.setIcon(IconLoader.load("buscar"));
        borrarFld.setIcon(IconLoader.load("borrar"));
        imprimirFld.setIcon(IconLoader.load("imprimir"));

        JPanel botones = new JPanel(new GridLayout(2, 3, 5, 5));
        botones.add(guardarFld);
        botones.add(modificarFld);
        botones.add(cancelarFld);
        botones.add(buscarFld);
        botones.add(borrarFld);
        botones.add(imprimirFld);
        form.add(new JLabel(""));
        form.add(botones);

        panel.add(form, BorderLayout.NORTH);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        guardarFld.addActionListener(e -> {
            if (validate()) {
                controller.create(take());
            }
        });

        modificarFld.addActionListener(e -> {
            if (validate()) {
                controller.update(take());
            }
        });

        cancelarFld.addActionListener(e -> controller.clear());

        buscarFld.addActionListener(e -> controller.search(busquedaFld.getText()));

        borrarFld.addActionListener(e -> {
            int op = JOptionPane.showConfirmDialog(panel, "Confirma borrar?");
            if (op == JOptionPane.YES_OPTION) {
                controller.delete();
            }
        });

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tabla.getSelectedRow();
                if (row >= 0) controller.edit(row);
            }
        });

        imprimirFld.addActionListener(e -> {
            String[] columnas = {"ID", "Nombre", "Teléfono"};
            List<String[]> filasPdf = new ArrayList<>();
            for (Funcionario f : filas) {
                filasPdf.add(new String[]{f.getId(), f.getNombre(), f.getTelefono()});
            }
            PdfReporter.imprimir(panel, "Reporte de Funcionarios", columnas, filasPdf);
        });

        Highlighter h = new Highlighter(Color.green);
        idFld.addMouseListener(h);
        nombreFld.addMouseListener(h);
        telefonoFld.addMouseListener(h);
    }

    public JPanel getPanel() { return panel; }
    public void setController(FuncionariosController c) { this.controller = c; }

    public Funcionario take() {
        Funcionario f = new Funcionario();
        f.setId(idFld.getText());
        f.setNombre(nombreFld.getText());
        f.setTelefono(telefonoFld.getText());
        return f;
    }

    private boolean validate() {
        boolean valid = true;
        if (idFld.getText().isEmpty()) {
            valid = false;
            idFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
        } else { idFld.setBackground(null); }
        if (nombreFld.getText().isEmpty()) {
            valid = false;
            nombreFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
        } else { nombreFld.setBackground(null); }
        return valid;
    }

    public void cargarTabla(List<Funcionario> lista) {
        filas = lista;
        modeloTabla.setRowCount(0);
        for (Funcionario f : lista) {
            modeloTabla.addRow(new Object[]{f.getId(), f.getNombre(), f.getTelefono()});
        }
    }

    public void setId(String v) { idFld.setText(v); }
    public void setNombre(String v) { nombreFld.setText(v); }
    public void setTelefono(String v) { telefonoFld.setText(v); }

    public void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(panel, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(panel, mensaje, "Informacion", JOptionPane.INFORMATION_MESSAGE);
    }

    public boolean confirmar(String mensaje) {
        return JOptionPane.showConfirmDialog(panel, mensaje, "Confirmar", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    public void limpiarFormulario() {
        idFld.setText("");
        nombreFld.setText("");
        telefonoFld.setText("");
        idFld.setBackground(null);
        nombreFld.setBackground(null);
    }
}