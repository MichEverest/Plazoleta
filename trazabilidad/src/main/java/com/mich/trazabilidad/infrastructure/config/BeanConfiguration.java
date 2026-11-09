package com.mich.trazabilidad.infrastructure.config;

import com.mich.trazabilidad.application.port.output.PlazoletaPort;
import com.mich.trazabilidad.application.port.output.TrazabilidadRepositoryPort;
import com.mich.trazabilidad.application.service.TrazabilidadService;
import com.mich.trazabilidad.application.usecase.TrazabilidadUseCase;
import com.mich.trazabilidad.infrastructure.adapter.output.feign.PlazoletaAdapter;
import feign.RequestInterceptor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@EnableJpaRepositories(basePackages = "com.mich.trazabilidad.infrastructure.adapter.output.persistence",
        considerNestedRepositories = true)
@EnableFeignClients(clients = PlazoletaAdapter.PlazoletaClient.class)
public class BeanConfiguration {

    private static final String BEARER_JWT = "bearerJwt";

    @Bean
    public TrazabilidadService trazabilidadService(TrazabilidadRepositoryPort trazabilidadRepositoryPort,
                                                   PlazoletaPort plazoletaPort) {
        return new TrazabilidadUseCase(trazabilidadRepositoryPort, plazoletaPort);
    }

    @Bean
    public RequestInterceptor reenviarAutorizacion() {
        return template -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
                String autorizacion = atributos.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (autorizacion != null) {
                    template.header(HttpHeaders.AUTHORIZATION, autorizacion);
                }
            }
        };
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("Microservicio Trazabilidad").version("1.0.0"))
                .components(new Components().addSecuritySchemes(BEARER_JWT,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
    }
}
