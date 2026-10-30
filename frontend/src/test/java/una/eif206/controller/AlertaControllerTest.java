package una.eif206.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.view.AlertaPanel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de unidad del panel de alertas (sin ventanas ni dialogos reales). */
class AlertaControllerTest {

    /** Vista de prueba: en vez de abrir dialogos, guarda lo que se le pidio. */
    static class AlertaPanelDePrueba extends AlertaPanel {
        boolean respuestaDescartar = true;
        Reserva detalleMostrado;
        int filasEnTabla;

        @Override public void cargarTabla(List<Reserva> alertas) { filasEnTabla = alertas.size(); }
        @Override public void mostrarDetalle(Reserva r) { detalleMostrado = r; }
        @Override public boolean confirmarDescartar(Reserva r) { return respuestaDescartar; }
    }

    private AlertaPanelDePrueba vista;
    private AlertaController controller;

    @BeforeEach
    void setUp() {
        vista = new AlertaPanelDePrueba();
        controller = new AlertaController(vista);
    }

    private Reserva alerta(String actividad) {
        Reserva r = new Reserva("RES-1", actividad, "2026-12-01", "08:00", "10:00",
                new Funcionario("222", "Maria Lopez", "", ""));
        r.getRecursos().add(new Recurso("1", "Laptop #238715", null));
        return r;
    }

    @Test
    void alLlegarUnaAlerta_seAgregaALaTabla() {
        controller.deliver_message(alerta("Reunion clientes"));
        assertEquals(1, controller.cantidad());
        assertEquals(1, vista.filasEnTabla);
    }

    @Test
    void verMuestraElDetalleDeLaFila() {
        Reserva r = alerta("Capacitacion");
        controller.deliver_message(r);
        controller.ver(0);
        assertSame(r, vista.detalleMostrado);
    }

    @Test
    void descartarConSi_quitaLaAlerta() {
        controller.deliver_message(alerta("Reunion"));
        vista.respuestaDescartar = true;
        controller.descartar(0);
        assertEquals(0, controller.cantidad());
    }

    @Test
    void descartarConNo_laAlertaSeQueda() {
        controller.deliver_message(alerta("Reunion"));
        vista.respuestaDescartar = false;
        controller.descartar(0);
        assertEquals(1, controller.cantidad());
    }

    @Test
    void limpiar_vaciaLaLista() {
        controller.deliver_message(alerta("A"));
        controller.deliver_message(alerta("B"));
        controller.limpiar();
        assertEquals(0, controller.cantidad());
        assertEquals(0, vista.filasEnTabla);
    }

    @Test
    void filaInvalida_noHaceNada() {
        controller.ver(5);
        controller.descartar(-1);
        assertNull(vista.detalleMostrado);
    }
}
