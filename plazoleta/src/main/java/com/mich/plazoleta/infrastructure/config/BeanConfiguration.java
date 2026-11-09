package com.mich.plazoleta.infrastructure.config;

import com.mich.plazoleta.application.port.output.PedidoRepositoryPort;
import com.mich.plazoleta.application.port.output.PlatoRepositoryPort;
import com.mich.plazoleta.application.port.output.RestauranteRepositoryPort;
import com.mich.plazoleta.application.port.output.ServiciosExternosPort;
import com.mich.plazoleta.application.service.PedidoService;
import com.mich.plazoleta.application.service.PlatoService;
import com.mich.plazoleta.application.service.RestauranteService;
import com.mich.plazoleta.application.usecase.PedidoUseCase;
import com.mich.plazoleta.application.usecase.PlatoUseCase;
import com.mich.plazoleta.application.usecase.RestauranteUseCase;
import com.mich.plazoleta.infrastructure.adapter.output.feign.ServiciosExternosAdapter;
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
@EnableJpaRepositories(basePackages = "com.mich.plazoleta.infrastructure.adapter.output.persistence",
        considerNestedRepositories = true)
@EnableFeignClients(clients = {ServiciosExternosAdapter.UsuarioClient.class,
        ServiciosExternosAdapter.MensajeriaClient.class, ServiciosExternosAdapter.TrazabilidadClient.class})
public class BeanConfiguration {

    private static final String BEARER_JWT = "bearerJwt";

    @Bean
    public RestauranteService restauranteService(RestauranteRepositoryPort restauranteRepositoryPort,
                                                 ServiciosExternosPort serviciosExternosPort) {
        return new RestauranteUseCase(restauranteRepositoryPort, serviciosExternosPort);
    }

    @Bean
    public PlatoService platoService(PlatoRepositoryPort platoRepositoryPort,
                                     RestauranteRepositoryPort restauranteRepositoryPort) {
        return new PlatoUseCase(platoRepositoryPort, restauranteRepositoryPort);
    }

    @Bean
    public PedidoService pedidoService(PedidoRepositoryPort pedidoRepositoryPort, PlatoRepositoryPort platoRepositoryPort,
                                       RestauranteRepositoryPort restauranteRepositoryPort,
                                       ServiciosExternosPort serviciosExternosPort) {
        return new PedidoUseCase(pedidoRepositoryPort, platoRepositoryPort, restauranteRepositoryPort,
                serviciosExternosPort);
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
                .info(new Info().title("Microservicio Plazoleta").version("1.0.0"))
                .components(new Components().addSecuritySchemes(BEARER_JWT,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
    }
}
