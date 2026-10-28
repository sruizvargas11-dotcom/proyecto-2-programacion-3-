package una.eif206.view;

import una.eif206.controller.AlertaController;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.util.IconLoader;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class AlertaPanel {

    private static final int COL_ACTIVIDAD = 0;
    private static final int COL_VER = 1;
    private static final int COL_DESCARTAR = 2;

    private JPanel panel;
    private JTable tabla;
    private final ImageIcon iconoVer = IconLoader.load("ver");
    private final ImageIcon iconoDescartar = IconLoader.load("descartar");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(new Object[]{"Actividad", "Ver", "Desc."}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) { return false; }

        @Override
        public Class<?> getColumnClass(int col) {
            if (col == COL_VER || col == COL_DESCARTAR) return Icon.class;
            return String.class;
        }
    };

    AlertaController controller;

    public AlertaPanel() {
        panel = new JPanel(new BorderLayout(5, 5));
        panel.setPreferredSize(new Dimension(260, 0));

        JLabel titulo = new JLabel("Alertas");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD));

        tabla = new JTable(modeloTabla);
        tabla.setRowHeight(22);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getColumnModel().getColumn(COL_ACTIVIDAD).setPreferredWidth(180);
        tabla.getColumnModel().getColumn(COL_VER).setPreferredWidth(35);
        tabla.getColumnModel().getColumn(COL_VER).setMaxWidth(40);
        tabla.getColumnModel().getColumn(COL_DESCARTAR).setPreferredWidth(50);
        tabla.getColumnModel().getColumn(COL_DESCARTAR).setMinWidth(50);
        tabla.getColumnModel().getColumn(COL_DESCARTAR).setMaxWidth(55);

        panel.add(titulo, BorderLayout.NORTH);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        // Clic en la columna Ver o Desc. de una fila
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = tabla.rowAtPoint(e.getPoint());
                int columna = tabla.columnAtPoint(e.getPoint());
                if (fila < 0) return;
                if (columna == COL_VER) {
                    controller.ver(fila);
                } else if (columna == COL_DESCARTAR) {
                    controller.descartar(fila);
                }
            }
        });
    }

    public JPanel getPanel() { return panel; }
    public void setController(AlertaController c) { this.controller = c; }

    public void cargarTabla(List<Reserva> alertas) {
        modeloTabla.setRowCount(0);
        for (Reserva r : alertas) {
            modeloTabla.addRow(new Object[]{textoFila(r), iconoVer, iconoDescartar});
        }
    }

    private String textoFila(Reserva r) {
        return nombreEncargado(r) + " - " + r.getActividad();
    }

    private String nombreEncargado(Reserva r) {
        if (r.getFuncionario() == null) return "(sin encargado)";
        return r.getFuncionario().getNombre();
    }

    public void mostrarDetalle(Reserva r) {
        JPanel datos = new JPanel(new GridLayout(4, 2, 10, 5));
        datos.add(new JLabel("Encargado:")); datos.add(new JLabel(nombreEncargado(r)));
        datos.add(new JLabel("Fecha:"));     datos.add(new JLabel(r.getFecha()));
        datos.add(new JLabel("Desde:"));     datos.add(new JLabel(r.getHoraInicio()));
        datos.add(new JLabel("Hasta:"));     datos.add(new JLabel(r.getHoraFin()));

        DefaultListModel<String> modeloRecursos = new DefaultListModel<>();
        if (r.getRecursos() != null) {
            for (Recurso rec : r.getRecursos()) {
                modeloRecursos.addElement(rec.getDescripcion());
            }
        }
        JList<String> listaRecursos = new JList<>(modeloRecursos);
        listaRecursos.setEnabled(false);
        JScrollPane scrollRecursos = new JScrollPane(listaRecursos);
        scrollRecursos.setPreferredSize(new Dimension(160, 90));

        JPanel recursos = new JPanel(new BorderLayout(10, 5));
        recursos.add(new JLabel("Recursos"), BorderLayout.WEST);
        recursos.add(scrollRecursos, BorderLayout.CENTER);

        JPanel contenido = new JPanel(new BorderLayout(5, 10));
        contenido.add(datos, BorderLayout.NORTH);
        contenido.add(recursos, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(panel, contenido, r.getActividad(), JOptionPane.PLAIN_MESSAGE);
    }

    public boolean confirmarDescartar(Reserva r) {
        return JOptionPane.showConfirmDialog(panel, r.getActividad(), "Descartar?",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                == JOptionPane.YES_OPTION;
    }
}