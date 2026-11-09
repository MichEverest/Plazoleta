package com.mich.trazabilidad.infrastructure.adapter.output.feign;

import com.mich.trazabilidad.application.port.output.PlazoletaPort;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

@Component
public class PlazoletaAdapter implements PlazoletaPort {

    @FeignClient(name = "plazoleta", url = "${services.plazoleta.url}")
    public interface PlazoletaClient {

        record RestauranteResponse(Long id) {
        }

        @GetMapping("/restaurantes/mio")
        RestauranteResponse consultarMiRestaurante();
    }

    private final PlazoletaClient plazoletaClient;

    public PlazoletaAdapter(PlazoletaClient plazoletaClient) {
        this.plazoletaClient = plazoletaClient;
    }

    @Override
    public Long obtenerIdRestauranteDelPropietario() {
        return plazoletaClient.consultarMiRestaurante().id();
    }
}
