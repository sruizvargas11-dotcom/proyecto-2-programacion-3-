package una.eif206.view;

import com.toedter.calendar.JDateChooser;
import una.eif206.ApplicationLogin;
import una.eif206.controller.CalendarizacionController;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Recurso;
import una.eif206.util.IconLoader;
import una.eif206.util.PdfReporter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class CalendarizacionView {

    private static final SimpleDateFormat FORMATO_ISO = new SimpleDateFormat("yyyy-MM-dd");

    private JPanel panel;
    private JDateChooser fechaFld;
    private JComboBox<CategoriaRecurso> categoriaFld;
    private JButton cargarFld;
    private JButton imprimirFld;
    private JTable tabla;

    CalendarizacionController controller;

    public CalendarizacionView() {
        panel        = new JPanel(new BorderLayout(5, 5));
        fechaFld     = new JDateChooser();
        fechaFld.setDateFormatString("yyyy-MM-dd");
        fechaFld.setPreferredSize(new Dimension(120, 25));
        categoriaFld = new JComboBox<>();
        cargarFld    = new JButton("Cargar");
        imprimirFld  = new JButton("Imprimir PDF");
        cargarFld.setIcon(IconLoader.load("cargar"));
        imprimirFld.setIcon(IconLoader.load("imprimir"));
        tabla        = new JTable();
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.add(new JLabel("Fecha:"));
        filtros.add(fechaFld);
        filtros.add(new JLabel("Categoria:"));
        filtros.add(categoriaFld);
        filtros.add(cargarFld);
        filtros.add(imprimirFld);

        panel.add(filtros, BorderLayout.NORTH);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        cargarFld.addActionListener(e -> {
            if (fechaFld.getDate() == null) {
                fechaFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
                mostrarError("Debe seleccionar una fecha");
                return;
            }
            fechaFld.setBackground(null);
            controller.cargar(
                    FORMATO_ISO.format(fechaFld.getDate()),
                    (CategoriaRecurso) categoriaFld.getSelectedItem()
            );
        });

        imprimirFld.addActionListener(e ->
                PdfReporter.imprimirDesdeTabla(panel, "Reporte de Calendarización", tabla));
    }

    public JPanel getPanel() { return panel; }
    public void setController(CalendarizacionController c) { this.controller = c; }

    public void cargarCategorias(List<CategoriaRecurso> categorias) {
        categoriaFld.removeAllItems();
        for (CategoriaRecurso c : categorias) {
            categoriaFld.addItem(c);
        }
    }

    public void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(panel, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void mostrarMatriz(List<Recurso> recursos, String[][] matriz) {
        actualizarTabla(recursos, matriz);
        panel.revalidate();
    }

    private void actualizarTabla(List<Recurso> recursos, String[][] matriz) {
        if (matriz == null || matriz.length == 0 || recursos == null || recursos.isEmpty()) return;

        String[] headers = new String[recursos.size() + 1];
        headers[0] = "Hora";
        for (int i = 0; i < recursos.size(); i++) {
            headers[i + 1] = recursos.get(i).getDescripcion();
        }

        String[][] data = new String[matriz.length][headers.length];
        for (int i = 0; i < matriz.length; i++) {
            for (int j = 0; j < headers.length; j++) {
                data[i][j] = (j < matriz[i].length && matriz[i][j] != null) ? matriz[i][j] : "";
            }
        }

        DefaultTableModel tableModel = new DefaultTableModel(data, headers) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tabla.setModel(tableModel);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(60);
        for (int i = 1; i < headers.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(200);
        }
    }
}