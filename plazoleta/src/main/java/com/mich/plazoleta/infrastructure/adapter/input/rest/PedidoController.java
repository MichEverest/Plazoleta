package com.mich.plazoleta.infrastructure.adapter.input.rest;

import com.mich.plazoleta.application.service.PedidoService;
import com.mich.plazoleta.domain.model.Pagina;
import com.mich.plazoleta.domain.model.Pedido;
import com.mich.plazoleta.infrastructure.adapter.input.rest.dto.PaginaDto;
import com.mich.plazoleta.infrastructure.adapter.input.rest.dto.PedidoDto;
import com.mich.plazoleta.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional
@RequestMapping("/pedidos")
@Tag(name = "Pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @Operation(summary = "HU11 - Realizar pedido (CLIENTE)")
    @PostMapping
    public ResponseEntity<PedidoDto.Response> crear(@RequestBody PedidoDto.Request request) {
        UsuarioAutenticado cliente = UsuarioAutenticado.actual();
        Pedido creado = pedidoService.crear(request.toDomain(), cliente.id(), cliente.correo());
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoDto.Response.de(creado));
    }

    @Operation(summary = "Mis pedidos (CLIENTE)")
    @GetMapping("/mis-pedidos")
    public ResponseEntity<PaginaDto<PedidoDto.Response>> listarMisPedidos(@RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "10") int size) {
        Pagina<Pedido> pedidos = pedidoService.listarDeCliente(UsuarioAutenticado.actual().id(),
                new Pagina.Solicitud(page, size));
        return ResponseEntity.ok(PaginaDto.de(pedidos, PedidoDto.Response::de));
    }

    @Operation(summary = "HU12 - Listar pedidos del restaurante por estado (EMPLEADO)")
    @GetMapping
    public ResponseEntity<PaginaDto<PedidoDto.Response>> listarDeRestaurante(
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pagina<Pedido> pedidos = pedidoService.listarDeRestaurante(UsuarioAutenticado.actual().id(), estado,
                new Pagina.Solicitud(page, size));
        return ResponseEntity.ok(PaginaDto.de(pedidos, PedidoDto.Response::de));
    }

    @Operation(summary = "HU13 - Asignarse un pedido (EMPLEADO)")
    @PatchMapping("/{id}/asignar")
    public ResponseEntity<PedidoDto.Response> asignar(@PathVariable Long id) {
        UsuarioAutenticado empleado = UsuarioAutenticado.actual();
        Pedido asignado = pedidoService.asignar(id, empleado.id(), empleado.correo());
        return ResponseEntity.ok(PedidoDto.Response.de(asignado));
    }

    @Operation(summary = "HU14 - Marcar pedido listo y notificar al cliente (EMPLEADO)")
    @PatchMapping("/{id}/listo")
    public ResponseEntity<PedidoDto.Response> marcarListo(@PathVariable Long id) {
        UsuarioAutenticado empleado = UsuarioAutenticado.actual();
        Pedido listo = pedidoService.marcarListo(id, empleado.id(), empleado.correo());
        return ResponseEntity.ok(PedidoDto.Response.de(listo));
    }

    @Operation(summary = "HU15 - Entregar pedido con PIN (EMPLEADO)")
    @PatchMapping("/{id}/entregar")
    public ResponseEntity<PedidoDto.Response> entregar(@PathVariable Long id, @RequestBody PedidoDto.Entrega request) {
        UsuarioAutenticado empleado = UsuarioAutenticado.actual();
        Pedido entregado = pedidoService.entregar(id, request.pin(), empleado.id(), empleado.correo());
        return ResponseEntity.ok(PedidoDto.Response.de(entregado));
    }

    @Operation(summary = "HU16 - Cancelar pedido pendiente (CLIENTE)")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<PedidoDto.Response> cancelar(@PathVariable Long id) {
        UsuarioAutenticado cliente = UsuarioAutenticado.actual();
        Pedido cancelado = pedidoService.cancelar(id, cliente.id(), cliente.correo());
        return ResponseEntity.ok(PedidoDto.Response.de(cancelado));
    }
}
