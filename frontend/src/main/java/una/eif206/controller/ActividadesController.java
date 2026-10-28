package una.eif206.controller;

import una.eif206.service.ActividadesService;
import una.eif206.view.ActividadesView;

public class ActividadesController {

    private final ActividadesView vista;
    private final ActividadesService actividadesService;

    public ActividadesController(ActividadesView vista, ActividadesService actividadesService) {
        this.vista = vista;
        this.actividadesService = actividadesService;
        vista.setController(this);
    }

    public void cargarSemana(String fechaReferencia) {
        try {
            vista.pintarMatriz(actividadesService.getActividadesSemana(fechaReferencia));
        } catch (Exception ex) {
            vista.mostrarError("Error inesperado: " + ex.getMessage());
        }
    }
}
