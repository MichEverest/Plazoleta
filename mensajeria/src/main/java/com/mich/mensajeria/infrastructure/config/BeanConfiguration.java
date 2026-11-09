package com.mich.mensajeria.infrastructure.config;

import com.mich.mensajeria.application.port.output.SmsPort;
import com.mich.mensajeria.application.service.SmsService;
import com.mich.mensajeria.application.usecase.SmsUseCase;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    private static final String BEARER_JWT = "bearerJwt";

    @Bean
    public SmsService smsService(SmsPort smsPort) {
        return new SmsUseCase(smsPort);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("Microservicio Mensajería").version("1.0.0"))
                .components(new Components().addSecuritySchemes(BEARER_JWT,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
    }
}
