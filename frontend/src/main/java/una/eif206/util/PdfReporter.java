package una.eif206.util;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PdfReporter {


    public static void imprimir(Component parent, String titulo, String[] columnas, List<String[]> filas) {
        JFileChooser chooser = new JFileChooser();
        String nombreSugerido = titulo.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
        chooser.setSelectedFile(new File(nombreSugerido));

        int opcion = chooser.showSaveDialog(parent);
        if (opcion != JFileChooser.APPROVE_OPTION) return;

        String ruta = chooser.getSelectedFile().getAbsolutePath();
        if (!ruta.toLowerCase().endsWith(".pdf")) ruta += ".pdf";

        try {
            generar(titulo, columnas, filas, ruta);
            JOptionPane.showMessageDialog(parent, "PDF generado correctamente en:\n" + ruta);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Error al generar el PDF: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    public static void generar(String titulo, String[] columnas, List<String[]> filas, String rutaArchivo) throws Exception {
        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream(rutaArchivo));
        doc.open();

        Font tituloFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
        Paragraph encabezado = new Paragraph(titulo, tituloFont);
        encabezado.setAlignment(Element.ALIGN_CENTER);
        doc.add(encabezado);
        doc.add(new Paragraph(" "));

        PdfPTable tabla = new PdfPTable(columnas.length);
        tabla.setWidthPercentage(100);

        Font colFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
        for (String col : columnas) {
            PdfPCell celda = new PdfPCell(new Phrase(col, colFont));
            celda.setBackgroundColor(BaseColor.LIGHT_GRAY);
            tabla.addCell(celda);
        }

        for (String[] fila : filas) {
            for (String valor : fila) {
                tabla.addCell(valor != null ? valor : "");
            }
        }

        doc.add(tabla);
        doc.close();
    }

    /**
     * Igual que imprimir(), pero para reportes con varias secciones/tablas
     * en un mismo PDF (por ejemplo Estadísticas: Recursos + Actividades).
     */
    public static void imprimirMultiple(Component parent, String tituloGeneral, List<Seccion> secciones) {
        JFileChooser chooser = new JFileChooser();
        String nombreSugerido = tituloGeneral.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
        chooser.setSelectedFile(new File(nombreSugerido));

        int opcion = chooser.showSaveDialog(parent);
        if (opcion != JFileChooser.APPROVE_OPTION) return;

        String ruta = chooser.getSelectedFile().getAbsolutePath();
        if (!ruta.toLowerCase().endsWith(".pdf")) ruta += ".pdf";

        try {
            generarMultiple(tituloGeneral, secciones, ruta);
            JOptionPane.showMessageDialog(parent, "PDF generado correctamente en:\n" + ruta);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Error al generar el PDF: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void generarMultiple(String tituloGeneral, List<Seccion> secciones, String rutaArchivo) throws Exception {
        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream(rutaArchivo));
        doc.open();

        Font tituloFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
        Font subtituloFont = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD);
        Font colFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);

        Paragraph encabezado = new Paragraph(tituloGeneral, tituloFont);
        encabezado.setAlignment(Element.ALIGN_CENTER);
        doc.add(encabezado);
        doc.add(new Paragraph(" "));

        for (Seccion s : secciones) {
            doc.add(new Paragraph(s.subtitulo, subtituloFont));
            doc.add(new Paragraph(" "));

            PdfPTable tablaSec = new PdfPTable(s.columnas.length);
            tablaSec.setWidthPercentage(100);
            for (String col : s.columnas) {
                PdfPCell celda = new PdfPCell(new Phrase(col, colFont));
                celda.setBackgroundColor(BaseColor.LIGHT_GRAY);
                tablaSec.addCell(celda);
            }
            for (String[] fila : s.filas) {
                for (String valor : fila) {
                    tablaSec.addCell(valor != null ? valor : "");
                }
            }
            doc.add(tablaSec);
            doc.add(new Paragraph(" "));
        }

        doc.close();
    }


    public static void imprimirDesdeTabla(Component parent, String titulo, JTable tabla) {
        int colCount = tabla.getColumnCount();
        String[] columnas = new String[colCount];
        for (int i = 0; i < colCount; i++) {
            columnas[i] = tabla.getColumnName(i);
        }

        List<String[]> filas = new ArrayList<>();
        for (int r = 0; r < tabla.getRowCount(); r++) {
            String[] fila = new String[colCount];
            for (int c = 0; c < colCount; c++) {
                Object valor = tabla.getValueAt(r, c);
                fila[c] = valor != null ? valor.toString() : "";
            }
            filas.add(fila);
        }

        imprimir(parent, titulo, columnas, filas);
    }

    public static class Seccion {
        public String subtitulo;
        public String[] columnas;
        public List<String[]> filas;

        public Seccion(String subtitulo, String[] columnas, List<String[]> filas) {
            this.subtitulo = subtitulo;
            this.columnas = columnas;
            this.filas = filas;
        }
    }
}