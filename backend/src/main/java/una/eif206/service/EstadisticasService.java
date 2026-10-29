package una.eif206.service;

import una.eif206.data.Repositorio;

import java.util.Map;

public class EstadisticasService {

    private final Repositorio repositorio;

    public EstadisticasService(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    public Map<String, Integer> getEstadisticasRecursos(String desde, String hasta) throws Exception {
        return repositorio.estadisticasRecursos(desde, hasta);
    }

    public Map<String, Integer> getEstadisticasActividades(String desde, String hasta) throws Exception {
        return repositorio.estadisticasActividades(desde, hasta);
    }
}
