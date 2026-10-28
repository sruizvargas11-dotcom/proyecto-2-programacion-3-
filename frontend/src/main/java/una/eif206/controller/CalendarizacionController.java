package una.eif206.controller;

import una.eif206.model.CategoriaRecurso;
import una.eif206.service.CalendarioService;
import una.eif206.service.CategoriaService;
import una.eif206.view.CalendarizacionView;

public class CalendarizacionController {

    private final CalendarizacionView vista;
    private final CalendarioService calendarioService;
    private final CategoriaService categoriaService;

    public CalendarizacionController(CalendarizacionView vista, CalendarioService calendarioService, CategoriaService categoriaService) {
        this.vista = vista;
        this.calendarioService = calendarioService;
        this.categoriaService = categoriaService;
        vista.setController(this);
        vista.cargarCategorias(categoriaService.findAll());
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
            vista.mostrarMatriz(calendarioService.findByCategoria(categoria),
                    calendarioService.getCalendario(fecha, categoria));
        } catch (Exception ex) {
            vista.mostrarError("Error inesperado: " + ex.getMessage());
        }
    }
}
