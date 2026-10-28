package una.eif206.controller;

import una.eif206.service.EstadisticasService;
import una.eif206.view.EstadisticasView;

public class EstadisticasController {

    private final EstadisticasView vista;
    private final EstadisticasService estadisticasService;

    public EstadisticasController(EstadisticasView vista, EstadisticasService estadisticasService) {
        this.vista = vista;
        this.estadisticasService = estadisticasService;
        vista.setController(this);
    }

    public void generar(String desde, String hasta) {
        try {
            vista.mostrarGraficos(
                    estadisticasService.getEstadisticasRecursos(desde, hasta),
                    estadisticasService.getEstadisticasActividades(desde, hasta));
        } catch (Exception ex) {
            vista.mostrarError("Error inesperado: " + ex.getMessage());
        }
    }
}
