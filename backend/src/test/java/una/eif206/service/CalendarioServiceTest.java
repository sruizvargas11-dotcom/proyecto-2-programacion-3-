package una.eif206.service;

import una.eif206.data.MemoriaRepositorio;
import una.eif206.data.Repositorio;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import una.eif206.model.enums.EstadoReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de CalendarioService (antes sin cobertura dedicada).
 * Usa MemoriaRepositorio real a traves de los demas *Service, igual que
 * ActividadesServiceTest/EstadisticasServiceTest, para que las reservas de
 * prueba pasen por las mismas validaciones que en produccion. Las pocas
 * pruebas que necesitan un estado que la API normal no permite crear
 * (horas invalidas, dos reservas ACTIVAS solapadas en el mismo recurso)
 * insertan directamente con MemoriaRepositorio.insertarReservaDirecta(), que documenta ese
 * caso como un limite de la capa de persistencia/validacion, no de
 * CalendarioService.
 */
class CalendarioServiceTest {

    private MemoriaRepositorio repositorio;
    private CalendarioService calendarioService;
    private ReservaService reservaService;
    private FuncionarioService funcionarioService;
    private CategoriaService categoriaService;
    private RecursoService recursoService;

    private Funcionario funcionario;
    private CategoriaRecurso categoriaLaptops;
    private List<Recurso> recursosLaptops;

    @BeforeEach
    void setUp() {
        repositorio = new MemoriaRepositorio();
        calendarioService = new CalendarioService(repositorio);
        reservaService = new ReservaService(repositorio);
        funcionarioService = new FuncionarioService(repositorio);
        categoriaService = new CategoriaService(repositorio);
        recursoService = new RecursoService(repositorio);

        funcionario = funcionarioService.findById("111");
        categoriaLaptops = categoriaService.findAll().stream()
                .filter(c -> c.getId().equals("CAT-000001")).findFirst().orElseThrow();
        recursosLaptops = recursoService.findByCategoria(categoriaLaptops);
    }

    private Reserva reservar(String actividad, String fecha, String horaInicio, String horaFin, Recurso recurso) {
        Reserva r = new Reserva(reservaService.generarId(), actividad, fecha, horaInicio, horaFin, funcionario);
        r.setRecursos(List.of(recurso));
        String error = reservaService.create(r);
        assertNull(error, "la reserva de prueba deberia crearse sin error: " + error);
        return r;
    }

    private static int filaDe(String[][] matriz, String hora) {
        for (int i = 0; i < matriz.length; i++) {
            if (matriz[i][0].equals(hora)) {
                return i;
            }
        }
        throw new AssertionError("hora no encontrada en la matriz: " + hora);
    }

    // ===================== casos validos =====================

    @Test
    void celdaMuestraActividadYFuncionarioEnLaHoraCorrecta() {
        reservar("Reunion", "2026-02-10", "09:00", "10:00", recursosLaptops.get(0));

        String[][] matriz = calendarioService.getCalendario("2026-02-10", categoriaLaptops);

        assertEquals("Reunion - Juan Perez", matriz[filaDe(matriz, "09:00")][1]);
        assertNull(matriz[filaDe(matriz, "08:00")][1]);
        assertNull(matriz[filaDe(matriz, "10:00")][1]);
    }

    @Test
    void columnasCoincidenConElOrdenDeRecursosPorCategoria() {
        reservar("Uso laptop 1", "2026-02-10", "09:00", "10:00", recursosLaptops.get(0));
        reservar("Uso laptop 2", "2026-02-10", "09:00", "10:00", recursosLaptops.get(1));

        String[][] matriz = calendarioService.getCalendario("2026-02-10", categoriaLaptops);

        int fila = filaDe(matriz, "09:00");
        assertEquals(recursosLaptops.size() + 1, matriz[fila].length);
        assertEquals("Uso laptop 1 - Juan Perez", matriz[fila][1]);
        assertEquals("Uso laptop 2 - Juan Perez", matriz[fila][2]);
    }

    @Test
    void reservaQueAbarcaVariasHorasMarcaTodasLasFilasDelRango() {
        reservar("Capacitacion", "2026-02-10", "09:00", "12:00", recursosLaptops.get(0));

        String[][] matriz = calendarioService.getCalendario("2026-02-10", categoriaLaptops);

        assertEquals("Capacitacion - Juan Perez", matriz[filaDe(matriz, "09:00")][1]);
        assertEquals("Capacitacion - Juan Perez", matriz[filaDe(matriz, "10:00")][1]);
        assertEquals("Capacitacion - Juan Perez", matriz[filaDe(matriz, "11:00")][1]);
        assertNull(matriz[filaDe(matriz, "12:00")][1], "el fin de la reserva no es inclusivo");
        assertNull(matriz[filaDe(matriz, "08:00")][1]);
    }

    @Test
    void recursoSinReservasEseDiaQuedaConTodasLasCeldasNulas() {
        reservar("Uso laptop 1", "2026-02-10", "09:00", "10:00", recursosLaptops.get(0));

        String[][] matriz = calendarioService.getCalendario("2026-02-10", categoriaLaptops);

        for (String[] fila : matriz) {
            assertNull(fila[2], "la columna de la segunda laptop no debia tener reservas");
        }
    }

    @Test
    void categoriaSinRecursosDaUnaMatrizDeUnaSolaColumnaSinExcepcion() {
        CategoriaRecurso sinRecursos = categoriaService.findAll().stream()
                .filter(c -> c.getId().equals("CAT-000003")).findFirst().orElseThrow();

        String[][] matriz = calendarioService.getCalendario("2026-02-10", sinRecursos);

        assertTrue(matriz.length > 0);
        for (String[] fila : matriz) {
            assertEquals(1, fila.length);
            assertNotNull(fila[0]);
        }
    }

    @Test
    void findByCategoriaDelegaEnElMismoRepositorioQueRecursoService() {
        List<Recurso> viaCalendario = calendarioService.findByCategoria(categoriaLaptops);

        assertEquals(recursosLaptops.size(), viaCalendario.size());
        assertEquals(recursosLaptops.get(0).getId(), viaCalendario.get(0).getId());
    }

    // ===================== fechas invalidas =====================

    @Test
    void fechaSinReservasDaUnCalendarioCompletamenteVacio() {
        reservar("Reunion", "2026-02-10", "09:00", "10:00", recursosLaptops.get(0));

        String[][] matriz = calendarioService.getCalendario("2099-01-01", categoriaLaptops);

        for (String[] fila : matriz) {
            for (int col = 1; col < fila.length; col++) {
                assertNull(fila[col]);
            }
        }
    }

    @Test
    void fechaNulaNoLanzaExcepcionYDaCalendarioVacio() {
        reservar("Reunion", "2026-02-10", "09:00", "10:00", recursosLaptops.get(0));

        String[][] matriz = calendarioService.getCalendario(null, categoriaLaptops);

        for (String[] fila : matriz) {
            for (int col = 1; col < fila.length; col++) {
                assertNull(fila[col]);
            }
        }
    }

    @Test
    void fechaConFormatoInvalidoNoLanzaExcepcionYDaCalendarioVacio() {
        reservar("Reunion", "2026-02-10", "09:00", "10:00", recursosLaptops.get(0));

        String[][] matriz = calendarioService.getCalendario("no-es-una-fecha", categoriaLaptops);

        for (String[] fila : matriz) {
            for (int col = 1; col < fila.length; col++) {
                assertNull(fila[col]);
            }
        }
    }

    @Test
    void reservaUnDiaAntesOUnDiaDespuesNoApareceEnElCalendarioDelDiaConsultado() {
        reservar("Reunion antes", "2026-02-09", "09:00", "10:00", recursosLaptops.get(0));
        reservar("Reunion despues", "2026-02-11", "09:00", "10:00", recursosLaptops.get(1));

        String[][] matriz = calendarioService.getCalendario("2026-02-10", categoriaLaptops);

        for (String[] fila : matriz) {
            assertNull(fila[1]);
            assertNull(fila[2]);
        }
    }

    // ===================== solapamientos =====================

    @Test
    void dosRecursosDistintosReservadosEnElMismoHorarioSeMuestranCadaUnoEnSuColumna() {
        reservar("Uso laptop 1", "2026-02-10", "09:00", "10:00", recursosLaptops.get(0));
        reservar("Uso laptop 2", "2026-02-10", "09:00", "10:00", recursosLaptops.get(1));

        String[][] matriz = calendarioService.getCalendario("2026-02-10", categoriaLaptops);

        int fila = filaDe(matriz, "09:00");
        assertEquals("Uso laptop 1 - Juan Perez", matriz[fila][1]);
        assertEquals("Uso laptop 2 - Juan Perez", matriz[fila][2]);
    }

    @Test
    void mismoRecursoConDosReservasEnHorariosDistintosNoSeMezclanEntreFilas() {
        reservar("Reunion temprano", "2026-02-10", "09:00", "10:00", recursosLaptops.get(0));
        reservar("Reunion tarde", "2026-02-10", "15:00", "16:00", recursosLaptops.get(0));

        String[][] matriz = calendarioService.getCalendario("2026-02-10", categoriaLaptops);

        assertEquals("Reunion temprano - Juan Perez", matriz[filaDe(matriz, "09:00")][1]);
        assertEquals("Reunion tarde - Juan Perez", matriz[filaDe(matriz, "15:00")][1]);
        assertNull(matriz[filaDe(matriz, "10:00")][1]);
        assertNull(matriz[filaDe(matriz, "14:00")][1]);
    }

    /**
     * El flujo normal (ReservaService.create, bajo el candado/transaccion de
     * crearReserva) nunca deja dos reservas ACTIVAS solapadas en el mismo
     * recurso; esta prueba documenta que, si esa invariante se violara (dato
     * insertado directo en el repositorio), CalendarioService simplemente
     * pinta la ultima reserva procesada en la celda compartida, sin fusionar
     * ni avisar del conflicto. No es un bug de CalendarioService (su trabajo
     * es pintar lo que hay, no detectar conflictos de otra capa) pero queda
     * documentado como limite conocido.
     */
    @Test
    void dosReservasActivasSolapadasEnElMismoRecursoLaUltimaProcesadaGanaLaCelda() {
        // Mismo horaInicio para que ambas cubran exactamente la misma fila
        // redonda (09:00): asi la segunda, al procesarse despues en
        // repositorio.listarReservas(), pisa la celda que dejo la primera.
        Reserva primera = new Reserva("RES-SOLAPA-1", "Primera", "2026-02-10", "09:00", "10:00", funcionario);
        primera.setRecursos(List.of(recursosLaptops.get(0)));
        Reserva segunda = new Reserva("RES-SOLAPA-2", "Segunda", "2026-02-10", "09:00", "11:00", funcionario);
        segunda.setRecursos(List.of(recursosLaptops.get(0)));
        // Bypass deliberado de reservaService.create(): solo asi se puede
        // llegar a este estado, que el flujo normal bloquea.
        repositorio.insertarReservaDirecta(primera);
        repositorio.insertarReservaDirecta(segunda);

        String[][] matriz = calendarioService.getCalendario("2026-02-10", categoriaLaptops);

        assertEquals("Segunda - Juan Perez", matriz[filaDe(matriz, "09:00")][1]);
        assertEquals("Segunda - Juan Perez", matriz[filaDe(matriz, "10:00")][1]);
    }

    // ===================== limites =====================

    @Test
    void reservaEnElPrimerBloqueDeHorasSeMuestraEnLaPrimeraFila() {
        reservar("Reunion madrugada", "2026-02-10", "06:00", "07:00", recursosLaptops.get(0));

        String[][] matriz = calendarioService.getCalendario("2026-02-10", categoriaLaptops);

        assertEquals("06:00", matriz[0][0]);
        assertEquals("Reunion madrugada - Juan Perez", matriz[0][1]);
    }

    @Test
    void reservaQueTerminaDespuesDeLas22HorasSeMuestraSoloEnLaUltimaFila() {
        Reserva r = new Reserva(reservaService.generarId(), "Reunion larga", "2026-02-10", "21:30", "23:00", funcionario);
        r.setRecursos(List.of(recursosLaptops.get(0)));
        assertNull(reservaService.create(r));

        String[][] matriz = calendarioService.getCalendario("2026-02-10", categoriaLaptops);

        assertEquals("22:00", matriz[matriz.length - 1][0]);
        assertEquals("Reunion larga - Juan Perez", matriz[matriz.length - 1][1]);
        assertNull(matriz[filaDe(matriz, "21:00")][1], "21:30 todavia no alcanza el bloque de las 21:00");
    }

    @Test
    void reservaFueraDelHorarioComercialNoApareceEnNingunaFila() {
        // 23:00-23:30 es una reserva valida (ReservaService no limita horario
        // comercial), pero cae fuera del rango 06:00-22:00 que pinta el calendario.
        reservar("Reunion nocturna", "2026-02-10", "23:00", "23:30", recursosLaptops.get(0));

        String[][] matriz = calendarioService.getCalendario("2026-02-10", categoriaLaptops);

        for (String[] fila : matriz) {
            assertNull(fila[1]);
        }
    }

    @Test
    void reservaCanceladaNoApareceEnElCalendario() {
        Reserva r = reservar("Reunion cancelada", "2026-02-10", "09:00", "10:00", recursosLaptops.get(0));
        r.setEstado(EstadoReserva.CANCELADA);

        String[][] matriz = calendarioService.getCalendario("2026-02-10", categoriaLaptops);

        assertNull(matriz[filaDe(matriz, "09:00")][1]);
    }

    @Test
    void reservaCompletadaNoApareceEnElCalendario() {
        // COMPLETADA esta en el modelo/BD pero ningun flujo actual la asigna;
        // se fuerza aqui solo para confirmar que getCalendario() tambien la excluye.
        Reserva r = reservar("Reunion completada", "2026-02-10", "09:00", "10:00", recursosLaptops.get(0));
        r.setEstado(EstadoReserva.COMPLETADA);

        String[][] matriz = calendarioService.getCalendario("2026-02-10", categoriaLaptops);

        assertNull(matriz[filaDe(matriz, "09:00")][1]);
    }

    @Test
    void horaMalformadaEnLaReservaNoRompeElCalendario() {
        // horarioValido() de ReservaService ya rechaza esto al crear, asi que
        // ambas reservas se insertan directo en el repositorio: la idea es
        // ejercitar SOLO a CalendarioService.horaEnRango() (que si atrapa el
        // error de parseo) con el dato corrupto ya instalado, sin pasar por
        // reservaService.create()/MemoriaRepositorio.horariosSolapan() (que
        // no tiene ese mismo try/catch; ver nota al final de la prueba).
        Reserva horaInvalida = new Reserva("RES-HORA-MALA", "Reunion", "2026-02-10", "25:99", "26:00", funcionario);
        horaInvalida.setRecursos(List.of(recursosLaptops.get(0)));
        Reserva valida = new Reserva("RES-HORA-OK", "Reunion valida", "2026-02-10", "09:00", "10:00", funcionario);
        valida.setRecursos(List.of(recursosLaptops.get(1)));
        repositorio.insertarReservaDirecta(horaInvalida);
        repositorio.insertarReservaDirecta(valida);

        String[][] matriz = calendarioService.getCalendario("2026-02-10", categoriaLaptops);

        for (String[] fila : matriz) {
            assertNull(fila[1]);
        }
        assertEquals("Reunion valida - Juan Perez", matriz[filaDe(matriz, "09:00")][2]);
    }
}
