package una.eif206.service;

import una.eif206.data.OracleRepositorio;
import una.eif206.data.Repositorio;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.ReservaExtraccion;
import una.eif206.model.Usuario;
import una.eif206.protocol.IService;

import java.util.List;
import java.util.Map;

/**
 * Implementacion de IService que delega en los services del P1, los cuales
 * reciben un Repositorio (OracleRepositorio por defecto en produccion;
 * MemoriaRepositorio en pruebas).
 */
public class Service implements IService {

    private final Repositorio repositorio;

    private final UsuarioService usuarioService;
    private final FuncionarioService funcionarioService;
    private final CategoriaService categoriaService;
    private final RecursoService recursoService;
    private final ReservaService reservaService;
    private final CalendarioService calendarioService;
    private final ActividadesService actividadesService;
    private final EstadisticasService estadisticasService;
    private final ReservaIAService reservaIAService;

    public Service() {
        this(new OracleRepositorio());
    }

    public Service(Repositorio repositorio) {
        this.repositorio = repositorio;
        this.usuarioService = new UsuarioService(repositorio);
        this.funcionarioService = new FuncionarioService(repositorio);
        this.categoriaService = new CategoriaService(repositorio);
        this.recursoService = new RecursoService(repositorio);
        this.reservaService = new ReservaService(repositorio);
        this.calendarioService = new CalendarioService(repositorio);
        this.actividadesService = new ActividadesService(repositorio);
        this.estadisticasService = new EstadisticasService(repositorio);
        this.reservaIAService = new ReservaIAService(repositorio);
    }

    public Repositorio getRepositorio() {
        return repositorio;
    }

    // ===================== UsuarioService =====================

    @Override
    public Usuario login(String id, String clave) {
        return usuarioService.login(id, clave);
    }

    @Override
    public String cambiarClave(Usuario u, String claveActual, String claveNueva) {
        return usuarioService.cambiarClave(u, claveActual, claveNueva);
    }

    // ===================== FuncionarioService =====================

    @Override
    public List<Funcionario> findAllFuncionarios() {
        return funcionarioService.findAll();
    }

    @Override
    public List<Funcionario> search(Funcionario e) {
        return funcionarioService.search(e);
    }

    @Override
    public List<Funcionario> search(String texto) {
        return funcionarioService.search(texto);
    }

    @Override
    public String create(Funcionario e) {
        return funcionarioService.create(e);
    }

    @Override
    public String update(Funcionario f) {
        return funcionarioService.update(f);
    }

    @Override
    public String delete(Funcionario e) {
        return funcionarioService.delete(e);
    }

    @Override
    public Funcionario findFuncionarioById(String id) {
        return funcionarioService.findById(id);
    }

    // ===================== CategoriaService =====================

    @Override
    public List<CategoriaRecurso> findAllCategorias() {
        return categoriaService.findAll();
    }

    @Override
    public CategoriaRecurso findCategoriaById(String id) {
        return categoriaService.findById(id);
    }

    @Override
    public List<CategoriaRecurso> search(CategoriaRecurso e) {
        return categoriaService.search(e);
    }

    @Override
    public String create(CategoriaRecurso e) {
        return categoriaService.create(e);
    }

    @Override
    public String update(CategoriaRecurso c) {
        return categoriaService.update(c);
    }

    @Override
    public String delete(CategoriaRecurso e) {
        return categoriaService.delete(e);
    }

    // ===================== RecursoService =====================

    @Override
    public List<Recurso> findAllRecursos() {
        return recursoService.findAll();
    }

    @Override
    public List<Recurso> findByCategoria(CategoriaRecurso categoria) {
        return recursoService.findByCategoria(categoria);
    }

    @Override
    public String create(Recurso e) {
        return recursoService.create(e);
    }

    @Override
    public String update(Recurso r) {
        return recursoService.update(r);
    }

    @Override
    public String delete(Recurso e) {
        return recursoService.delete(e);
    }

    // ===================== ReservaService =====================

    @Override
    public List<Reserva> findAllReservas() {
        return reservaService.findAll();
    }

    @Override
    public List<Reserva> findByFuncionario(Funcionario f) {
        return reservaService.findByFuncionario(f);
    }

    @Override
    public String generarId() {
        return reservaService.generarId();
    }

    @Override
    public String create(Reserva e) {
        return reservaService.create(e);
    }

    @Override
    public Recurso buscarRecursoDisponible(CategoriaRecurso categoria, String fecha, String horaInicio, String horaFin) {
        return reservaService.buscarRecursoDisponible(categoria, fecha, horaInicio, horaFin);
    }

    @Override
    public String asignarRecursosDisponibles(List<CategoriaRecurso> categorias, String fecha,
                                              String horaInicio, String horaFin, List<Recurso> recursosAsignados) {
        return reservaService.asignarRecursosDisponibles(categorias, fecha, horaInicio, horaFin, recursosAsignados);
    }

    @Override
    public String cancelar(Reserva r) {
        return reservaService.cancelar(r);
    }

    // ===================== CalendarioService =====================

    @Override
    public String[][] getCalendario(String fecha, CategoriaRecurso categoria) {
        return calendarioService.getCalendario(fecha, categoria);
    }

    // ===================== ActividadesService =====================

    @Override
    public Map<String, Map<String, String>> getActividadesSemana(String fechaReferencia) throws Exception {
        return actividadesService.getActividadesSemana(fechaReferencia);
    }

    // ===================== EstadisticasService =====================

    @Override
    public Map<String, Integer> getEstadisticasRecursos(String desde, String hasta) throws Exception {
        return estadisticasService.getEstadisticasRecursos(desde, hasta);
    }

    @Override
    public Map<String, Integer> getEstadisticasActividades(String desde, String hasta) throws Exception {
        return estadisticasService.getEstadisticasActividades(desde, hasta);
    }

    // ===================== ReservaIAService =====================

    @Override
    public ReservaExtraccion extraerReserva(String frase) throws Exception {
        return reservaIAService.extraerReserva(frase);
    }
}
