package com.mich.usuarios.infrastructure.config;

import com.mich.usuarios.application.port.output.SeguridadPort;
import com.mich.usuarios.application.port.output.UsuarioRepositoryPort;
import com.mich.usuarios.application.service.UsuarioService;
import com.mich.usuarios.application.usecase.UsuarioUseCase;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.mich.usuarios.infrastructure.adapter.output.persistence",
        considerNestedRepositories = true)
public class BeanConfiguration {

    private static final String BEARER_JWT = "bearerJwt";

    @Bean
    public UsuarioService usuarioService(UsuarioRepositoryPort usuarioRepositoryPort, SeguridadPort seguridadPort) {
        return new UsuarioUseCase(usuarioRepositoryPort, seguridadPort);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("Microservicio Usuarios").version("1.0.0"))
                .components(new Components().addSecuritySchemes(BEARER_JWT,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
    }
}
