package una.eif206.data;

import jakarta.xml.bind.annotation.*;
import una.eif206.model.*;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "data")
@XmlAccessorType(XmlAccessType.FIELD)
public class Data {

    @XmlElementWrapper(name = "admins")
    @XmlElement(name = "admin")
    private List<Admin> admins;

    @XmlElementWrapper(name = "funcionarios")
    @XmlElement(name = "funcionario")
    private List<Funcionario> funcionarios;

    @XmlElementWrapper(name = "categorias")
    @XmlElement(name = "categoria")
    private List<CategoriaRecurso> categorias;

    @XmlElementWrapper(name = "recursos")
    @XmlElement(name = "recurso")
    private List<Recurso> recursos;

    @XmlElementWrapper(name = "reservas")
    @XmlElement(name = "reserva")
    private List<Reserva> reservas;

    private int categoriaCounter = 4;
    private int reservaCounter = 1;

    public Data() {
        admins = new ArrayList<>();
        admins.add(new Admin("admin", "admin123"));

        funcionarios = new ArrayList<>();
        funcionarios.add(new Funcionario("111", "Juan Perez", "TI", "3323"));
        funcionarios.add(new Funcionario("222", "Maria Lopez", "RRHH", "4444"));

        categorias = new ArrayList<>();
        categorias.add(new CategoriaRecurso("CAT-000001", "Laptop windows"));
        categorias.add(new CategoriaRecurso("CAT-000002", "Sala de Juntas"));
        categorias.add(new CategoriaRecurso("CAT-000003", "Sala para 10 personas"));

        recursos = new ArrayList<>();
        recursos.add(new Recurso("238715", "Laptop #238715", categorias.get(0)));
        recursos.add(new Recurso("45238",  "Laptop #45238",  categorias.get(0)));
        recursos.add(new Recurso("34343",  "Sala 1 primer piso", categorias.get(1)));

        reservas = new ArrayList<>();
    }

    public String generarIdCategoria() {
        return String.format("CAT-%06d", categoriaCounter++);
    }
    public String generarIdReserva() {
        return String.format("RES-%06d", reservaCounter++);
    }

    public List<Admin> getAdmins() { return admins; }
    public List<Funcionario> getFuncionarios() { return funcionarios; }
    public List<CategoriaRecurso> getCategorias() { return categorias; }
    public List<Recurso> getRecursos() { return recursos; }
    public List<Reserva> getReservas() { return reservas; }
}