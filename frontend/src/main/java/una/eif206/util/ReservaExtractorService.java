package una.eif206.util;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ReservaExtractorService {

    @SystemMessage("""
        Eres un asistente especializado en extraer información de reservas.
        1. Fecha en formato yyyy-MM-dd
        2. Horas en formato HH:mm de 24 horas
        3. Solo usa categorias de la lista proporcionada
        4. Si falta un campo devuelve null
        """)
    @UserMessage("""
        Fecha de hoy: {{hoy}}
        Categorias disponibles: {{categorias}}
        Frase: "{{frase}}"
        """)
    ReservaExtraccion extraer(@V("frase") String frase,
                              @V("categorias") String categorias,
                              @V("hoy") String hoy);
}
