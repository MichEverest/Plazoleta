package com.mich.plazoleta.application.usecase;

import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ProhibidoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.NoEncontradoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ValidacionException;
import com.mich.plazoleta.domain.model.Categoria;
import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Plato;
import com.mich.plazoleta.domain.model.Restaurante;
import com.mich.plazoleta.application.port.output.PlatoRepositoryPort;
import com.mich.plazoleta.application.port.output.RestauranteRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatoUseCaseTest {

    private static final Long ID_PROPIETARIO = 2L;
    private static final Long OTRO_PROPIETARIO = 9L;

    @Mock
    private PlatoRepositoryPort platoRepositoryPort;

    @Mock
    private RestauranteRepositoryPort restauranteRepositoryPort;

    @InjectMocks
    private PlatoUseCase platoUseCase;

    @Test
    void crearPlatoQuedaActivo() {
        Plato plato = platoValido();
        when(platoRepositoryPort.existeCategoria(1L)).thenReturn(true);
        when(restauranteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(restaurante()));
        when(platoRepositoryPort.guardar(plato)).thenReturn(plato);

        assertTrue(platoUseCase.crear(plato, ID_PROPIETARIO).getActivo());
    }

    @Test
    void crearPlatoFallaSiPrecioEsCero() {
        Plato plato = platoValido();
        plato.setPrecio(0);

        assertThrows(ValidacionException.class, () -> platoUseCase.crear(plato, ID_PROPIETARIO));
        verify(platoRepositoryPort, never()).guardar(any());
    }

    @Test
    void crearPlatoFallaSiFaltaNombre() {
        Plato plato = platoValido();
        plato.setNombre(null);

        assertThrows(ValidacionException.class, () -> platoUseCase.crear(plato, ID_PROPIETARIO));
    }

    @Test
    void crearPlatoFallaSiCategoriaNoExiste() {
        Plato plato = platoValido();
        when(platoRepositoryPort.existeCategoria(1L)).thenReturn(false);

        assertThrows(NoEncontradoException.class, () -> platoUseCase.crear(plato, ID_PROPIETARIO));
    }

    @Test
    void crearPlatoFallaSiRestauranteNoExiste() {
        Plato plato = platoValido();
        when(platoRepositoryPort.existeCategoria(1L)).thenReturn(true);
        when(restauranteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> platoUseCase.crear(plato, ID_PROPIETARIO));
    }

    @Test
    void crearPlatoFallaSiRestauranteEsDeOtroPropietario() {
        Plato plato = platoValido();
        when(platoRepositoryPort.existeCategoria(1L)).thenReturn(true);
        when(restauranteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(restaurante()));

        assertThrows(ProhibidoException.class, () -> platoUseCase.crear(plato, OTRO_PROPIETARIO));
        verify(platoRepositoryPort, never()).guardar(any());
    }

    @Test
    void actualizarPlatoCambiaSoloPrecioYDescripcion() {
        Plato plato = platoValido();
        when(platoRepositoryPort.buscarPorId(5L)).thenReturn(Optional.of(plato));
        when(restauranteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(restaurante()));
        when(platoRepositoryPort.guardar(plato)).thenReturn(plato);

        Plato actualizado = platoUseCase.actualizar(5L, 30000, "Nueva descripción", ID_PROPIETARIO);

        assertEquals(30000, actualizado.getPrecio());
        assertEquals("Nueva descripción", actualizado.getDescripcion());
        assertEquals("Bandeja paisa", actualizado.getNombre());
    }

    @Test
    void actualizarPlatoFallaSiPrecioEsNegativo() {
        assertThrows(ValidacionException.class, () -> platoUseCase.actualizar(5L, -1, "Desc", ID_PROPIETARIO));
    }

    @Test
    void actualizarPlatoFallaSiPlatoNoExiste() {
        when(platoRepositoryPort.buscarPorId(5L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> platoUseCase.actualizar(5L, 1000, "Desc", ID_PROPIETARIO));
    }

    @Test
    void actualizarPlatoFallaSiEsDeOtroPropietario() {
        when(platoRepositoryPort.buscarPorId(5L)).thenReturn(Optional.of(platoValido()));
        when(restauranteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(restaurante()));

        assertThrows(ProhibidoException.class, () -> platoUseCase.actualizar(5L, 1000, "Desc", OTRO_PROPIETARIO));
    }

    @Test
    void cambiarEstadoPlatoLoDeshabilita() {
        Plato plato = platoValido();
        when(platoRepositoryPort.buscarPorId(5L)).thenReturn(Optional.of(plato));
        when(restauranteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(restaurante()));
        when(platoRepositoryPort.guardar(plato)).thenReturn(plato);

        assertFalse(platoUseCase.cambiarEstado(5L, false, ID_PROPIETARIO).getActivo());
    }

    @Test
    void cambiarEstadoPlatoFallaSiActivoEsNulo() {
        assertThrows(ValidacionException.class, () -> platoUseCase.cambiarEstado(5L, null, ID_PROPIETARIO));
    }

    @Test
    void cambiarEstadoPlatoFallaSiEsDeOtroPropietario() {
        when(platoRepositoryPort.buscarPorId(5L)).thenReturn(Optional.of(platoValido()));
        when(restauranteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(restaurante()));

        assertThrows(ProhibidoException.class, () -> platoUseCase.cambiarEstado(5L, false, OTRO_PROPIETARIO));
    }

    @Test
    void listarPlatosParaClienteSoloTraeActivos() {
        Pagina.Solicitud paginacion = new Pagina.Solicitud(0, 10);
        Pagina<Plato> pagina = new Pagina<>(List.of(platoValido()), 0, 10, 1, 1);
        when(restauranteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(restaurante()));
        when(platoRepositoryPort.listarPorRestaurante(1L, 3L, true, paginacion)).thenReturn(pagina);

        assertEquals(pagina, platoUseCase.listarPorRestaurante(1L, 3L, paginacion, 3L, "CLIENTE"));
    }

    @Test
    void listarPlatosParaPropietarioDuenoTraeTodos() {
        Pagina.Solicitud paginacion = new Pagina.Solicitud(0, 10);
        Pagina<Plato> pagina = new Pagina<>(List.of(platoValido()), 0, 10, 1, 1);
        when(restauranteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(restaurante()));
        when(platoRepositoryPort.listarPorRestaurante(1L, null, false, paginacion)).thenReturn(pagina);

        assertEquals(pagina, platoUseCase.listarPorRestaurante(1L, null, paginacion, ID_PROPIETARIO, "PROPIETARIO"));
    }

    @Test
    void listarPlatosFallaSiPropietarioNoEsDueno() {
        when(restauranteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.of(restaurante()));

        assertThrows(ProhibidoException.class,
                () -> platoUseCase.listarPorRestaurante(1L, null, new Pagina.Solicitud(0, 10), OTRO_PROPIETARIO, "PROPIETARIO"));
    }

    @Test
    void listarPlatosFallaSiRestauranteNoExiste() {
        when(restauranteRepositoryPort.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> platoUseCase.listarPorRestaurante(1L, null, new Pagina.Solicitud(0, 10), 3L, "CLIENTE"));
    }

    @Test
    void listarCategoriasRetornaLasCategoriasDelRepositorio() {
        List<Categoria> categorias = List.of(new Categoria(1L, "ENTRADAS", "Entradas"));
        when(platoRepositoryPort.listarCategorias()).thenReturn(categorias);

        assertEquals(categorias, platoUseCase.listarCategorias());
    }

    private Plato platoValido() {
        Plato plato = new Plato();
        plato.setNombre("Bandeja paisa");
        plato.setPrecio(25000);
        plato.setDescripcion("Plato típico");
        plato.setUrlImagen("https://imagen.png");
        plato.setIdCategoria(1L);
        plato.setIdRestaurante(1L);
        plato.setActivo(true);
        return plato;
    }

    private Restaurante restaurante() {
        return new Restaurante(1L, "Sabores", "900123456", "Calle 10", "+573001234567", "logo", ID_PROPIETARIO);
    }
}
