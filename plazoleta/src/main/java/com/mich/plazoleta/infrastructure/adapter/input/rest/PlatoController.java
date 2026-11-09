package com.mich.plazoleta.infrastructure.adapter.input.rest;

import com.mich.plazoleta.application.service.PlatoService;
import com.mich.plazoleta.domain.model.Plato;
import com.mich.plazoleta.infrastructure.adapter.input.rest.dto.PlatoDto;
import com.mich.plazoleta.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional
@RequestMapping("/platos")
@Tag(name = "Platos")
public class PlatoController {

    private final PlatoService platoService;

    public PlatoController(PlatoService platoService) {
        this.platoService = platoService;
    }

    @Operation(summary = "HU3 - Crear plato (PROPIETARIO dueño del restaurante)")
    @PostMapping
    public ResponseEntity<PlatoDto.Response> crear(@RequestBody PlatoDto.Request request) {
        Plato creado = platoService.crear(request.toDomain(), UsuarioAutenticado.actual().id());
        return ResponseEntity.status(HttpStatus.CREATED).body(PlatoDto.Response.de(creado));
    }

    @Operation(summary = "HU4 - Modificar precio y descripción (PROPIETARIO dueño)")
    @PutMapping("/{id}")
    public ResponseEntity<PlatoDto.Response> actualizar(@PathVariable Long id,
                                                        @RequestBody PlatoDto.Actualizacion request) {
        Plato actualizado = platoService.actualizar(id, request.precio(), request.descripcion(),
                UsuarioAutenticado.actual().id());
        return ResponseEntity.ok(PlatoDto.Response.de(actualizado));
    }

    @Operation(summary = "HU7 - Habilitar o deshabilitar plato (PROPIETARIO dueño)")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<PlatoDto.Response> cambiarEstado(@PathVariable Long id, @RequestBody PlatoDto.Estado request) {
        Plato actualizado = platoService.cambiarEstado(id, request.activo(), UsuarioAutenticado.actual().id());
        return ResponseEntity.ok(PlatoDto.Response.de(actualizado));
    }
}
