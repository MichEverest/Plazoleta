package com.mich.plazoleta.application.usecase;

import com.mich.plazoleta.application.port.output.RestauranteRepositoryPort;
import com.mich.plazoleta.application.port.output.ServiciosExternosPort;
import com.mich.plazoleta.application.service.RestauranteService;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ConflictoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.NoEncontradoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ValidacionException;
import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Restaurante;
import com.mich.plazoleta.domain.model.Usuario;

public class RestauranteUseCase implements RestauranteService {

    private static final String MENSAJE_NOMBRE_SOLO_NUMEROS = "El nombre del restaurante no puede contener solo números";
    private static final String MENSAJE_NIT_INVALIDO = "El NIT debe ser numérico";
    private static final String MENSAJE_TELEFONO_INVALIDO = "El teléfono debe ser numérico, de máximo 13 caracteres y puede iniciar con +";
    private static final String MENSAJE_NIT_EXISTENTE = "Ya existe un restaurante con ese NIT";
    private static final String MENSAJE_PROPIETARIO_NO_EXISTE = "No existe el usuario propietario con id %d";
    private static final String MENSAJE_NO_ES_PROPIETARIO = "El usuario con id %d no tiene rol PROPIETARIO";
    private static final String MENSAJE_PROPIETARIO_CON_RESTAURANTE = "El propietario ya tiene un restaurante registrado";
    private static final String MENSAJE_SIN_RESTAURANTE = "El propietario no tiene un restaurante registrado";

    private final RestauranteRepositoryPort restauranteRepositoryPort;
    private final ServiciosExternosPort serviciosExternosPort;

    public RestauranteUseCase(RestauranteRepositoryPort restauranteRepositoryPort,
                              ServiciosExternosPort serviciosExternosPort) {
        this.restauranteRepositoryPort = restauranteRepositoryPort;
        this.serviciosExternosPort = serviciosExternosPort;
    }

    @Override
    public Restaurante crear(Restaurante restaurante) {
        validarDatos(restaurante);
        if (restauranteRepositoryPort.existeNit(restaurante.nit())) {
            throw new ConflictoException(MENSAJE_NIT_EXISTENTE);
        }
        validarPropietario(restaurante.idPropietario());
        return restauranteRepositoryPort.guardar(restaurante);
    }

    @Override
    public Restaurante consultarDePropietario(Long idPropietario) {
        return restauranteRepositoryPort.buscarPorPropietario(idPropietario)
                .orElseThrow(() -> new NoEncontradoException(MENSAJE_SIN_RESTAURANTE));
    }

    @Override
    public Pagina<Restaurante> listar(Pagina.Solicitud solicitud) {
        return restauranteRepositoryPort.listar(solicitud);
    }

    @Override
    public Long crearEmpleado(Usuario empleado, Long idPropietario) {
        Restaurante restaurante = consultarDePropietario(idPropietario);
        Long idEmpleado = serviciosExternosPort.crearEmpleado(empleado);
        restauranteRepositoryPort.vincularEmpleado(restaurante.id(), idEmpleado);
        return idEmpleado;
    }

    private void validarDatos(Restaurante restaurante) {
        Validaciones.requerido(restaurante.nombre(), "nombre");
        Validaciones.requerido(restaurante.nit(), "nit");
        Validaciones.requerido(restaurante.direccion(), "direccion");
        Validaciones.requerido(restaurante.telefono(), "telefono");
        Validaciones.requerido(restaurante.urlLogo(), "urlLogo");
        Validaciones.requerido(restaurante.idPropietario(), "idPropietario");
        if (Validaciones.esNumerico(restaurante.nombre())) {
            throw new ValidacionException(MENSAJE_NOMBRE_SOLO_NUMEROS);
        }
        if (!Validaciones.esNumerico(restaurante.nit())) {
            throw new ValidacionException(MENSAJE_NIT_INVALIDO);
        }
        if (!Validaciones.esTelefonoValido(restaurante.telefono())) {
            throw new ValidacionException(MENSAJE_TELEFONO_INVALIDO);
        }
    }

    private void validarPropietario(Long idPropietario) {
        Usuario usuario = serviciosExternosPort.buscarUsuario(idPropietario)
                .orElseThrow(() -> new NoEncontradoException(String.format(MENSAJE_PROPIETARIO_NO_EXISTE, idPropietario)));
        if (usuario.rol() != Usuario.Rol.PROPIETARIO) {
            throw new ValidacionException(String.format(MENSAJE_NO_ES_PROPIETARIO, idPropietario));
        }
        if (restauranteRepositoryPort.existeDePropietario(idPropietario)) {
            throw new ConflictoException(MENSAJE_PROPIETARIO_CON_RESTAURANTE);
        }
    }
}
