package com.mich.trazabilidad.infrastructure.adapter.input.rest;

import com.mich.trazabilidad.application.service.TrazabilidadService;
import com.mich.trazabilidad.domain.model.Trazabilidad;
import com.mich.trazabilidad.infrastructure.adapter.input.rest.dto.TrazabilidadDto;
import com.mich.trazabilidad.infrastructure.security.UsuarioAutenticado;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Transactional
@RequestMapping("/trazabilidad")
@Tag(name = "Trazabilidad")
public class TrazabilidadController {

    private final TrazabilidadService trazabilidadService;

    public TrazabilidadController(TrazabilidadService trazabilidadService) {
        this.trazabilidadService = trazabilidadService;
    }

    @Operation(summary = "Registrar un cambio de estado de pedido (uso interno de Plazoleta)")
    @PostMapping
    public ResponseEntity<Void> registrar(@RequestBody TrazabilidadDto.Request request) {
        trazabilidadService.registrar(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "HU17 - Trazabilidad de un pedido propio (CLIENTE)")
    @GetMapping("/pedidos/{idPedido}")
    public ResponseEntity<List<TrazabilidadDto.Historial>> consultarHistorial(@PathVariable Long idPedido) {
        List<Trazabilidad.Historial> historial = trazabilidadService.consultarHistorial(idPedido,
                UsuarioAutenticado.actual().id());
        return ResponseEntity.ok(historial.stream().map(TrazabilidadDto.Historial::de).toList());
    }
}
