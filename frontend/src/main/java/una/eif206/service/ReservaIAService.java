package una.eif206.service;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import una.eif206.data.Data;
import una.eif206.model.CategoriaRecurso;
import una.eif206.util.ReservaExtraccion;
import una.eif206.util.ReservaExtractorService;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.stream.Collectors;

public class ReservaIAService {

    private final Data data;
    private ReservaExtractorService reservaExtractorService;

    public ReservaIAService(Data data) {
        this.data = data;
    }

    public ReservaExtraccion extraerReserva(String frase) throws Exception {
        if (reservaExtractorService == null) {
            try {
                // Intenta con Groq usando tu API key
                OpenAiChatModel model = OpenAiChatModel.builder()
                        .baseUrl("https://api.groq.com/openai/v1")
                        .apiKey(cargarApiKey())
                        .modelName("openai/gpt-oss-20b")
                        .build();
                reservaExtractorService = AiServices.create(ReservaExtractorService.class, model);
            } catch (Exception e) {
                // Fallback silencioso al proxy demo gratuito
                OpenAiChatModel model = OpenAiChatModel.builder()
                        .baseUrl("http://langchain4j.dev/demo/openai/v1")
                        .apiKey("demo")
                        .modelName("gpt-4o-mini")
                        .build();
                reservaExtractorService = AiServices.create(ReservaExtractorService.class, model);
            }
        }

        String categorias = data.getCategorias().stream()
                .map(CategoriaRecurso::getDescripcion)
                .collect(Collectors.joining(", "));
        String hoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        return reservaExtractorService.extraer(frase, categorias, hoy);
    }

    private String cargarApiKey() throws Exception {
        Properties propiedades = new Properties();
        try (InputStream is = ReservaIAService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) {
                throw new Exception("No se encontro config.properties con la API key de Groq");
            }
            propiedades.load(is);
        }
        String key = propiedades.getProperty("groq.api.key");
        if (key == null || key.isEmpty() || key.equals("YOUR_KEY_HERE")) {
            throw new Exception("Debe configurar groq.api.key en config.properties");
        }
        return key;
    }
}
