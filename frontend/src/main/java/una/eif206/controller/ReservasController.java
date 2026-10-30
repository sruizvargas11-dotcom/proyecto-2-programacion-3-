package una.eif206.controller;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.Usuario;
import una.eif206.model.ReservaExtraccion;
import una.eif206.model.enums.EstadoReserva;
import una.eif206.model.enums.UsuarioRol;
import una.eif206.protocol.IService;
import una.eif206.util.Sesion;
import una.eif206.view.ReservasView;

import javax.swing.SwingWorker;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservasController {

    private final ReservasView vista;
    private final IService service;
    private List<Reserva> listaActual = new ArrayList<>();
    private Reserva current;

    public ReservasController(ReservasView vista, IService service) {
        this.vista = vista;
        this.service = service;
        vista.setController(this);
        vista.cargarCategorias(service.findAllCategorias());
        cargarLista();
    }

    public void cargarLista() {
        Usuario usuario = Sesion.getUsuario();
        if (usuario.getRol() == UsuarioRol.ADMIN) {
            listaActual = service.findAllReservas();
        } else {
            listaActual = service.findByFuncionario((Funcionario) usuario);
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

            Reserva reserva = new Reserva("", actividad, fecha, horaInicio, horaFin, funcionario);
            for (CategoriaRecurso categoria : categoriasSeleccionadas) {
                reserva.getRecursos().add(new Recurso("", "", categoria));
            }

            String error = service.create(reserva);
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
            String error = service.cancelar(current);
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
        String frase = vista.getFrase();
        if (frase == null || frase.trim().isEmpty()) {
            vista.mostrarError("Escriba una frase para extraer.");
            return;
        }
        vista.setCargandoIA(true);
        new SwingWorker<ReservaExtraccion, Void>() {
            @Override
            protected ReservaExtraccion doInBackground() throws Exception {
                return service.extraerReserva(frase);
            }

            @Override
            protected void done() {
                vista.setCargandoIA(false);
                try {
                    llenarDesdeIA(get());
                } catch (Exception e) {
                    // El detalle tecnico va a la consola; al usuario, un mensaje claro.
                    System.err.println("[IA] " + (e.getCause() != null ? e.getCause() : e));
                    vista.mostrarError("No se pudo procesar la frase con IA.\n"
                            + "Intente de nuevo o llene los campos manualmente.");
                }
            }
        }.execute();
    }

    private void llenarDesdeIA(ReservaExtraccion r) {
        if (r == null) return;
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
    }

    public void extraerConIA() {
        procesarIA();
    }
}