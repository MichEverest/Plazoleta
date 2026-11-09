package com.mich.usuarios.infrastructure.adapter.input.rest;

import com.mich.usuarios.application.service.UsuarioService;
import com.mich.usuarios.domain.model.Usuario;
import com.mich.usuarios.infrastructure.adapter.input.rest.dto.UsuarioDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "HU1 - Crear propietario (ADMINISTRADOR)")
    @PostMapping("/propietario")
    public ResponseEntity<UsuarioDto.Creado> crearPropietario(@RequestBody UsuarioDto.Request request) {
        Usuario creado = usuarioService.crearPropietario(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).body(new UsuarioDto.Creado(creado.getId()));
    }

    @Operation(summary = "HU6 - Crear empleado (PROPIETARIO)")
    @PostMapping("/empleado")
    public ResponseEntity<UsuarioDto.Creado> crearEmpleado(@RequestBody UsuarioDto.Request request) {
        Usuario creado = usuarioService.crearEmpleado(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).body(new UsuarioDto.Creado(creado.getId()));
    }

    @Operation(summary = "HU8 - Registrar cliente (público)")
    @PostMapping("/cliente")
    public ResponseEntity<UsuarioDto.Creado> crearCliente(@RequestBody UsuarioDto.Request request) {
        Usuario creado = usuarioService.crearCliente(request.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).body(new UsuarioDto.Creado(creado.getId()));
    }

    @Operation(summary = "Consultar usuario por id (autenticado)")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDto.Response> consultarPorId(@PathVariable Long id) {
        Usuario usuario = usuarioService.consultarPorId(id);
        return ResponseEntity.ok(UsuarioDto.Response.de(usuario));
    }
}
