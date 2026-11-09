package com.mich.plazoleta.infrastructure.config;

import com.mich.plazoleta.domain.model.Usuario.Rol;
import com.mich.plazoleta.infrastructure.adapter.input.rest.GlobalExceptionHandler.ErrorResponse;
import com.mich.plazoleta.infrastructure.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
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

    private static final String ADMINISTRADOR = Rol.ADMINISTRADOR.name();
    private static final String PROPIETARIO = Rol.PROPIETARIO.name();
    private static final String EMPLEADO = Rol.EMPLEADO.name();
    private static final String CLIENTE = Rol.CLIENTE.name();
    private static final String[] RUTAS_FRONT = {"/", "/index.html", "/styles.css"};
    private static final String[] RUTAS_SWAGGER = {"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"};
    private static final String RUTA_H2_CONSOLE = "/h2-console/**";
    private static final List<String> METODOS_PERMITIDOS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final List<String> HEADERS_PERMITIDOS = List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE);
    private static final String MENSAJE_NO_AUTENTICADO = "Se requiere autenticación";
    private static final String MENSAJE_SIN_PERMISOS = "No tiene permisos para realizar esta acción";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final List<String> allowedOrigins;
    private final boolean h2ConsoleEnabled;

    public SecurityConfiguration(JwtService jwtService, ObjectMapper objectMapper,
                                 @Value("${cors.allowed-origins}") List<String> allowedOrigins,
                                 @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.allowedOrigins = allowedOrigins;
        this.h2ConsoleEnabled = h2ConsoleEnabled;
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
                    auth.requestMatchers(HttpMethod.GET, RUTAS_FRONT).permitAll();
                    auth.requestMatchers(RUTAS_SWAGGER).permitAll();
                    if (h2ConsoleEnabled) {
                        auth.requestMatchers(RUTA_H2_CONSOLE).permitAll();
                    }
                    auth.requestMatchers(HttpMethod.POST, "/restaurantes").hasRole(ADMINISTRADOR);
                    auth.requestMatchers(HttpMethod.GET, "/restaurantes/mio").hasRole(PROPIETARIO);
                    auth.requestMatchers(HttpMethod.GET, "/restaurantes").hasRole(CLIENTE);
                    auth.requestMatchers(HttpMethod.GET, "/restaurantes/*/platos", "/categorias")
                            .hasAnyRole(CLIENTE, PROPIETARIO);
                    auth.requestMatchers(HttpMethod.POST, "/platos", "/empleados").hasRole(PROPIETARIO);
                    auth.requestMatchers(HttpMethod.PUT, "/platos/*").hasRole(PROPIETARIO);
                    auth.requestMatchers(HttpMethod.PATCH, "/platos/*/estado").hasRole(PROPIETARIO);
                    auth.requestMatchers(HttpMethod.POST, "/pedidos").hasRole(CLIENTE);
                    auth.requestMatchers(HttpMethod.GET, "/pedidos/mis-pedidos").hasRole(CLIENTE);
                    auth.requestMatchers(HttpMethod.PATCH, "/pedidos/*/cancelar").hasRole(CLIENTE);
                    auth.requestMatchers(HttpMethod.GET, "/pedidos").hasRole(EMPLEADO);
                    auth.requestMatchers(HttpMethod.PATCH, "/pedidos/*/asignar", "/pedidos/*/listo",
                            "/pedidos/*/entregar").hasRole(EMPLEADO);
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtService.filtro(), UsernamePasswordAuthenticationFilter.class);
        if (h2ConsoleEnabled) {
            http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        }
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
