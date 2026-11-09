package com.mich.plazoleta.application.usecase;

import com.mich.plazoleta.application.port.output.RestauranteRepositoryPort;
import com.mich.plazoleta.application.port.output.ServiciosExternosPort;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ConflictoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.NoEncontradoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ValidacionException;
import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Restaurante;
import com.mich.plazoleta.domain.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestauranteUseCaseTest {

    private static final Long ID_PROPIETARIO = 2L;

    @Mock
    private RestauranteRepositoryPort restauranteRepositoryPort;

    @Mock
    private ServiciosExternosPort serviciosExternosPort;

    @InjectMocks
    private RestauranteUseCase restauranteUseCase;

    @Test
    void crearGuardaSiPropietarioEsValido() {
        Restaurante restaurante = restaurante("Sabores de Cartheyia", "900123456", "+573001234567");
        when(serviciosExternosPort.buscarUsuario(ID_PROPIETARIO)).thenReturn(Optional.of(usuario(Usuario.Rol.PROPIETARIO)));
        when(restauranteRepositoryPort.guardar(restaurante)).thenReturn(restaurante);

        assertEquals(restaurante, restauranteUseCase.crear(restaurante));
    }

    @Test
    void crearPermiteNombreConNumeros() {
        Restaurante restaurante = restaurante("Pizza 24", "900123456", "+573001234567");
        when(serviciosExternosPort.buscarUsuario(ID_PROPIETARIO)).thenReturn(Optional.of(usuario(Usuario.Rol.PROPIETARIO)));
        when(restauranteRepositoryPort.guardar(restaurante)).thenReturn(restaurante);

        assertEquals(restaurante, restauranteUseCase.crear(restaurante));
    }

    @Test
    void crearFallaSiFaltaDireccion() {
        Restaurante restaurante = new Restaurante(null, "Sabores", "900123456", "", "+573001234567", "logo", ID_PROPIETARIO);

        assertThrows(ValidacionException.class, () -> restauranteUseCase.crear(restaurante));
        verify(restauranteRepositoryPort, never()).guardar(any());
    }

    @Test
    void crearFallaSiNombreEsSoloNumeros() {
        assertThrows(ValidacionException.class,
                () -> restauranteUseCase.crear(restaurante("12345", "900123456", "+573001234567")));
    }

    @Test
    void crearFallaSiNitNoEsNumerico() {
        assertThrows(ValidacionException.class,
                () -> restauranteUseCase.crear(restaurante("Sabores", "90012-3", "+573001234567")));
    }

    @Test
    void crearFallaSiTelefonoEsInvalido() {
        assertThrows(ValidacionException.class,
                () -> restauranteUseCase.crear(restaurante("Sabores", "900123456", "+5730000000011")));
    }

    @Test
    void crearFallaSiNitYaExiste() {
        when(restauranteRepositoryPort.existeNit("900123456")).thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> restauranteUseCase.crear(restaurante("Sabores", "900123456", "+573001234567")));
    }

    @Test
    void crearFallaSiPropietarioNoExiste() {
        when(serviciosExternosPort.buscarUsuario(ID_PROPIETARIO)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> restauranteUseCase.crear(restaurante("Sabores", "900123456", "+573001234567")));
    }

    @Test
    void crearFallaSiUsuarioNoEsPropietario() {
        when(serviciosExternosPort.buscarUsuario(ID_PROPIETARIO)).thenReturn(Optional.of(usuario(Usuario.Rol.CLIENTE)));

        assertThrows(ValidacionException.class,
                () -> restauranteUseCase.crear(restaurante("Sabores", "900123456", "+573001234567")));
        verify(restauranteRepositoryPort, never()).guardar(any());
    }

    @Test
    void crearFallaSiPropietarioYaTieneRestaurante() {
        when(serviciosExternosPort.buscarUsuario(ID_PROPIETARIO)).thenReturn(Optional.of(usuario(Usuario.Rol.PROPIETARIO)));
        when(restauranteRepositoryPort.existeDePropietario(ID_PROPIETARIO)).thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> restauranteUseCase.crear(restaurante("Sabores", "900123456", "+573001234567")));
    }

    @Test
    void consultarDePropietarioRetornaSuRestaurante() {
        Restaurante restaurante = restaurante("Sabores", "900123456", "+573001234567");
        when(restauranteRepositoryPort.buscarPorPropietario(ID_PROPIETARIO)).thenReturn(Optional.of(restaurante));

        assertEquals(restaurante, restauranteUseCase.consultarDePropietario(ID_PROPIETARIO));
    }

    @Test
    void consultarDePropietarioFallaSiNoTiene() {
        when(restauranteRepositoryPort.buscarPorPropietario(ID_PROPIETARIO)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> restauranteUseCase.consultarDePropietario(ID_PROPIETARIO));
    }

    @Test
    void listarDelegaEnElRepositorio() {
        Pagina.Solicitud solicitud = new Pagina.Solicitud(0, 10);
        Pagina<Restaurante> pagina = new Pagina<>(List.of(), 0, 10, 0, 0);
        when(restauranteRepositoryPort.listar(solicitud)).thenReturn(pagina);

        assertEquals(pagina, restauranteUseCase.listar(solicitud));
    }

    @Test
    void crearEmpleadoLoVinculaAlRestauranteDelPropietario() {
        Usuario empleado = usuario(Usuario.Rol.EMPLEADO);
        when(restauranteRepositoryPort.buscarPorPropietario(ID_PROPIETARIO))
                .thenReturn(Optional.of(new Restaurante(1L, "Sabores", "1", "x", "1", "logo", ID_PROPIETARIO)));
        when(serviciosExternosPort.crearEmpleado(empleado)).thenReturn(7L);

        assertEquals(7L, restauranteUseCase.crearEmpleado(empleado, ID_PROPIETARIO));
        verify(restauranteRepositoryPort).vincularEmpleado(1L, 7L);
    }

    @Test
    void crearEmpleadoFallaSiPropietarioNoTieneRestaurante() {
        when(restauranteRepositoryPort.buscarPorPropietario(ID_PROPIETARIO)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> restauranteUseCase.crearEmpleado(usuario(Usuario.Rol.EMPLEADO), ID_PROPIETARIO));
        verify(serviciosExternosPort, never()).crearEmpleado(any());
    }

    @Test
    void solicitudDePaginaFallaSiTamanoEsCero() {
        assertThrows(ValidacionException.class, () -> new Pagina.Solicitud(0, 0));
    }

    private Restaurante restaurante(String nombre, String nit, String telefono) {
        return new Restaurante(null, nombre, nit, "Calle 10 # 20-30", telefono, "https://logo.png", ID_PROPIETARIO);
    }

    private Usuario usuario(Usuario.Rol rol) {
        return new Usuario(ID_PROPIETARIO, "Cartheyia", "Plazoleta", "1000000002", "+573000000002",
                "cartheyia@plazoleta.com", "Clave123*", rol);
    }
}
