package com.mich.trazabilidad.infrastructure.adapter.input.rest;

import com.mich.trazabilidad.application.service.TrazabilidadService;
import com.mich.trazabilidad.domain.model.Eficiencia;
import com.mich.trazabilidad.domain.model.Pagina;
import com.mich.trazabilidad.infrastructure.adapter.input.rest.dto.PaginaDto;
import com.mich.trazabilidad.infrastructure.adapter.input.rest.dto.TrazabilidadDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional
@RequestMapping("/trazabilidad/eficiencia")
@Tag(name = "Eficiencia")
public class EficienciaController {

    private final TrazabilidadService trazabilidadService;

    public EficienciaController(TrazabilidadService trazabilidadService) {
        this.trazabilidadService = trazabilidadService;
    }

    @Operation(summary = "HU18 - Tiempo de cada pedido entregado y promedio del restaurante (PROPIETARIO)")
    @GetMapping("/pedidos")
    public ResponseEntity<TrazabilidadDto.ReporteEficiencia> calcularPedidos(@RequestParam(defaultValue = "0") int page,
                                                                             @RequestParam(defaultValue = "10") int size) {
        Eficiencia.Reporte reporte = trazabilidadService.calcularEficienciaPedidos(new Pagina.Solicitud(page, size));
        return ResponseEntity.ok(TrazabilidadDto.ReporteEficiencia.de(reporte));
    }

    @Operation(summary = "HU18 - Ranking de empleados por tiempo medio de preparación (PROPIETARIO)")
    @GetMapping("/empleados")
    public ResponseEntity<PaginaDto<TrazabilidadDto.RankingEmpleado>> calcularEmpleados(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pagina<Eficiencia.Empleado> ranking = trazabilidadService.calcularRankingEmpleados(new Pagina.Solicitud(page, size));
        return ResponseEntity.ok(PaginaDto.de(ranking, TrazabilidadDto.RankingEmpleado::de));
    }
}
