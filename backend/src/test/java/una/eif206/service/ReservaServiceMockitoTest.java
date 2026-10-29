package una.eif206.service;

import una.eif206.data.GlobalException;
import una.eif206.data.Repositorio;
import una.eif206.model.CategoriaRecurso;
import una.eif206.model.Funcionario;
import una.eif206.model.Recurso;
import una.eif206.model.Reserva;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba ReservaService.create(Reserva) con un Repositorio simulado
 * (Mockito): verifica el CONTRATO de la nueva resolucion server-side (que
 * ReservaService delega integra en repositorio.crearReserva) sin tocar
 * MemoriaRepositorio/Oracle. La resolucion en si (generar id, elegir recurso
 * libre por categoria, todo bajo un solo candado/transaccion) se prueba en
 * ServicioReservaTest (Oracle simulado) y en ReservaConcurrenciaPorCategoriaTest
 * (MemoriaRepositorio, 20 hilos).
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceMockitoTest {

    private static final Funcionario JUAN = new Funcionario("111", "Juan Perez", "TI", "3323");
    private static final CategoriaRecurso CAT_LAPTOPS = new CategoriaRecurso("CAT-000001", "Laptop windows");

    private static Reserva reservaConRecurso(String id, Recurso recurso) {
        Reserva r = new Reserva(id, "Reunion", "2031-01-01", "09:00", "10:00", JUAN);
        r.setRecursos(List.of(recurso));
        return r;
    }

    @Test
    void conIdVacioNoConsultaExistenciaYDelegaDirectoEnCrearReserva() throws Exception {
        Repositorio repositorio = mock(Repositorio.class);
        Reserva e = reservaConRecurso("", new Recurso("238715", "Laptop #238715", CAT_LAPTOPS));

        String error = new ReservaService(repositorio).create(e);

        assertNull(error);
        verify(repositorio, never()).buscarReservaPorId(any());
        verify(repositorio).crearReserva(e);
    }

    @Test
    void conIdExplicitoConsultaExistenciaAntesDeCrear() throws Exception {
        Repositorio repositorio = mock(Repositorio.class);
        when(repositorio.buscarReservaPorId("RES-1")).thenReturn(null);
        Reserva e = reservaConRecurso("RES-1", new Recurso("238715", "Laptop #238715", CAT_LAPTOPS));

        String error = new ReservaService(repositorio).create(e);

        assertNull(error);
        verify(repositorio).buscarReservaPorId("RES-1");
        verify(repositorio).crearReserva(e);
    }

    @Test
    void conIdExplicitoYaExistenteNoLlamaACrearReserva() throws Exception {
        Repositorio repositorio = mock(Repositorio.class);
        when(repositorio.buscarReservaPorId("RES-1")).thenReturn(reservaConRecurso("RES-1",
                new Recurso("238715", "Laptop #238715", CAT_LAPTOPS)));
        Reserva e = reservaConRecurso("RES-1", new Recurso("238715", "Laptop #238715", CAT_LAPTOPS));

        String error = new ReservaService(repositorio).create(e);

        assertEquals("Reserva ya existe", error);
        verify(repositorio, never()).crearReserva(any());
    }

    @Test
    void recursoSinIdNiCategoriaFallaAntesDeTocarElRepositorio() throws Exception {
        Repositorio repositorio = mock(Repositorio.class);
        Reserva e = reservaConRecurso(null, new Recurso(null, null, null));

        String error = new ReservaService(repositorio).create(e);

        assertNotNull(error);
        verify(repositorio, never()).buscarReservaPorId(any());
        verify(repositorio, never()).crearReserva(any());
    }

    @Test
    void recursoSoloPorCategoriaSePasaIntactoAlRepositorioQueLoResuelve() throws Exception {
        Repositorio repositorio = mock(Repositorio.class);
        Recurso soloCategoria = new Recurso(null, null, CAT_LAPTOPS);
        Reserva e = reservaConRecurso(null, soloCategoria);

        String error = new ReservaService(repositorio).create(e);

        assertNull(error);
        verify(repositorio).crearReserva(eq(e));
        // ReservaService no resuelve nada por su cuenta: el placeholder solo-categoria
        // llega intacto a crearReserva, que es quien lo reemplaza por un recurso real.
        assertEquals(CAT_LAPTOPS, e.getRecursos().get(0).getCategoria());
    }

    @Test
    void propagaElMensajeDeNegocioQueLanzaElRepositorio() throws Exception {
        Repositorio repositorio = mock(Repositorio.class);
        Reserva e = reservaConRecurso(null, new Recurso(null, null, CAT_LAPTOPS));
        org.mockito.Mockito.doThrow(new GlobalException("No hay recursos disponibles para: Laptop windows"))
                .when(repositorio).crearReserva(e);

        String error = new ReservaService(repositorio).create(e);

        assertEquals("No hay recursos disponibles para: Laptop windows", error);
    }
}
