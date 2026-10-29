package una.eif206.data;

import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Equivalente a ReservaConcurrenciaTest pero para la resolucion server-side
 * de recursos "solo por categoria" (id vacio + categoria presente): 20 hilos
 * piden la MISMA categoria y el MISMO horario, con 5 recursos disponibles en
 * esa categoria. Cada hilo debe recibir uno DISTINTO (sin repetidos) hasta
 * agotarse; los que llegan tarde (15 de 20) deben recibir el error de
 * negocio, sin que se cree nada para ellos. El candado de
 * MemoriaRepositorio.crearReserva (generar id + elegir recurso por categoria
 * + insertar, todo junto) es lo que garantiza esto.
 */
class ReservaConcurrenciaPorCategoriaTest {

    private static final CategoriaRecurso CATEGORIA_TEST = new CategoriaRecurso("CAT-TEST-CONC", "Categoria concurrencia");

    @Test
    void veinteHilosPidenLaMismaCategoriaYHorarioCadaUnoRecibeUnRecursoDistinto() throws Exception {
        MemoriaRepositorio repositorio = new MemoriaRepositorio();
        repositorio.insertarCategoriaDirecta(CATEGORIA_TEST);
        int recursosDisponibles = 5;
        for (int i = 1; i <= recursosDisponibles; i++) {
            repositorio.insertarRecursoDirecto(new Recurso("REC-TEST-" + i, "Recurso de prueba " + i, CATEGORIA_TEST));
        }
        Funcionario funcionario = repositorio.buscarFuncionarioPorId("111");
        String fecha = "2026-09-01";

        int hilos = 20;
        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger fallosEsperados = new AtomicInteger();
        Set<String> erroresInesperados = new CopyOnWriteArraySet<>();
        Set<String> recursosAsignados = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < hilos; i++) {
            final int n = i;
            executor.submit(() -> {
                try {
                    salida.await();
                    Recurso porCategoria = new Recurso(null, null, CATEGORIA_TEST);
                    Reserva reserva = new Reserva(null, "Reunion " + n, fecha, "09:00", "10:00", funcionario);
                    reserva.setRecursos(List.of(porCategoria));
                    repositorio.crearReserva(reserva);
                    exitos.incrementAndGet();
                    boolean nuevo = recursosAsignados.add(reserva.getRecursos().get(0).getId());
                    if (!nuevo) {
                        erroresInesperados.add("recurso repetido: " + reserva.getRecursos().get(0).getId());
                    }
                } catch (GlobalException esperado) {
                    fallosEsperados.incrementAndGet();
                } catch (Exception inesperado) {
                    erroresInesperados.add(inesperado.toString());
                }
            });
        }

        salida.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertTrue(erroresInesperados.isEmpty(), "errores inesperados: " + erroresInesperados);
        assertEquals(recursosDisponibles, exitos.get());
        assertEquals(hilos - recursosDisponibles, fallosEsperados.get());
        assertEquals(recursosDisponibles, recursosAsignados.size());
        for (int i = 1; i <= recursosDisponibles; i++) {
            assertTrue(recursosAsignados.contains("REC-TEST-" + i));
        }
    }
}
