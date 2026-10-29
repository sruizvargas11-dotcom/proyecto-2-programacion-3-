package una.eif206.data;

/**
 * Envuelve GlobalException/NoDataException como RuntimeException para los
 * metodos de lectura de Repositorio (findAll, search, findById, ...) que en
 * el P1 no declaran "throws Exception" y por lo tanto no pueden propagar un
 * fallo de base de datos como excepcion chequeada sin romper esa firma.
 */
public class RepositorioException extends RuntimeException {

    public RepositorioException(String message, Throwable cause) {
        super(message, cause);
    }
}
