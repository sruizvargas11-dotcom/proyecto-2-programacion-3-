package una.eif206.controller;

import una.eif206.client.ThreadListener;
import una.eif206.model.Reserva;
import una.eif206.view.AlertaPanel;

import java.util.ArrayList;
import java.util.List;

public class AlertaController implements ThreadListener {

    private final AlertaPanel vista;
    private final List<Reserva> alertas = new ArrayList<>();

    public AlertaController(AlertaPanel vista) {
        this.vista = vista;
        vista.setController(this);
        vista.cargarTabla(alertas);
    }

    @Override
    public void deliver_message(Reserva reserva) {
        agregar(reserva);
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
}