package com.mich.mensajeria.infrastructure.config;

import com.mich.mensajeria.infrastructure.adapter.input.rest.GlobalExceptionHandler.ErrorResponse;
import com.mich.mensajeria.infrastructure.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class SecurityConfiguration {

    private static final String[] RUTAS_SWAGGER = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"};
    private static final List<String> METODOS_PERMITIDOS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final List<String> HEADERS_PERMITIDOS = List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE);
    private static final String MENSAJE_NO_AUTENTICADO = "Se requiere autenticación";
    private static final String MENSAJE_SIN_PERMISOS = "No tiene permisos para realizar esta acción";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final List<String> allowedOrigins;

    public SecurityConfiguration(JwtService jwtService, ObjectMapper objectMapper,
                                 @Value("${cors.allowed-origins}") List<String> allowedOrigins) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((request, response, ex) ->
                                escribirError(response, MENSAJE_NO_AUTENTICADO, HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, ex) ->
                                escribirError(response, MENSAJE_SIN_PERMISOS, HttpStatus.FORBIDDEN)))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(RUTAS_SWAGGER).permitAll();
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtService.filtro(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(METODOS_PERMITIDOS);
        configuration.setAllowedHeaders(HEADERS_PERMITIDOS);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void escribirError(HttpServletResponse response, String mensaje, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(mensaje, status));
    }
}
