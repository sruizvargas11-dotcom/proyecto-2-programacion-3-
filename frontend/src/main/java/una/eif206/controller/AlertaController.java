package una.eif206.controller;

import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.view.AlertaPanel;

import java.util.ArrayList;
import java.util.List;

public class AlertaController {

    private final AlertaPanel vista;
    private final List<Reserva> alertas = new ArrayList<>();

    public AlertaController(AlertaPanel vista) {
        this.vista = vista;
        vista.setController(this);
        vista.cargarTabla(alertas);
    }

    public void agregar(Reserva reserva) {
        if (reserva == null) return;
        alertas.add(reserva);
        vista.cargarTabla(alertas);
    }

    public void ver(int fila) {
        if (fila < 0 || fila >= alertas.size()) return;
        vista.mostrarDetalle(alertas.get(fila));
    }

    public void descartar(int fila) {
        if (fila < 0 || fila >= alertas.size()) return;
        Reserva reserva = alertas.get(fila);
        if (vista.confirmarDescartar(reserva)) {
            alertas.remove(reserva);
            vista.cargarTabla(alertas);
        }
    }

    public void limpiar() {
        alertas.clear();
        vista.cargarTabla(alertas);
    }

    public int cantidad() { return alertas.size(); }

    // ---------------------------------------------------------------
    // TEMPORAL: solo para probar el panel antes de tener el Backend.
    // Borrar este metodo (y el boton que lo usa) en la Fase C.
    // ---------------------------------------------------------------
    private int contadorPrueba = 0;

    public void simularAlerta() {
        String[][] ejemplos = {
                {"Juan Mora", "Reunion clientes", "2026-07-31", "08:00", "10:00"},
                {"Pedro Arias", "Capacitacion", "2026-08-03", "13:00", "15:00"},
                {"Juan Mora", "Cafe de cumpleanos", "2026-08-05", "15:00", "16:00"},
                {"Ana Lopez", "Coordinacion viaje", "2026-08-07", "09:00", "11:00"}
        };
        String[] e = ejemplos[contadorPrueba % ejemplos.length];
        contadorPrueba++;

        Funcionario encargado = new Funcionario("000", e[0], "", "");
        Reserva reserva = new Reserva("PRUEBA-" + contadorPrueba, e[1], e[2], e[3], e[4], encargado);
        reserva.getRecursos().add(new Recurso("R1", "Laptop #238715", null));
        reserva.getRecursos().add(new Recurso("R2", "Sala 1 primer piso", null));
        agregar(reserva);
    }
}