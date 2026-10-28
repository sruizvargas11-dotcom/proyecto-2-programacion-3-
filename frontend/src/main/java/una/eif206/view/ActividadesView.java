package una.eif206.view;

import com.toedter.calendar.JDateChooser;
import una.eif206.ApplicationLogin;
import una.eif206.controller.ActividadesController;
import una.eif206.service.ActividadesService;
import una.eif206.util.IconLoader;
import una.eif206.util.PdfReporter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class ActividadesView {

    private static final String[] DIAS = ActividadesService.NOMBRES_DIAS;
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("H:mm");
    private static final SimpleDateFormat FORMATO_ISO = new SimpleDateFormat("yyyy-MM-dd");

    private JPanel panel;
    private JDateChooser fechaFld;
    private JButton cargarFld;
    private JButton imprimirFld;
    private JTable tabla;
    private DefaultTableModel tableModel;

    ActividadesController controller;

    public ActividadesView() {
        panel     = new JPanel(new BorderLayout(5, 5));
        fechaFld  = new JDateChooser();
        fechaFld.setDateFormatString("yyyy-MM-dd");
        fechaFld.setPreferredSize(new Dimension(120, 25));
        cargarFld = new JButton("Cargar semana");
        imprimirFld = new JButton("Imprimir PDF");
        cargarFld.setIcon(IconLoader.load("cargar"));
        imprimirFld.setIcon(IconLoader.load("imprimir"));
        tabla     = new JTable();

        tableModel = new DefaultTableModel();
        tableModel.addColumn("Hora");
        for (String dia : DIAS) {
            tableModel.addColumn(dia);
        }
        tabla.setModel(tableModel);

        JPanel top = new JPanel();
        top.add(new JLabel("Fecha de referencia:"));
        top.add(fechaFld);
        top.add(cargarFld);
        top.add(imprimirFld);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        cargarFld.addActionListener(e -> {
            if (fechaFld.getDate() == null) {
                fechaFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
                return;
            }
            fechaFld.setBackground(null);
            controller.cargarSemana(FORMATO_ISO.format(fechaFld.getDate()));
        });

        imprimirFld.addActionListener(e ->
                PdfReporter.imprimirDesdeTabla(panel, "Reporte de Actividades", tabla));
    }

    public JPanel getPanel() { return panel; }
    public void setController(ActividadesController c) { this.controller = c; }

    public void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(panel, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void pintarMatriz(Map<String, Map<String, String>> matriz) {
        tableModel.setRowCount(0);
        if (matriz == null) {
            return;
        }
        Map<String, Map<String, String>> ordenado = new TreeMap<>(
                Comparator.comparing(h -> LocalTime.parse(h, FORMATO_HORA)));
        ordenado.putAll(matriz);
        for (Map.Entry<String, Map<String, String>> fila : ordenado.entrySet()) {
            Object[] row = new Object[DIAS.length + 1];
            row[0] = fila.getKey();
            Map<String, String> porDia = fila.getValue();
            for (int i = 0; i < DIAS.length; i++) {
                String texto = (porDia != null) ? porDia.get(DIAS[i]) : null;
                row[i + 1] = (texto != null) ? texto : "";
            }
            tableModel.addRow(row);
        }
        panel.revalidate();
    }
}