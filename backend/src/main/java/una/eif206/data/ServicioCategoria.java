package una.eif206.data;

import una.eif206.model.CategoriaRecurso;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD de CATEGORIA_RECURSO. Nombres exactos a script_reservas_oracle11g.sql:
 * insertarCategoria, modificarCategoria, eliminarCategoria, buscarCategoria,
 * listarCategoria, listarCategoriaTexto.
 */
public class ServicioCategoria extends Servicio {

    private static final String INSERTAR = "{call insertarCategoria(?,?)}";
    private static final String MODIFICAR = "{call modificarCategoria(?,?)}";
    private static final String ELIMINAR = "{call eliminarCategoria(?)}";
    private static final String BUSCAR = "{?=call buscarCategoria(?)}";
    private static final String LISTAR = "{?=call listarCategoria()}";
    private static final String LISTAR_TEXTO = "{?=call listarCategoriaTexto(?)}";

    public ServicioCategoria() {
        super();
    }

    public ServicioCategoria(ConexionProvider conexionProvider) {
        super(conexionProvider);
    }

    public List<CategoriaRecurso> listar() throws GlobalException, NoDataException {
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

    public List<CategoriaRecurso> listarPorTexto(String texto) throws GlobalException, NoDataException {
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

    public CategoriaRecurso buscar(String id) throws GlobalException, NoDataException {
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

    /** Inserta y devuelve el id generado por la secuencia (OUT p_id). */
    public String insertar(String descripcion) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(INSERTAR)) {
            cs.setString(1, descripcion);
            cs.registerOutParameter(2, Types.VARCHAR);
            cs.execute();
            return cs.getString(2);
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    public void modificar(CategoriaRecurso c) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(MODIFICAR)) {
            cs.setString(1, c.getId());
            cs.setString(2, c.getDescripcion());
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

    private static List<CategoriaRecurso> mapearLista(CallableStatement cs) throws SQLException {
        List<CategoriaRecurso> lista = new ArrayList<>();
        try (ResultSet rs = (ResultSet) cs.getObject(1)) {
            while (rs.next()) {
                lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    private static CategoriaRecurso mapearFila(ResultSet rs) throws SQLException {
        return new CategoriaRecurso(rs.getString("ID"), rs.getString("DESCRIPCION"));
    }
}
