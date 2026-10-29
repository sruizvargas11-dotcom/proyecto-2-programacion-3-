package una.eif206.controller;

import una.eif206.protocol.IService;
import una.eif206.view.ActividadesView;

public class ActividadesController {

    private final ActividadesView vista;
    private final IService service;

    public ActividadesController(ActividadesView vista, IService service) {
        this.vista = vista;
        this.service = service;
        vista.setController(this);
    }

    public void cargarSemana(String fechaReferencia) {
        try {
            vista.pintarMatriz(service.getActividadesSemana(fechaReferencia));
        } catch (Exception ex) {
            vista.mostrarError("Error inesperado: " + ex.getMessage());
        }
    }
}
