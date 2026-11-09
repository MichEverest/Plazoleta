package com.mich.trazabilidad.application.usecase;

import com.mich.trazabilidad.application.port.output.PlazoletaPort;
import com.mich.trazabilidad.application.port.output.TrazabilidadRepositoryPort;
import com.mich.trazabilidad.domain.exception.ExcepcionesDominio.NoEncontradoException;
import com.mich.trazabilidad.domain.exception.ExcepcionesDominio.ProhibidoException;
import com.mich.trazabilidad.domain.exception.ExcepcionesDominio.ValidacionException;
import com.mich.trazabilidad.domain.model.Eficiencia;
import com.mich.trazabilidad.domain.model.Pagina;
import com.mich.trazabilidad.domain.model.Trazabilidad;
import com.mich.trazabilidad.domain.model.Trazabilidad.Estado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrazabilidadUseCaseTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final Long ID_RESTAURANTE = 1L;

    @Mock
    private TrazabilidadRepositoryPort trazabilidadRepositoryPort;

    @Mock
    private PlazoletaPort plazoletaPort;

    @InjectMocks
    private TrazabilidadUseCase trazabilidadUseCase;

    @Test
    void registrarAsignaFechaYGuarda() {
        Trazabilidad trazabilidad = registro(20L, null, Estado.PENDIENTE, 0, null);
        trazabilidad.setFecha(null);

        trazabilidadUseCase.registrar(trazabilidad);

        assertNotNull(trazabilidad.getFecha());
        verify(trazabilidadRepositoryPort).guardar(trazabilidad);
    }

    @Test
    void registrarFallaSinEstadoNuevo() {
        Trazabilidad trazabilidad = registro(20L, null, null, 0, null);

        assertThrows(ValidacionException.class, () -> trazabilidadUseCase.registrar(trazabilidad));
        verify(trazabilidadRepositoryPort, never()).guardar(any());
    }

    @Test
    void registrarFallaSinCorreoCliente() {
        Trazabilidad trazabilidad = registro(20L, null, Estado.PENDIENTE, 0, null);
        trazabilidad.setCorreoCliente(" ");

        assertThrows(ValidacionException.class, () -> trazabilidadUseCase.registrar(trazabilidad));
    }

    @Test
    void consultarHistorialCalculaDuracionDeCadaEstado() {
        when(trazabilidadRepositoryPort.listarPorPedido(20L)).thenReturn(List.of(
                registro(20L, null, Estado.PENDIENTE, 0, null),
                registro(20L, Estado.PENDIENTE, Estado.EN_PREPARACION, 5, 7L),
                registro(20L, Estado.EN_PREPARACION, Estado.CANCELADO, 15, null)));

        List<Trazabilidad.Historial> historial = trazabilidadUseCase.consultarHistorial(20L, 3L);

        assertEquals(300L, historial.get(0).duracionSegundos());
        assertEquals(600L, historial.get(1).duracionSegundos());
        assertNull(historial.get(2).duracionSegundos());
    }

    @Test
    void consultarHistorialFallaSiNoHayRegistros() {
        when(trazabilidadRepositoryPort.listarPorPedido(20L)).thenReturn(List.of());

        assertThrows(NoEncontradoException.class, () -> trazabilidadUseCase.consultarHistorial(20L, 3L));
    }

    @Test
    void consultarHistorialFallaSiPedidoEsDeOtroCliente() {
        when(trazabilidadRepositoryPort.listarPorPedido(20L))
                .thenReturn(List.of(registro(20L, null, Estado.PENDIENTE, 0, null)));

        assertThrows(ProhibidoException.class, () -> trazabilidadUseCase.consultarHistorial(20L, 99L));
    }

    @Test
    void eficienciaPedidosCalculaPromedioYMarcaLosQueLoSuperan() {
        when(plazoletaPort.obtenerIdRestauranteDelPropietario()).thenReturn(ID_RESTAURANTE);
        when(trazabilidadRepositoryPort.listarPorRestaurante(ID_RESTAURANTE)).thenReturn(List.of(
                registro(10L, null, Estado.PENDIENTE, 0, null),
                registro(10L, Estado.LISTO, Estado.ENTREGADO, 10, 7L),
                registro(11L, null, Estado.PENDIENTE, 0, null),
                registro(11L, Estado.LISTO, Estado.ENTREGADO, 30, 7L),
                registro(12L, null, Estado.PENDIENTE, 0, null)));

        Eficiencia.Reporte reporte = trazabilidadUseCase.calcularEficienciaPedidos(new Pagina.Solicitud(0, 10));

        assertEquals(1200.0, reporte.promedioSegundos());
        assertEquals(2, reporte.pedidos().totalElementos());
        assertFalse(reporte.pedidos().contenido().get(0).superaPromedio());
        assertTrue(reporte.pedidos().contenido().get(1).superaPromedio());
    }

    @Test
    void eficienciaPedidosSinEntregadosRetornaPromedioCero() {
        when(plazoletaPort.obtenerIdRestauranteDelPropietario()).thenReturn(ID_RESTAURANTE);
        when(trazabilidadRepositoryPort.listarPorRestaurante(ID_RESTAURANTE))
                .thenReturn(List.of(registro(10L, null, Estado.PENDIENTE, 0, null)));

        Eficiencia.Reporte reporte = trazabilidadUseCase.calcularEficienciaPedidos(new Pagina.Solicitud(0, 10));

        assertEquals(0.0, reporte.promedioSegundos());
        assertEquals(0, reporte.pedidos().totalElementos());
    }

    @Test
    void rankingEmpleadosOrdenaDelMasRapidoAlMasLento() {
        when(plazoletaPort.obtenerIdRestauranteDelPropietario()).thenReturn(ID_RESTAURANTE);
        when(trazabilidadRepositoryPort.listarPorRestaurante(ID_RESTAURANTE)).thenReturn(List.of(
                registro(10L, Estado.PENDIENTE, Estado.EN_PREPARACION, 0, 7L),
                registro(10L, Estado.EN_PREPARACION, Estado.LISTO, 20, 7L),
                registro(10L, Estado.LISTO, Estado.ENTREGADO, 25, 7L),
                registro(11L, Estado.PENDIENTE, Estado.EN_PREPARACION, 0, 8L),
                registro(11L, Estado.EN_PREPARACION, Estado.LISTO, 5, 8L),
                registro(11L, Estado.LISTO, Estado.ENTREGADO, 6, 8L)));

        Pagina<Eficiencia.Empleado> ranking = trazabilidadUseCase.calcularRankingEmpleados(new Pagina.Solicitud(0, 10));

        assertEquals(8L, ranking.contenido().get(0).idEmpleado());
        assertEquals(1, ranking.contenido().get(0).posicion());
        assertEquals(7L, ranking.contenido().get(1).idEmpleado());
    }

    @Test
    void rankingEmpleadosIgnoraPedidosNoEntregados() {
        when(plazoletaPort.obtenerIdRestauranteDelPropietario()).thenReturn(ID_RESTAURANTE);
        when(trazabilidadRepositoryPort.listarPorRestaurante(ID_RESTAURANTE)).thenReturn(List.of(
                registro(10L, Estado.PENDIENTE, Estado.EN_PREPARACION, 0, 7L),
                registro(10L, Estado.EN_PREPARACION, Estado.LISTO, 20, 7L)));

        assertEquals(0, trazabilidadUseCase.calcularRankingEmpleados(new Pagina.Solicitud(0, 10)).totalElementos());
    }

    @Test
    void paginarRetornaLaPaginaSolicitada() {
        Pagina<Integer> pagina = new Pagina.Solicitud(1, 2).paginar(List.of(1, 2, 3, 4, 5));

        assertEquals(List.of(3, 4), pagina.contenido());
        assertEquals(3, pagina.totalPaginas());
    }

    @Test
    void paginarFueraDeRangoRetornaVacio() {
        assertEquals(List.of(), new Pagina.Solicitud(5, 2).paginar(List.of(1, 2)).contenido());
    }

    @Test
    void solicitudDePaginaFallaConValoresInvalidos() {
        assertThrows(ValidacionException.class, () -> new Pagina.Solicitud(-1, 10));
        assertThrows(ValidacionException.class, () -> new Pagina.Solicitud(0, 101));
    }

    private Trazabilidad registro(Long idPedido, Estado anterior, Estado nuevo, int minutos, Long idEmpleado) {
        return new Trazabilidad(null, idPedido, 3L, "jean@plazoleta.com", ID_RESTAURANTE, INICIO.plusMinutes(minutos),
                anterior, nuevo, idEmpleado, idEmpleado == null ? null : "empleado" + idEmpleado + "@plazoleta.com");
    }
}
