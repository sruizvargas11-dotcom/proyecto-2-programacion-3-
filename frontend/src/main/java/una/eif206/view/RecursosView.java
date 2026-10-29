package una.eif206.view;

import una.eif206.ApplicationLogin;
import una.eif206.controller.RecursosController;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Recurso;
import una.eif206.util.Highlighter;
import una.eif206.util.IconLoader;
import una.eif206.util.PdfReporter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class RecursosView {

    private JPanel panel;
    private JTextField idFld;
    private JTextField descripcionFld;
    private JComboBox<CategoriaRecurso> categoriaFld;
    private JComboBox<CategoriaRecurso> filtroFld;
    private JButton guardarFld;
    private JButton modificarFld;
    private JButton cancelarFld;
    private JButton borrarFld;
    private JButton imprimirFld;
    private JTable tabla;

    private boolean loadingCategorias = false;

    private final DefaultTableModel modeloTabla = new DefaultTableModel(new Object[]{"ID", "Descripción", "Categoría"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) { return false; }
    };
    private List<Recurso> filas = new ArrayList<>();

    RecursosController controller;

    public RecursosView() {
        panel          = new JPanel(new BorderLayout(5, 5));
        idFld          = new JTextField(10);
        descripcionFld = new JTextField(20);
        categoriaFld   = new JComboBox<>();
        filtroFld      = new JComboBox<>();
        guardarFld     = new JButton("Guardar");
        modificarFld   = new JButton("Modificar");
        cancelarFld    = new JButton("Limpiar");
        borrarFld      = new JButton("Borrar");
        imprimirFld    = new JButton("Imprimir PDF");
        tabla          = new JTable();
        tabla.setModel(modeloTabla);

        JPanel form = new JPanel(new GridLayout(4, 2, 5, 5));
        form.add(new JLabel("ID:"));          form.add(idFld);
        form.add(new JLabel("Descripción:")); form.add(descripcionFld);
        form.add(new JLabel("Categoría:"));   form.add(categoriaFld);

        guardarFld.setIcon(IconLoader.load("guardar"));
        modificarFld.setIcon(IconLoader.load("modificar"));
        cancelarFld.setIcon(IconLoader.load("limpiar"));
        borrarFld.setIcon(IconLoader.load("borrar"));
        imprimirFld.setIcon(IconLoader.load("imprimir"));

        JPanel botones = new JPanel(new GridLayout(2, 3, 5, 5));
        botones.add(guardarFld);
        botones.add(modificarFld);
        botones.add(cancelarFld);
        botones.add(borrarFld);
        botones.add(imprimirFld);
        form.add(new JLabel(""));
        form.add(botones);

        JPanel filtroPanel = new JPanel();
        filtroPanel.add(new JLabel("Filtrar por categoría:"));
        filtroPanel.add(filtroFld);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(form, BorderLayout.NORTH);
        norte.add(filtroPanel, BorderLayout.SOUTH);

        panel.add(norte, BorderLayout.NORTH);
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

        borrarFld.addActionListener(e -> {
            int op = JOptionPane.showConfirmDialog(panel, "Confirma borrar?");
            if (op == JOptionPane.YES_OPTION) {
                controller.delete();
            }
        });

        filtroFld.addActionListener(e -> {
            if (loadingCategorias) return;
            CategoriaRecurso seleccionada = (CategoriaRecurso) filtroFld.getSelectedItem();
            controller.filtrarPorCategoria(seleccionada);
        });

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tabla.getSelectedRow();
                if (row >= 0) controller.edit(row);
            }
        });

        imprimirFld.addActionListener(e -> {
            String[] columnas = {"ID", "Descripción", "Categoría"};
            List<String[]> filasPdf = new ArrayList<>();
            for (Recurso r : filas) {
                String cat = r.getCategoria() != null ? r.getCategoria().getDescripcion() : "";
                filasPdf.add(new String[]{r.getId(), r.getDescripcion(), cat});
            }
            PdfReporter.imprimir(panel, "Reporte de Recursos", columnas, filasPdf);
        });

        Highlighter h = new Highlighter(Color.green);
        descripcionFld.addMouseListener(h);
    }

    public JPanel getPanel() { return panel; }
    public void setController(RecursosController c) { this.controller = c; }

    public Recurso take() {
        Recurso r = new Recurso();
        r.setId(idFld.getText());
        r.setDescripcion(descripcionFld.getText());
        r.setCategoria((CategoriaRecurso) categoriaFld.getSelectedItem());
        return r;
    }

    private boolean validate() {
        boolean valid = true;
        if (idFld.getText().isEmpty()) {
            valid = false;
            idFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
        } else { idFld.setBackground(null); }
        if (descripcionFld.getText().isEmpty()) {
            valid = false;
            descripcionFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
        } else { descripcionFld.setBackground(null); }
        if (categoriaFld.getSelectedItem() == null) {
            valid = false;
            categoriaFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
        } else { categoriaFld.setBackground(null); }
        return valid;
    }

    public void cargarTabla(List<Recurso> lista) {
        filas = lista;
        modeloTabla.setRowCount(0);
        for (Recurso r : lista) {
            String cat = r.getCategoria() != null ? r.getCategoria().getDescripcion() : "";
            modeloTabla.addRow(new Object[]{r.getId(), r.getDescripcion(), cat});
        }
    }

    public void cargarCategorias(List<CategoriaRecurso> categorias) {
        loadingCategorias = true;
        categoriaFld.removeAllItems();
        for (CategoriaRecurso c : categorias) categoriaFld.addItem(c);

        filtroFld.removeAllItems();
        filtroFld.addItem(null);
        for (CategoriaRecurso c : categorias) filtroFld.addItem(c);
        loadingCategorias = false;
    }

    public void setId(String v) { idFld.setText(v); }
    public void setDescripcion(String v) { descripcionFld.setText(v); }
    public void setCategoria(CategoriaRecurso c) { categoriaFld.setSelectedItem(c); }

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
        categoriaFld.setSelectedItem(null);
        idFld.setBackground(null);
        descripcionFld.setBackground(null);
        categoriaFld.setBackground(null);
    }
}