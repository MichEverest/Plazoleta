package com.mich.plazoleta.infrastructure.adapter.input.rest;

import com.mich.plazoleta.application.service.PlatoService;
import com.mich.plazoleta.domain.model.Categoria;
import com.mich.plazoleta.infrastructure.adapter.input.rest.dto.PlatoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Transactional
@RequestMapping("/categorias")
@Tag(name = "Categorías")
public class CategoriaController {

    private final PlatoService platoService;

    public CategoriaController(PlatoService platoService) {
        this.platoService = platoService;
    }

    @Operation(summary = "Listar categorías de platos (CLIENTE, PROPIETARIO)")
    @GetMapping
    public ResponseEntity<List<PlatoDto.Categoria>> listar() {
        List<Categoria> categorias = platoService.listarCategorias();
        return ResponseEntity.ok(categorias.stream().map(PlatoDto.Categoria::de).toList());
    }
}
