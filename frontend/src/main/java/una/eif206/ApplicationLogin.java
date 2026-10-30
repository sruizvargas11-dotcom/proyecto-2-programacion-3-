package una.eif206;

import una.eif206.client.FakeService;
import una.eif206.client.Service;
import una.eif206.client.SocketListener;
import una.eif206.util.ConfigCliente;
import una.eif206.controller.ActividadesController;
import una.eif206.controller.AlertaController;
import una.eif206.controller.CalendarizacionController;
import una.eif206.controller.CambiarClaveController;
import una.eif206.controller.CategoriasController;
import una.eif206.controller.EstadisticasController;
import una.eif206.controller.FuncionariosController;
import una.eif206.controller.LoginController;
import una.eif206.controller.RecursosController;
import una.eif206.controller.ReservasController;
import una.eif206.model.Usuario;
import una.eif206.model.enums.UsuarioRol;
import una.eif206.protocol.IService;
import una.eif206.util.Sesion;
import una.eif206.view.ActividadesView;
import una.eif206.view.AlertaPanel;
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

public class ApplicationLogin extends JFrame {

    public static final Color BACKGROUND_ERROR = new Color(255, 102, 102);
    private JTabbedPane tabs;
    private AlertaController alertaController;

    private final IService service;
    /** Canal de alertas (socket ASYNC). Es null en modo prueba (FakeService). */
    private final SocketListener listener;

    public ApplicationLogin(IService service, SocketListener listener) {
        if (Sesion.getUsuario() == null) {
            throw new IllegalStateException("No hay usuario en sesion");
        }

        this.service = service;
        this.listener = listener;

        // Como en el enunciado: "SISTEMA DE RESERVAS - 222 (FUNCIONARIO)"
        Usuario usuario = Sesion.getUsuario();
        setTitle("SISTEMA DE RESERVAS - " + usuario.getId() + " (" + usuario.getRol() + ")");
        setSize(1050, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tabs = new JTabbedPane();

        //  Crear todos los MVCs
        FuncionariosView fView = new FuncionariosView();
        new FuncionariosController(fView, service);

        CategoriasView catView = new CategoriasView();
        new CategoriasController(catView, service);

        RecursosView recView = new RecursosView();
        new RecursosController(recView, service);

        ReservasView resView = new ReservasView();
        new ReservasController(resView, service);

        CalendarizacionView calView = new CalendarizacionView();
        new CalendarizacionController(calView, service);

        ActividadesView actView = new ActividadesView();
        new ActividadesController(actView, service);

        EstadisticasView estView = new EstadisticasView();
        new EstadisticasController(estView, service);

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

        //  Panel de alertas
        AlertaPanel alertaPanel = new AlertaPanel();
        alertaController = new AlertaController(alertaPanel);
        if (listener != null) {
            listener.setDestino(alertaController);
        }

        botonesPanel.add(cambiarClaveBtn);
        botonesPanel.add(cerrarSesionBtn);
        topPanel.add(botonesPanel, BorderLayout.EAST);

        String rolTexto = usuario.getRol() == UsuarioRol.ADMIN ? "Administrador" : "Funcionario";
        JLabel opcionesLbl = new JLabel("Opciones: " + rolTexto + " - " + usuario.getNombre());
        opcionesLbl.setFont(opcionesLbl.getFont().deriveFont(Font.BOLD));
        opcionesLbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        topPanel.add(opcionesLbl, BorderLayout.WEST);

        add(topPanel, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(alertaPanel.getPanel(), BorderLayout.EAST);
    }

    private void abrirCambiarClave() {
        una.eif206.model.Usuario u = Sesion.getUsuario();
        CambiarClaveView ccv = new CambiarClaveView();
        new CambiarClaveController(ccv, service, u);
        ccv.setVisible(true);
    }

    private void cerrarSesion() {
        if (listener != null) {
            listener.setDestino(null);
        }
        alertaController.limpiar();
        Sesion.logout();
        dispose();

        SwingUtilities.invokeLater(() -> {
            Runnable onLoginExitoso = () -> new ApplicationLogin(service, listener).setVisible(true);
            LoginView loginView = new LoginView();
            LoginController loginController = new LoginController(loginView, service, onLoginExitoso);
            loginView.setController(loginController);
            loginView.setVisible(true);
        });
    }

    private static boolean conexionPerdidaAvisada = false;

    private static void avisarConexionPerdida() {
        if (conexionPerdidaAvisada) return;
        conexionPerdidaAvisada = true;
        JOptionPane.showMessageDialog(null,
                "Se perdio la conexion con el servidor.\nLa aplicacion se va a cerrar.",
                "Conexion perdida", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {}

        // Patron Proxy: FakeService (datos de prueba) o Service (Backend real).
        IService service;
        SocketListener listener = null;
        if (ConfigCliente.usarServidor()) {
            Service proxy;
            try {
                // Socket SYNC: pedidos y respuestas.
                proxy = new Service(ConfigCliente.getHost(), ConfigCliente.getPuerto());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        e.getMessage() + "\nVerifique que el Backend este encendido.",
                        "Error de conexion", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
                return;
            }
            try {
                // Socket ASYNC: Usa el sid que el Backend le dio al socket SYNC.
                listener = new SocketListener(proxy.getHost(), proxy.getPuerto(), proxy.getSid());
                listener.start();
            } catch (Exception e) {
                listener = null;
                JOptionPane.showMessageDialog(null,
                        "No se pudo abrir el canal de alertas. La aplicacion funciona, pero sin alertas.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
            }
            // Si se cae el Backend (lo detecta cualquiera de los dos sockets): avisar y cerrar.
            proxy.setAlPerderConexion(ApplicationLogin::avisarConexionPerdida);
            if (listener != null) listener.setAlPerderConexion(ApplicationLogin::avisarConexionPerdida);

            // Al cerrar el programa: detener el listener y avisar LOGOUT al Backend.
            final SocketListener listenerFinal = listener;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (listenerFinal != null) listenerFinal.stop();
                proxy.cerrar();
            }));
            service = proxy;
        } else {
            service = new FakeService();
        }
        final SocketListener listenerApp = listener;

        SwingUtilities.invokeLater(() -> {

            Runnable onLoginExitoso = () -> new ApplicationLogin(service, listenerApp).setVisible(true);

            LoginView loginView = new LoginView();
            LoginController loginController = new LoginController(loginView, service, onLoginExitoso);
            loginView.setController(loginController);
            loginView.setVisible(true);
        });
    }
}