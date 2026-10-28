package una.eif206;

import una.eif206.controller.ActividadesController;
import una.eif206.controller.CalendarizacionController;
import una.eif206.controller.CambiarClaveController;
import una.eif206.controller.CategoriasController;
import una.eif206.controller.EstadisticasController;
import una.eif206.controller.FuncionariosController;
import una.eif206.controller.LoginController;
import una.eif206.controller.RecursosController;
import una.eif206.controller.ReservasController;
import una.eif206.data.Data;
import una.eif206.data.XMLHelper;
import una.eif206.service.ActividadesService;
import una.eif206.service.CalendarioService;
import una.eif206.service.CategoriaService;
import una.eif206.service.EstadisticasService;
import una.eif206.service.FuncionarioService;
import una.eif206.service.RecursoService;
import una.eif206.service.ReservaIAService;
import una.eif206.service.ReservaService;
import una.eif206.service.UsuarioService;
import una.eif206.util.Sesion;
import una.eif206.view.ActividadesView;
import una.eif206.view.CalendarizacionView;
import una.eif206.view.CambiarClaveView;
import una.eif206.view.CategoriasView;
import una.eif206.view.EstadisticasView;
import una.eif206.view.FuncionariosView;
import una.eif206.view.LoginView;
import una.eif206.view.RecursosView;
import una.eif206.view.ReservasView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ApplicationLogin extends JFrame {

    public static final Color BACKGROUND_ERROR = new Color(255, 102, 102);
    private JTabbedPane tabs;

    private final UsuarioService usuarioService;
    private final FuncionarioService funcionarioService;
    private final CategoriaService categoriaService;
    private final RecursoService recursoService;
    private final ReservaService reservaService;
    private final EstadisticasService estadisticasService;
    private final ActividadesService actividadesService;
    private final CalendarioService calendarioService;
    private final ReservaIAService reservaIAService;

    public ApplicationLogin(UsuarioService usuarioService, FuncionarioService funcionarioService,
                             CategoriaService categoriaService, RecursoService recursoService,
                             ReservaService reservaService, EstadisticasService estadisticasService,
                             ActividadesService actividadesService, CalendarioService calendarioService,
                             ReservaIAService reservaIAService) {
        if (Sesion.getUsuario() == null) {
            throw new IllegalStateException("No hay usuario en sesion");
        }

        this.usuarioService = usuarioService;
        this.funcionarioService = funcionarioService;
        this.categoriaService = categoriaService;
        this.recursoService = recursoService;
        this.reservaService = reservaService;
        this.estadisticasService = estadisticasService;
        this.actividadesService = actividadesService;
        this.calendarioService = calendarioService;
        this.reservaIAService = reservaIAService;

        setTitle("Sistema de Reserva de Recursos - " +
                Sesion.getUsuario().getId() + " (" +
                Sesion.getUsuario().getRol() + ")");
        setSize(1050, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabs = new JTabbedPane();

        //  Crear todos los MVCs
        FuncionariosView fView = new FuncionariosView();
        new FuncionariosController(fView, funcionarioService);

        CategoriasView catView = new CategoriasView();
        new CategoriasController(catView, categoriaService);

        RecursosView recView = new RecursosView();
        new RecursosController(recView, recursoService, categoriaService);

        ReservasView resView = new ReservasView();
        new ReservasController(resView, reservaService, categoriaService, reservaIAService);

        CalendarizacionView calView = new CalendarizacionView();
        new CalendarizacionController(calView, calendarioService, categoriaService);

        ActividadesView actView = new ActividadesView();
        new ActividadesController(actView, actividadesService);

        EstadisticasView estView = new EstadisticasView();
        new EstadisticasController(estView, estadisticasService);

        //  Tabs segun rol
        switch (Sesion.getUsuario().getRol()) {
            case ADMIN:
                tabs.addTab("Funcionarios",    fView.getPanel());
                tabs.addTab("Categorias",      catView.getPanel());
                tabs.addTab("Recursos",        recView.getPanel());
                tabs.addTab("Calendarizacion", calView.getPanel());
                tabs.addTab("Actividades",     actView.getPanel());
                tabs.addTab("Estadisticas",    estView.getPanel());
                break;
            case FUNCIONARIO:
                tabs.addTab("Reservas",        resView.getPanel());
                tabs.addTab("Calendarizacion", calView.getPanel());
                tabs.addTab("Actividades",     actView.getPanel());
                tabs.addTab("Estadisticas",    estView.getPanel());
                break;
        }

        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel botonesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton cambiarClaveBtn = new JButton("Cambiar Clave");
        cambiarClaveBtn.addActionListener(e -> {
            try {
                abrirCambiarClave();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error: " + ex.getMessage());
            }
        });

        JButton cerrarSesionBtn = new JButton("Cerrar Sesion");
        cerrarSesionBtn.addActionListener(e -> {
            int opcion = JOptionPane.showConfirmDialog(this,
                    "Esta seguro que desea cerrar sesion?",
                    "Cerrar Sesion", JOptionPane.YES_NO_OPTION);
            if (opcion == JOptionPane.YES_OPTION) {
                cerrarSesion();
            }
        });

        botonesPanel.add(cambiarClaveBtn);
        botonesPanel.add(cerrarSesionBtn);
        topPanel.add(botonesPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    private void abrirCambiarClave() {
        una.eif206.model.Usuario u = Sesion.getUsuario();
        CambiarClaveView ccv = new CambiarClaveView();
        new CambiarClaveController(ccv, usuarioService, u);
        ccv.setVisible(true);
    }

    private void cerrarSesion() {
        Sesion.logout();
        dispose();

        SwingUtilities.invokeLater(() -> {
            Runnable onLoginExitoso = () -> new ApplicationLogin(
                    usuarioService, funcionarioService, categoriaService, recursoService,
                    reservaService, estadisticasService, actividadesService, calendarioService,
                    reservaIAService).setVisible(true);
            LoginView loginView = new LoginView();
            LoginController loginController = new LoginController(loginView, usuarioService, onLoginExitoso);
            loginView.setController(loginController);
            loginView.setVisible(true);
        });
    }

    public static void main(String[] args) {
        Data data;
        try {
            data = XMLHelper.instance().load();
        } catch (Exception e) {
            data = new Data();
            JOptionPane.showMessageDialog(null,
                    "No se pudo cargar data.xml. Se iniciara con datos vacios.",
                    "Advertencia", JOptionPane.WARNING_MESSAGE);
        }
        final Data d = data;

        UsuarioService usuarioService = new UsuarioService(d);
        FuncionarioService funcionarioService = new FuncionarioService(d);
        CategoriaService categoriaService = new CategoriaService(d);
        RecursoService recursoService = new RecursoService(d);
        ReservaService reservaService = new ReservaService(d);
        EstadisticasService estadisticasService = new EstadisticasService(d);
        ActividadesService actividadesService = new ActividadesService(d);
        CalendarioService calendarioService = new CalendarioService(d);
        ReservaIAService reservaIAService = new ReservaIAService(d);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                XMLHelper.instance().store(d);
            } catch (Throwable e) {
                System.err.println("Error al guardar: " + e.getMessage());
            }
        }));

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception e) {}

            Runnable onLoginExitoso = () -> new ApplicationLogin(
                    usuarioService, funcionarioService, categoriaService, recursoService,
                    reservaService, estadisticasService, actividadesService, calendarioService,
                    reservaIAService).setVisible(true);

            LoginView loginView = new LoginView();
            LoginController loginController = new LoginController(loginView, usuarioService, onLoginExitoso);
            loginView.setController(loginController);
            loginView.setVisible(true);
        });
    }
}
