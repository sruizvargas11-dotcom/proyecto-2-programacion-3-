package una.eif206.data;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Recurso;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD de RECURSO. Nombres exactos a script_reservas_oracle11g.sql:
 * insertarRecurso, modificarRecurso, eliminarRecurso, buscarRecurso,
 * listarRecurso, listarRecursoPorCategoria. mapearLista/mapearFila son
 * package-private: ServicioReserva las reutiliza para listarRecursoPorReserva
 * (mismas columnas).
 */
public class ServicioRecurso extends Servicio {

    private static final String INSERTAR = "{call insertarRecurso(?,?,?)}";
    private static final String MODIFICAR = "{call modificarRecurso(?,?,?)}";
    private static final String ELIMINAR = "{call eliminarRecurso(?)}";
    private static final String BUSCAR = "{?=call buscarRecurso(?)}";
    private static final String LISTAR = "{?=call listarRecurso()}";
    private static final String LISTAR_POR_CATEGORIA = "{?=call listarRecursoPorCategoria(?)}";

    public ServicioRecurso() {
        super();
    }

    public ServicioRecurso(ConexionProvider conexionProvider) {
        super(conexionProvider);
    }

    public List<Recurso> listar() throws GlobalException, NoDataException {
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

    public List<Recurso> listarPorCategoria(String categoriaId) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(LISTAR_POR_CATEGORIA)) {
            registrarCursorSalida(cs, 1);
            cs.setString(2, categoriaId);
            cs.execute();
            return mapearLista(cs);
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    public Recurso buscar(String id) throws GlobalException, NoDataException {
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

    public void insertar(Recurso r) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(INSERTAR)) {
            cs.setString(1, r.getId());
            cs.setString(2, r.getDescripcion());
            cs.setString(3, r.getCategoria().getId());
            cs.execute();
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    public void modificar(Recurso r) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(MODIFICAR)) {
            cs.setString(1, r.getId());
            cs.setString(2, r.getDescripcion());
            cs.setString(3, r.getCategoria().getId());
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

    static List<Recurso> mapearLista(CallableStatement cs) throws SQLException {
        List<Recurso> lista = new ArrayList<>();
        try (ResultSet rs = (ResultSet) cs.getObject(1)) {
            while (rs.next()) {
                lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    static Recurso mapearFila(ResultSet rs) throws SQLException {
        CategoriaRecurso categoria = new CategoriaRecurso(rs.getString("CATEGORIA_ID"), rs.getString("CATEGORIA_DESCRIPCION"));
        Recurso recurso = new Recurso(rs.getString("ID"), rs.getString("DESCRIPCION"), categoria);
        recurso.setDisponible(rs.getInt("DISPONIBLE") == 1);
        return recurso;
    }
}
