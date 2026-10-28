package una.eif206.view;

import una.eif206.ApplicationLogin;
import una.eif206.controller.CategoriasController;
import una.eif206.model.CategoriaRecurso;
import una.eif206.util.Highlighter;
import una.eif206.util.IconLoader;
import una.eif206.util.PdfReporter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriasView {

    private JPanel panel;
    private JTextField idFld;
    private JTextField descripcionFld;
    private JTextField busquedaFld;
    private JButton guardarFld;
    private JButton modificarFld;
    private JButton cancelarFld;
    private JButton buscarFld;
    private JButton borrarFld;
    private JButton imprimirFld;
    private JTable tabla;

    private final DefaultTableModel modeloTabla = new DefaultTableModel(new Object[]{"ID", "Descripción"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) { return false; }
    };
    private List<CategoriaRecurso> filas = new ArrayList<>();

    CategoriasController controller;

    public CategoriasView() {
        panel          = new JPanel(new BorderLayout(5, 5));
        idFld          = new JTextField(10);
        descripcionFld = new JTextField(20);
        busquedaFld    = new JTextField(15);
        guardarFld     = new JButton("Guardar");
        modificarFld   = new JButton("Modificar");
        cancelarFld    = new JButton("Limpiar");
        buscarFld      = new JButton("Buscar");
        borrarFld      = new JButton("Borrar");
        imprimirFld    = new JButton("Imprimir PDF");
        tabla          = new JTable();
        tabla.setModel(modeloTabla);

        idFld.setEditable(false);

        guardarFld.setIcon(IconLoader.load("guardar"));
        modificarFld.setIcon(IconLoader.load("modificar"));
        cancelarFld.setIcon(IconLoader.load("limpiar"));
        buscarFld.setIcon(IconLoader.load("buscar"));
        borrarFld.setIcon(IconLoader.load("borrar"));
        imprimirFld.setIcon(IconLoader.load("imprimir"));

        JPanel form = new JPanel(new GridLayout(4, 2, 5, 5));
        form.add(new JLabel("ID:"));          form.add(idFld);
        form.add(new JLabel("Descripción:")); form.add(descripcionFld);
        form.add(new JLabel("Buscar:"));      form.add(busquedaFld);

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
            String[] columnas = {"ID", "Descripción"};
            List<String[]> filasPdf = new ArrayList<>();
            for (CategoriaRecurso c : filas) {
                filasPdf.add(new String[]{c.getId(), c.getDescripcion()});
            }
            PdfReporter.imprimir(panel, "Reporte de Categorías", columnas, filasPdf);
        });

        Highlighter h = new Highlighter(Color.green);
        descripcionFld.addMouseListener(h);
    }

    public JPanel getPanel() { return panel; }
    public void setController(CategoriasController c) { this.controller = c; }

    public CategoriaRecurso take() {
        CategoriaRecurso c = new CategoriaRecurso();
        c.setId(idFld.getText());
        c.setDescripcion(descripcionFld.getText());
        return c;
    }

    private boolean validate() {
        boolean valid = true;
        if (descripcionFld.getText().isEmpty()) {
            valid = false;
            descripcionFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
        } else { descripcionFld.setBackground(null); }
        return valid;
    }

    public void cargarTabla(List<CategoriaRecurso> lista) {
        filas = lista;
        modeloTabla.setRowCount(0);
        for (CategoriaRecurso c : lista) {
            modeloTabla.addRow(new Object[]{c.getId(), c.getDescripcion()});
        }
    }

    public void setId(String v) { idFld.setText(v); }
    public void setDescripcion(String v) { descripcionFld.setText(v); descripcionFld.setBackground(null); }

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
        descripcionFld.setText("");
        descripcionFld.setBackground(null);
    }
}