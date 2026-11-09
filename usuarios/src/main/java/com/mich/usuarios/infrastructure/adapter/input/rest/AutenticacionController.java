package com.mich.usuarios.infrastructure.adapter.input.rest;

import com.mich.usuarios.application.service.UsuarioService;
import com.mich.usuarios.domain.model.Usuario;
import com.mich.usuarios.infrastructure.adapter.input.rest.dto.UsuarioDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación")
public class AutenticacionController {

    private final UsuarioService usuarioService;

    public AutenticacionController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "HU5 - Iniciar sesión (público)")
    @PostMapping("/login")
    public ResponseEntity<UsuarioDto.Sesion> iniciarSesion(@RequestBody UsuarioDto.Login request) {
        Usuario.Sesion sesion = usuarioService.iniciarSesion(request.correo(), request.clave());
        return ResponseEntity.ok(UsuarioDto.Sesion.de(sesion));
    }
}
