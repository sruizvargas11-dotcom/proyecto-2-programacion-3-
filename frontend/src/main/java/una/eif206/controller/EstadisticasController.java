package una.eif206.controller;

import una.eif206.protocol.IService;
import una.eif206.view.EstadisticasView;

public class EstadisticasController {

    private final EstadisticasView vista;
    private final IService service;

    public EstadisticasController(EstadisticasView vista, IService service) {
        this.vista = vista;
        this.service = service;
        vista.setController(this);
    }

    public void generar(String desde, String hasta) {
        try {
            vista.mostrarGraficos(
                    service.getEstadisticasRecursos(desde, hasta),
                    service.getEstadisticasActividades(desde, hasta));
        } catch (Exception ex) {
            vista.mostrarError("Error inesperado: " + ex.getMessage());
        }
    }
}
