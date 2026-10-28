package una.eif206.view;

import com.toedter.calendar.JDateChooser;
import una.eif206.ApplicationLogin;
import una.eif206.controller.ReservasController;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.util.Highlighter;
import una.eif206.util.IconLoader;
import una.eif206.util.PdfReporter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReservasView {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("H:mm");
    private static final SimpleDateFormat FORMATO_ISO = new SimpleDateFormat("yyyy-MM-dd");

    private JPanel panel;
    private JTextField actividadFld;
    private JDateChooser fechaFld;
    private JTextField horaInicioFld;
    private JTextField horaFinFld;
    private JTextField fraseFld;
    private DefaultListModel<CategoriaRecurso> categoriasListModel;
    private JList<CategoriaRecurso> categoriasList;
    private JButton guardarFld;
    private JButton cancelarReservaFld;
    private JButton limpiarFld;
    private JButton imprimirFld;
    private JButton extraerIAFld;
    private JTable tabla;

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Actividad", "Fecha", "Hora Inicio", "Hora Fin", "Estado", "Recursos"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) { return false; }
    };
    private List<Reserva> filas = new ArrayList<>();

    ReservasController controller;

    public ReservasView() {
        panel               = new JPanel(new BorderLayout(5, 5));
        actividadFld        = new JTextField(20);
        fechaFld             = new JDateChooser();
        fechaFld.setDateFormatString("yyyy-MM-dd");
        fechaFld.setPreferredSize(new Dimension(120, 25));
        horaInicioFld        = new JTextField(5);
        horaFinFld           = new JTextField(5);
        fraseFld             = new JTextField(20);
        categoriasListModel  = new DefaultListModel<>();
        categoriasList       = new JList<>(categoriasListModel);
        guardarFld           = new JButton("Crear reserva");
        cancelarReservaFld   = new JButton("Cancelar reserva");
        limpiarFld           = new JButton("Limpiar");
        imprimirFld          = new JButton("Imprimir PDF");
        extraerIAFld         = new JButton("Extraer AI");
        guardarFld.setIcon(IconLoader.load("crear"));
        cancelarReservaFld.setIcon(IconLoader.load("cancelar"));
        limpiarFld.setIcon(IconLoader.load("limpiar"));
        imprimirFld.setIcon(IconLoader.load("imprimir"));
        extraerIAFld.setIcon(IconLoader.load("buscar"));
        tabla                = new JTable();
        tabla.setModel(modeloTabla);

        categoriasList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JPanel form = new JPanel(new GridLayout(4, 2, 5, 5));
        form.add(new JLabel("Actividad:"));             form.add(actividadFld);
        form.add(new JLabel("Fecha:"));                  form.add(fechaFld);
        form.add(new JLabel("Hora inicio (HH:mm):"));    form.add(horaInicioFld);
        form.add(new JLabel("Hora fin (HH:mm):"));       form.add(horaFinFld);

        JPanel iaPanel = new JPanel(new BorderLayout(5, 5));
        iaPanel.setBorder(BorderFactory.createTitledBorder("Extraer reserva con IA"));
        iaPanel.add(fraseFld, BorderLayout.CENTER);
        iaPanel.add(extraerIAFld, BorderLayout.EAST);

        JScrollPane categoriasScroll = new JScrollPane(categoriasList);
        categoriasScroll.setBorder(BorderFactory.createTitledBorder("Categorías a reservar"));

        JPanel botones = new JPanel();
        botones.add(guardarFld);
        botones.add(cancelarReservaFld);
        botones.add(limpiarFld);
        botones.add(imprimirFld);

        JPanel south = new JPanel(new BorderLayout(5, 5));
        south.add(iaPanel, BorderLayout.NORTH);
        south.add(botones, BorderLayout.SOUTH);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(form, BorderLayout.CENTER);
        top.add(categoriasScroll, BorderLayout.EAST);
        top.add(south, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        guardarFld.addActionListener(e -> {
            if (validate()) {
                controller.create(actividadFld.getText(), FORMATO_ISO.format(fechaFld.getDate()),
                        horaInicioFld.getText(), horaFinFld.getText(),
                        categoriasList.getSelectedValuesList());
            }
        });

        cancelarReservaFld.addActionListener(e -> {
            int op = JOptionPane.showConfirmDialog(panel, "¿Cancelar la reserva seleccionada?");
            if (op == JOptionPane.YES_OPTION) {
                controller.cancelar();
            }
        });

        limpiarFld.addActionListener(e -> {
            controller.clear();
            categoriasList.clearSelection();
            tabla.clearSelection();
        });

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tabla.getSelectedRow();
                if (row >= 0) controller.edit(row);
            }
        });

        imprimirFld.addActionListener(e ->
                PdfReporter.imprimirDesdeTabla(panel, "Reporte de Reservas", tabla));

        extraerIAFld.addActionListener(e -> controller.extraerConIA());

        Highlighter h = new Highlighter(Color.green);
        actividadFld.addMouseListener(h);
        horaInicioFld.addMouseListener(h);
        horaFinFld.addMouseListener(h);
    }

    public JPanel getPanel() { return panel; }
    public void setController(ReservasController c) { this.controller = c; }

    private boolean validate() {
        boolean valid = true;
        valid &= checkField(actividadFld);
        if (fechaFld.getDate() == null) {
            valid = false;
            fechaFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
        } else {
            fechaFld.setBackground(null);
        }
        boolean horaInicioValida = checkHoraField(horaInicioFld);
        boolean horaFinValida = checkHoraField(horaFinFld);
        valid &= horaInicioValida;
        valid &= horaFinValida;
        if (horaInicioValida && horaFinValida) {
            LocalTime inicio = LocalTime.parse(horaInicioFld.getText(), FORMATO_HORA);
            LocalTime fin = LocalTime.parse(horaFinFld.getText(), FORMATO_HORA);
            if (!inicio.isBefore(fin)) {
                valid = false;
                horaInicioFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
                horaFinFld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
                JOptionPane.showMessageDialog(panel, "La hora de inicio debe ser anterior a la hora de fin.",
                        "Validación", JOptionPane.WARNING_MESSAGE);
            }
        }
        if (categoriasList.getSelectedValuesList().isEmpty()) {
            valid = false;
            JOptionPane.showMessageDialog(panel, "Seleccione al menos una categoría.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
        }
        return valid;
    }

    private boolean checkField(JTextField fld) {
        if (fld.getText().isEmpty()) {
            fld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
            return false;
        }
        fld.setBackground(null);
        return true;
    }

    private boolean checkHoraField(JTextField fld) {
        if (!checkField(fld)) {
            return false;
        }
        try {
            LocalTime.parse(fld.getText(), FORMATO_HORA);
            fld.setToolTipText(null);
            return true;
        } catch (DateTimeParseException ex) {
            fld.setBackground(ApplicationLogin.BACKGROUND_ERROR);
            fld.setToolTipText("Formato: HH:mm");
            return false;
        }
    }

    public void limpiarFormulario() {
        actividadFld.setText("");
        fechaFld.setDate(null);
        horaInicioFld.setText("");
        horaFinFld.setText("");
        actividadFld.setBackground(null);
        fechaFld.setBackground(null);
        horaInicioFld.setBackground(null);
        horaFinFld.setBackground(null);
    }

    public void cargarTabla(List<Reserva> lista) {
        filas = lista;
        modeloTabla.setRowCount(0);
        for (Reserva r : lista) {
            String recursos = r.getRecursos() == null ? "" : r.getRecursos().stream()
                    .map(Recurso::getId).collect(Collectors.joining(", "));
            modeloTabla.addRow(new Object[]{r.getId(), r.getActividad(), r.getFecha(),
                    r.getHoraInicio(), r.getHoraFin(), r.getEstado(), recursos});
        }
    }

    public void cargarCategorias(List<CategoriaRecurso> categorias) {
        categoriasListModel.clear();
        categorias.forEach(categoriasListModel::addElement);
    }

    public void setCancelarHabilitado(boolean habilitado) {
        cancelarReservaFld.setEnabled(habilitado);
    }

    public String getFrase() { return fraseFld.getText(); }
    public void setActividad(String v) { actividadFld.setText(v); }
    public void setFecha(LocalDate fecha) { fechaFld.setDate(java.sql.Date.valueOf(fecha)); }
    public void setHoraInicio(String v) { horaInicioFld.setText(v); }
    public void setHoraFin(String v) { horaFinFld.setText(v); }

    public void seleccionarCategoriasPorNombre(List<String> nombres) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < categoriasListModel.size(); i++) {
            String descripcion = categoriasListModel.get(i).getDescripcion();
            for (String nombre : nombres) {
                if (nombre != null && descripcion.equalsIgnoreCase(nombre.trim())) {
                    indices.add(i);
                    break;
                }
            }
        }
        if (!indices.isEmpty()) {
            int[] arr = new int[indices.size()];
            for (int i = 0; i < arr.length; i++) arr[i] = indices.get(i);
            categoriasList.setSelectedIndices(arr);
        }
    }

    public void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(panel, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(panel, mensaje, "Informacion", JOptionPane.INFORMATION_MESSAGE);
    }
}
