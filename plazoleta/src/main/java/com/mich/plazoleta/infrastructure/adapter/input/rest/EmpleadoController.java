package com.mich.plazoleta.infrastructure.adapter.input.rest;

import com.mich.plazoleta.application.service.RestauranteService;
import com.mich.plazoleta.infrastructure.adapter.input.rest.dto.RestauranteDto;
import com.mich.plazoleta.infrastructure.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional
@RequestMapping("/empleados")
@Tag(name = "Empleados")
public class EmpleadoController {

    private final RestauranteService restauranteService;

    public EmpleadoController(RestauranteService restauranteService) {
        this.restauranteService = restauranteService;
    }

    @Operation(summary = "HU6 - Crear empleado del restaurante del propietario (PROPIETARIO)")
    @PostMapping
    public ResponseEntity<RestauranteDto.EmpleadoCreado> crear(@RequestBody RestauranteDto.Empleado request) {
        Long idEmpleado = restauranteService.crearEmpleado(request.toDomain(), UsuarioAutenticado.actual().id());
        return ResponseEntity.status(HttpStatus.CREATED).body(new RestauranteDto.EmpleadoCreado(idEmpleado));
    }
}
