package una.eif206.data;

import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Varios hilos intentan reservar el MISMO recurso en el MISMO horario al
 * mismo tiempo. El candado de MemoriaRepositorio.crearReserva (equivalente al
 * de ServicioReserva.crear contra Oracle) debe garantizar que la
 * comprobacion de disponibilidad y la insercion sean atomicas: exactamente
 * un hilo gana, sin importar el orden real de ejecucion.
 */
class ReservaConcurrenciaTest {

    @Test
    void soloUnHiloReservaElMismoRecursoEnElMismoHorario() throws Exception {
        MemoriaRepositorio repositorio = new MemoriaRepositorio();
        Funcionario funcionario = repositorio.buscarFuncionarioPorId("111");
        Recurso recurso = repositorio.listarRecursos().get(0);
        String fecha = "2026-09-01";

        int hilos = 20;
        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger fallosEsperados = new AtomicInteger();
        List<Exception> erroresInesperados = new CopyOnWriteArrayList<>();

        for (int i = 0; i < hilos; i++) {
            final int n = i;
            executor.submit(() -> {
                try {
                    salida.await();
                    Reserva reserva = new Reserva("TST-CONC-" + n, "Reunion " + n, fecha, "09:00", "10:00", funcionario);
                    reserva.setRecursos(List.of(recurso));
                    repositorio.crearReserva(reserva);
                    exitos.incrementAndGet();
                } catch (GlobalException esperado) {
                    fallosEsperados.incrementAndGet();
                } catch (Exception inesperado) {
                    erroresInesperados.add(inesperado);
                }
            });
        }

        salida.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertTrue(erroresInesperados.isEmpty(), "errores inesperados: " + erroresInesperados);
        assertEquals(1, exitos.get());
        assertEquals(hilos - 1, fallosEsperados.get());
        assertEquals(1, repositorio.listarIdsRecursosOcupados(fecha, "09:00", "10:00").size());
    }
}
