package una.eif206.controller;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.Usuario;
import una.eif206.model.enums.EstadoReserva;
import una.eif206.model.enums.UsuarioRol;
import una.eif206.service.CategoriaService;
import una.eif206.service.ReservaIAService;
import una.eif206.service.ReservaService;
import una.eif206.util.ReservaExtraccion;
import una.eif206.util.Sesion;
import una.eif206.view.ReservasView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservasController {

    private final ReservasView vista;
    private final ReservaService reservaService;
    private final CategoriaService categoriaService;
    private final ReservaIAService reservaIAService;
    private List<Reserva> listaActual = new ArrayList<>();
    private Reserva current;

    public ReservasController(ReservasView vista, ReservaService reservaService,
                               CategoriaService categoriaService, ReservaIAService reservaIAService) {
        this.vista = vista;
        this.reservaService = reservaService;
        this.categoriaService = categoriaService;
        this.reservaIAService = reservaIAService;
        vista.setController(this);
        vista.cargarCategorias(categoriaService.findAll());
        cargarLista();
    }

    public void cargarLista() {
        Usuario usuario = Sesion.getUsuario();
        if (usuario.getRol() == UsuarioRol.ADMIN) {
            listaActual = reservaService.findAll();
        } else {
            listaActual = reservaService.findByFuncionario((Funcionario) usuario);
        }
        vista.cargarTabla(listaActual);
    }

    public void create(String actividad, String fecha, String horaInicio,
                        String horaFin, List<CategoriaRecurso> categoriasSeleccionadas) {
        try {
            if (!(Sesion.getUsuario() instanceof Funcionario)) {
                vista.mostrarError("Solo un funcionario puede crear reservas");
                return;
            }
            Funcionario funcionario = (Funcionario) Sesion.getUsuario();

            List<Recurso> recursos = new ArrayList<>();
            String errorDisponibilidad = reservaService.asignarRecursosDisponibles(
                    categoriasSeleccionadas, fecha, horaInicio, horaFin, recursos);
            if (errorDisponibilidad != null) {
                vista.mostrarError(errorDisponibilidad);
                return;
            }

            Reserva reserva = new Reserva(reservaService.generarId(), actividad, fecha, horaInicio, horaFin, funcionario);
            reserva.setRecursos(recursos);

            String error = reservaService.create(reserva);
            if (error != null) {
                vista.mostrarError(error);
                return;
            }
            vista.mostrarMensaje("RESERVA CREADA");
            current = null;
            vista.setCancelarHabilitado(false);
            vista.limpiarFormulario();
            cargarLista();
        } catch (Exception ex) {
            vista.mostrarError("Error inesperado: " + ex.getMessage());
        }
    }

    public void cancelar() {
        try {
            if (current == null) {
                vista.mostrarError("Seleccione una reserva de la tabla.");
                return;
            }
            String error = reservaService.cancelar(current);
            if (error != null) {
                vista.mostrarError(error);
                return;
            }
            current = null;
            vista.setCancelarHabilitado(false);
            vista.limpiarFormulario();
            cargarLista();
        } catch (Exception ex) {
            vista.mostrarError("Error inesperado: " + ex.getMessage());
        }
    }

    public void clear() {
        current = null;
        vista.setCancelarHabilitado(false);
        vista.limpiarFormulario();
    }

    public void edit(int row) {
        current = listaActual.get(row);
        boolean hayCancelable = current.getId() != null && !current.getId().isEmpty()
                && current.getEstado() != EstadoReserva.CANCELADA;
        vista.setCancelarHabilitado(hayCancelable);
    }

    private void procesarIA() {
        try {
            String frase = vista.getFrase();
            if (frase == null || frase.isEmpty()) {
                vista.mostrarError("Escriba una frase para extraer.");
                return;
            }
            ReservaExtraccion r = reservaIAService.extraerReserva(frase);
            if (r.getActividad() != null) vista.setActividad(r.getActividad());
            if (r.getFecha() != null) {
                try {
                    vista.setFecha(LocalDate.parse(r.getFecha()));
                } catch (Exception ignored) {
                    // Si la IA devuelve un formato inesperado, se deja el campo tal cual
                }
            }
            if (r.getHoraInicio() != null) vista.setHoraInicio(r.getHoraInicio());
            if (r.getHoraFinal() != null) vista.setHoraFin(r.getHoraFinal());
            if (r.getCategoriasRecurso() != null && !r.getCategoriasRecurso().isEmpty())
                vista.seleccionarCategoriasPorNombre(r.getCategoriasRecurso());
        } catch (Exception e) {
            vista.mostrarError("Error al procesar IA: " + e.getMessage());
        }
    }

    public void extraerConIA() {
        procesarIA();
    }
}
