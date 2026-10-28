package una.eif206.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlIDREF;
import una.eif206.model.enums.EstadoReserva;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Reserva {

    private String id;
    private String actividad;
    private String fecha;
    private String horaInicio;
    private String horaFin;
    @XmlIDREF
    private Funcionario funcionario;
    @XmlIDREF
    @XmlElement(name = "recurso")
    private List<Recurso> recursos;
    private EstadoReserva estado;

    public Reserva(String id, String actividad, String fecha,
                   String horaInicio, String horaFin, Funcionario funcionario) {
        this.id = id;
        this.actividad = actividad;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.funcionario = funcionario;
        this.recursos = new ArrayList<>();
        this.estado = EstadoReserva.ACTIVA;
    }

    public Reserva() {
        this.id = "";
        this.actividad = "";
        this.fecha = "";
        this.horaInicio = "";
        this.horaFin = "";
        this.recursos = new ArrayList<>();
        this.estado = EstadoReserva.ACTIVA;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getActividad() { return actividad; }
    public void setActividad(String a) { this.actividad = a; }
    public String getFecha() { return fecha; }
    public void setFecha(String f) { this.fecha = f; }
    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String h) { this.horaInicio = h; }
    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String h) { this.horaFin = h; }
    public Funcionario getFuncionario() { return funcionario; }
    public void setFuncionario(Funcionario f) { this.funcionario = f; }
    public List<Recurso> getRecursos() { return recursos; }
    public void setRecursos(List<Recurso> r) { this.recursos = r; }
    public EstadoReserva getEstado() { return estado; }
    public void setEstado(EstadoReserva e) { this.estado = e; }
}