package una.eif206.data;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agregados calculados en SQL (GROUP BY), no en Java. Nombres exactos a
 * script_reservas_oracle11g.sql: estadisticaRecursosCategoria,
 * estadisticaActividadesSemana (renombradas desde el enunciado original para
 * no exceder el limite de 30 caracteres de un identificador en Oracle 11g).
 */
public class ServicioEstadisticas extends Servicio {

    private static final String RECURSOS_POR_CATEGORIA = "{?=call estadisticaRecursosCategoria(?,?)}";
    private static final String ACTIVIDADES_POR_SEMANA = "{?=call estadisticaActividadesSemana(?,?)}";

    public ServicioEstadisticas() {
        super();
    }

    public ServicioEstadisticas(ConexionProvider conexionProvider) {
        super(conexionProvider);
    }

    public Map<String, Integer> estadisticasRecursos(String desde, String hasta) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(RECURSOS_POR_CATEGORIA)) {
            registrarCursorSalida(cs, 1);
            cs.setDate(2, Date.valueOf(desde));
            cs.setDate(3, Date.valueOf(hasta));
            cs.execute();
            Map<String, Integer> resultado = new LinkedHashMap<>();
            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                while (rs.next()) {
                    resultado.put(rs.getString("CATEGORIA_DESCRIPCION"), rs.getInt("CANTIDAD"));
                }
            }
            return resultado;
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }

    public Map<String, Integer> estadisticasActividades(String desde, String hasta) throws GlobalException, NoDataException {
        conectar();
        try (CallableStatement cs = conexion.prepareCall(ACTIVIDADES_POR_SEMANA)) {
            registrarCursorSalida(cs, 1);
            cs.setDate(2, Date.valueOf(desde));
            cs.setDate(3, Date.valueOf(hasta));
            cs.execute();
            Map<String, Integer> resultado = new LinkedHashMap<>();
            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                while (rs.next()) {
                    resultado.put(rs.getDate("SEMANA_INICIO").toLocalDate().toString(), rs.getInt("CANTIDAD"));
                }
            }
            return resultado;
        } catch (SQLException e) {
            throw errorSql(e);
        } finally {
            desconectar();
        }
    }
}
