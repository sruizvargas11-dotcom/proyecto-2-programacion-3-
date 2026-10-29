package una.eif206.data;

import una.eif206.model.Funcionario;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD de funcionarios contra la tabla USUARIO (ROL='FUNCIONARIO'). Nombres
 * de procedimientos exactos a script_reservas_oracle11g.sql: insertarFuncionario,
 * modificarFuncionario, eliminarFuncionario, buscarFuncionario, listarFuncionario,
 * listarFuncionarioTexto.
 */
public class ServicioFuncionario extends Servicio {

    private static final String INSERTAR = "{call insertarFuncionario(?,?,?,?,?)}";
    private static final String MODIFICAR = "{call modificarFuncionario(?,?,?,?)}";
    private static final String ELIMINAR = "{call eliminarFuncionario(?)}";
    private static final String BUSCAR = "{?=call buscarFuncionario(?)}";
    private static final String LISTAR = "{?=call listarFuncionario()}";
    private static final String LISTAR_TEXTO = "{?=call listarFuncionarioTexto(?)}";

    public ServicioFuncionario() {
        super();
    }

    public ServicioFuncionario(ConexionProvider conexionProvider) {
        super(conexionProvider);
    }

    public List<Funcionario> listar() throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(LISTAR)) {
            registrarCursorSalida(cs, 1);
            cs.execute();
            return mapearLista(cs);
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    public List<Funcionario> listarPorTexto(String texto) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(LISTAR_TEXTO)) {
            registrarCursorSalida(cs, 1);
            cs.setString(2, texto);
            cs.execute();
            return mapearLista(cs);
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    public Funcionario buscar(String id) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(BUSCAR)) {
            registrarCursorSalida(cs, 1);
            cs.setString(2, id);
            cs.execute();
            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                return rs.next() ? mapearFila(rs) : null;
            }
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    public void insertar(Funcionario f) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(INSERTAR)) {
            cs.setString(1, f.getId());
            cs.setString(2, f.getNombre());
            cs.setString(3, f.getClave());
            cs.setString(4, f.getDepartamento());
            cs.setString(5, f.getTelefono());
            cs.execute();
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    public void modificar(Funcionario f) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(MODIFICAR)) {
            cs.setString(1, f.getId());
            cs.setString(2, f.getNombre());
            cs.setString(3, f.getDepartamento());
            cs.setString(4, f.getTelefono());
            cs.execute();
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    public void eliminar(String id) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(ELIMINAR)) {
            cs.setString(1, id);
            cs.execute();
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    private static List<Funcionario> mapearLista(CallableStatement cs) throws SQLException {
        List<Funcionario> lista = new ArrayList<>();
        try (ResultSet rs = (ResultSet) cs.getObject(1)) {
            while (rs.next()) {
                lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    static Funcionario mapearFila(ResultSet rs) throws SQLException {
        Funcionario f = new Funcionario(rs.getString("ID"), rs.getString("NOMBRE"),
                rs.getString("DEPARTAMENTO"), rs.getString("TELEFONO"));
        f.setClave(rs.getString("CLAVE"));
        return f;
    }
}
