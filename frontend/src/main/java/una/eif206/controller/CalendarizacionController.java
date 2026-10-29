package una.eif206.controller;

import una.eif206.model.CategoriaRecurso;
import una.eif206.protocol.IService;
import una.eif206.view.CalendarizacionView;

public class CalendarizacionController {

    private final CalendarizacionView vista;
    private final IService service;

    public CalendarizacionController(CalendarizacionView vista, IService service) {
        this.vista = vista;
        this.service = service;
        vista.setController(this);
        vista.cargarCategorias(service.findAllCategorias());
    }

    public void cargar(String fecha, CategoriaRecurso categoria) {
        try {
            if (fecha == null || fecha.isEmpty()) {
                vista.mostrarError("Debe ingresar una fecha");
                return;
            }
            if (categoria == null) {
                vista.mostrarError("Debe seleccionar una categoria");
                return;
            }
            vista.mostrarMatriz(service.findByCategoria(categoria),
                    service.getCalendario(fecha, categoria));
        } catch (Exception ex) {
            vista.mostrarError("Error inesperado: " + ex.getMessage());
        }
    }
}
