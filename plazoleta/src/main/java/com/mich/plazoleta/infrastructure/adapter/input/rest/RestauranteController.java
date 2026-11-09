package com.mich.plazoleta.infrastructure.adapter.input.rest;

import com.mich.plazoleta.application.service.PlatoService;
import com.mich.plazoleta.application.service.RestauranteService;
import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Plato;
import com.mich.plazoleta.domain.model.Restaurante;
import com.mich.plazoleta.infrastructure.adapter.input.rest.dto.PaginaDto;
import com.mich.plazoleta.infrastructure.adapter.input.rest.dto.PlatoDto;
import com.mich.plazoleta.infrastructure.adapter.input.rest.dto.RestauranteDto;
import com.mich.plazoleta.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional
@RequestMapping("/restaurantes")
@Tag(name = "Restaurantes")
public class RestauranteController {

    private final RestauranteService restauranteService;
    private final PlatoService platoService;

    public RestauranteController(RestauranteService restauranteService, PlatoService platoService) {
        this.restauranteService = restauranteService;
        this.platoService = platoService;
    }

    @Operation(summary = "HU2 - Crear restaurante (ADMINISTRADOR)")
    @PostMapping
    public ResponseEntity<RestauranteDto.Response> crear(@RequestBody RestauranteDto.Request request) {
        Restaurante creado = restauranteService.crear(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).body(RestauranteDto.Response.de(creado));
    }

    @Operation(summary = "Restaurante del propietario autenticado (PROPIETARIO)")
    @GetMapping("/mio")
    public ResponseEntity<RestauranteDto.Response> consultarMio() {
        Restaurante restaurante = restauranteService.consultarDePropietario(UsuarioAutenticado.actual().id());
        return ResponseEntity.ok(RestauranteDto.Response.de(restaurante));
    }

    @Operation(summary = "HU9 - Listar restaurantes en orden alfabético (CLIENTE)")
    @GetMapping
    public ResponseEntity<PaginaDto<RestauranteDto.Resumen>> listar(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        Pagina<Restaurante> restaurantes = restauranteService.listar(new Pagina.Solicitud(page, size));
        return ResponseEntity.ok(PaginaDto.de(restaurantes, RestauranteDto.Resumen::de));
    }

    @Operation(summary = "HU10 - Listar platos de un restaurante (CLIENTE, PROPIETARIO)")
    @GetMapping("/{idRestaurante}/platos")
    public ResponseEntity<PaginaDto<PlatoDto.Response>> listarPlatos(@PathVariable Long idRestaurante,
                                                                     @RequestParam(required = false) Long categoriaId,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        UsuarioAutenticado usuario = UsuarioAutenticado.actual();
        Pagina<Plato> platos = platoService.listarPorRestaurante(idRestaurante, categoriaId,
                new Pagina.Solicitud(page, size), usuario.id(), usuario.rol());
        return ResponseEntity.ok(PaginaDto.de(platos, PlatoDto.Response::de));
    }
}
