package una.eif206.view;

import com.toedter.calendar.JDateChooser;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import una.eif206.ApplicationLogin;
import una.eif206.controller.EstadisticasController;
import una.eif206.util.IconLoader;
import una.eif206.util.PdfReporter;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EstadisticasView {

    private static final SimpleDateFormat FORMATO_ISO = new SimpleDateFormat("yyyy-MM-dd");

    private JPanel panel;
    private JDateChooser desdeFld;
    private JDateChooser hastaFld;
    private JButton generarFld;
    private JButton imprimirFld;
    private ChartPanel panelRecursos;
    private ChartPanel panelActividades;

    private Map<String, Integer> datosRecursos = new LinkedHashMap<>();
    private Map<String, Integer> datosActividades = new LinkedHashMap<>();

    EstadisticasController controller;

    public EstadisticasView() {
        panel      = new JPanel(new BorderLayout(5, 5));
        desdeFld   = new JDateChooser();
        hastaFld   = new JDateChooser();
        desdeFld.setDateFormatString("yyyy-MM-dd");
        hastaFld.setDateFormatString("yyyy-MM-dd");
        desdeFld.setPreferredSize(new Dimension(120, 25));
        hastaFld.setPreferredSize(new Dimension(120, 25));
        generarFld = new JButton("Generar");
        imprimirFld = new JButton("Imprimir PDF");
        generarFld.setIcon(IconLoader.load("generar"));
        imprimirFld.setIcon(IconLoader.load("imprimir"));

        JPanel filtros = new JPanel();
        filtros.add(new JLabel("Desde:"));
        filtros.add(desdeFld);
        filtros.add(new JLabel("Hasta:"));
        filtros.add(hastaFld);
        filtros.add(generarFld);
        filtros.add(imprimirFld);

        JFreeChart chartRecursos = ChartFactory.createBarChart(
                "Recursos más reservados", "Categoría", "Cantidad",
                new DefaultCategoryDataset(), PlotOrientation.VERTICAL,
                false, true, false);
        JFreeChart chartActividades = ChartFactory.createBarChart(
                "Actividades más frecuentes", "Semana", "Cantidad",
                new DefaultCategoryDataset(), PlotOrientation.VERTICAL,
                false, true, false);

        panelRecursos    = new ChartPanel(chartRecursos);
        panelActividades = new ChartPanel(chartActividades);

        JPanel graficos = new JPanel(new GridLayout(1, 2, 5, 5));
        graficos.add(panelRecursos);
        graficos.add(panelActividades);

        panel.add(filtros, BorderLayout.NORTH);
        panel.add(graficos, BorderLayout.CENTER);

        generarFld.addActionListener(e -> {
            if (validate()) {
                String desde = FORMATO_ISO.format(desdeFld.getDate());
                String hasta = FORMATO_ISO.format(hastaFld.getDate());
                controller.generar(desde, hasta);
            }
        });

        imprimirFld.addActionListener(e -> {
            List<String[]> filasRecursos = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : datosRecursos.entrySet()) {
                filasRecursos.add(new String[]{entry.getKey(), String.valueOf(entry.getValue())});
            }
            List<String[]> filasActividades = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : datosActividades.entrySet()) {
                filasActividades.add(new String[]{entry.getKey(), String.valueOf(entry.getValue())});
            }

            List<PdfReporter.Seccion> secciones = new ArrayList<>();
            secciones.add(new PdfReporter.Seccion("Recursos más reservados",
                    new String[]{"Categoría", "Cantidad"}, filasRecursos));
            secciones.add(new PdfReporter.Seccion("Actividades más frecuentes",
                    new String[]{"Semana", "Cantidad"}, filasActividades));

            PdfReporter.imprimirMultiple(panel, "Reporte de Estadísticas", secciones);
        });
    }

    public JPanel getPanel() { return panel; }
    public void setController(EstadisticasController c) { this.controller = c; }

    private boolean validate() {
        boolean valid = true;
        if (desdeFld.getDate() == null) {
            valid = false;
            desdeFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
        } else { desdeFld.setBackground(null); }
        if (hastaFld.getDate() == null) {
            valid = false;
            hastaFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
        } else { hastaFld.setBackground(null); }
        return valid;
    }

    private DefaultCategoryDataset toDataset(Map<String, Integer> datos, String serieNombre) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Integer> entry : datos.entrySet()) {
            dataset.addValue(entry.getValue(), serieNombre, entry.getKey());
        }
        return dataset;
    }

    public void mostrarGraficos(Map<String, Integer> datosRecursos, Map<String, Integer> datosActividades) {
        this.datosRecursos = datosRecursos;
        this.datosActividades = datosActividades;
        panelRecursos.getChart().getCategoryPlot().setDataset(toDataset(datosRecursos, "Reservas"));
        panelActividades.getChart().getCategoryPlot().setDataset(toDataset(datosActividades, "Reservas"));
        panel.revalidate();
    }

    public void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(panel, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}