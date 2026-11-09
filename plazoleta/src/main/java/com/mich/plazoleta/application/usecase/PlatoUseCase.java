package com.mich.plazoleta.application.usecase;

import com.mich.plazoleta.application.port.output.PlatoRepositoryPort;
import com.mich.plazoleta.application.port.output.RestauranteRepositoryPort;
import com.mich.plazoleta.application.service.PlatoService;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.NoEncontradoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ProhibidoException;
import com.mich.plazoleta.domain.exception.ExcepcionesDominio.ValidacionException;
import com.mich.plazoleta.domain.model.Categoria;
import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Plato;
import com.mich.plazoleta.domain.model.Restaurante;
import com.mich.plazoleta.domain.model.Usuario;

import java.util.List;

public class PlatoUseCase implements PlatoService {

    private static final String MENSAJE_PRECIO_INVALIDO = "El precio debe ser un número entero mayor a 0";
    private static final String MENSAJE_CATEGORIA_NO_EXISTE = "No existe la categoría con id %d";
    private static final String MENSAJE_RESTAURANTE_NO_EXISTE = "No existe el restaurante con id %d";
    private static final String MENSAJE_PLATO_NO_EXISTE = "No existe el plato con id %d";
    private static final String MENSAJE_RESTAURANTE_AJENO = "El restaurante no pertenece al propietario autenticado";

    private final PlatoRepositoryPort platoRepositoryPort;
    private final RestauranteRepositoryPort restauranteRepositoryPort;

    public PlatoUseCase(PlatoRepositoryPort platoRepositoryPort, RestauranteRepositoryPort restauranteRepositoryPort) {
        this.platoRepositoryPort = platoRepositoryPort;
        this.restauranteRepositoryPort = restauranteRepositoryPort;
    }

    @Override
    public Plato crear(Plato plato, Long idPropietario) {
        Validaciones.requerido(plato.getNombre(), "nombre");
        Validaciones.requerido(plato.getDescripcion(), "descripcion");
        Validaciones.requerido(plato.getUrlImagen(), "urlImagen");
        Validaciones.requerido(plato.getIdCategoria(), "idCategoria");
        Validaciones.requerido(plato.getIdRestaurante(), "idRestaurante");
        validarPrecio(plato.getPrecio());
        if (!platoRepositoryPort.existeCategoria(plato.getIdCategoria())) {
            throw new NoEncontradoException(String.format(MENSAJE_CATEGORIA_NO_EXISTE, plato.getIdCategoria()));
        }
        validarDueno(buscarRestaurante(plato.getIdRestaurante()), idPropietario);
        plato.setActivo(true);
        return platoRepositoryPort.guardar(plato);
    }

    @Override
    public Plato actualizar(Long idPlato, Integer precio, String descripcion, Long idPropietario) {
        validarPrecio(precio);
        Validaciones.requerido(descripcion, "descripcion");
        Plato plato = buscarPlatoDelPropietario(idPlato, idPropietario);
        plato.setPrecio(precio);
        plato.setDescripcion(descripcion);
        return platoRepositoryPort.guardar(plato);
    }

    @Override
    public Plato cambiarEstado(Long idPlato, Boolean activo, Long idPropietario) {
        Validaciones.requerido(activo, "activo");
        Plato plato = buscarPlatoDelPropietario(idPlato, idPropietario);
        plato.setActivo(activo);
        return platoRepositoryPort.guardar(plato);
    }

    @Override
    public Pagina<Plato> listarPorRestaurante(Long idRestaurante, Long idCategoria, Pagina.Solicitud solicitud,
                                              Long idUsuario, String rol) {
        Restaurante restaurante = buscarRestaurante(idRestaurante);
        boolean esPropietario = Usuario.Rol.PROPIETARIO.name().equals(rol);
        if (esPropietario) {
            validarDueno(restaurante, idUsuario);
        }
        return platoRepositoryPort.listarPorRestaurante(idRestaurante, idCategoria, !esPropietario, solicitud);
    }

    @Override
    public List<Categoria> listarCategorias() {
        return platoRepositoryPort.listarCategorias();
    }

    private void validarPrecio(Integer precio) {
        Validaciones.requerido(precio, "precio");
        if (precio <= 0) {
            throw new ValidacionException(MENSAJE_PRECIO_INVALIDO);
        }
    }

    private Plato buscarPlatoDelPropietario(Long idPlato, Long idPropietario) {
        Plato plato = platoRepositoryPort.buscarPorId(idPlato)
                .orElseThrow(() -> new NoEncontradoException(String.format(MENSAJE_PLATO_NO_EXISTE, idPlato)));
        validarDueno(buscarRestaurante(plato.getIdRestaurante()), idPropietario);
        return plato;
    }

    private Restaurante buscarRestaurante(Long idRestaurante) {
        return restauranteRepositoryPort.buscarPorId(idRestaurante)
                .orElseThrow(() -> new NoEncontradoException(String.format(MENSAJE_RESTAURANTE_NO_EXISTE, idRestaurante)));
    }

    private void validarDueno(Restaurante restaurante, Long idPropietario) {
        if (!restaurante.idPropietario().equals(idPropietario)) {
            throw new ProhibidoException(MENSAJE_RESTAURANTE_AJENO);
        }
    }
}
