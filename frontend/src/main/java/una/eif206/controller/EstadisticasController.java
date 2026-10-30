package una.eif206.controller;

import una.eif206.protocol.IService;
import una.eif206.view.EstadisticasView;

import javax.swing.SwingWorker;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class EstadisticasController {

    private final EstadisticasView vista;
    private final IService service;

    public EstadisticasController(EstadisticasView vista, IService service) {
        this.vista = vista;
        this.service = service;
        vista.setController(this);
    }

    /** Las dos consultas corren en otro hilo (SwingWorker); los graficos se dibujan en done(). */
    public void generar(String desde, String hasta) {
        vista.setCargando(true);
        new SwingWorker<Void, Void>() {
            private Map<String, Integer> recursos;
            private Map<String, Integer> actividades;

            @Override
            protected Void doInBackground() throws Exception {
                recursos = service.getEstadisticasRecursos(desde, hasta);
                actividades = service.getEstadisticasActividades(desde, hasta);
                return null;
            }

            @Override
            protected void done() {
                vista.setCargando(false);
                try {
                    get();
                    vista.mostrarGraficos(recursos, actividades);
                } catch (ExecutionException ex) {
                    vista.mostrarError("Error inesperado: " + ex.getCause().getMessage());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}