package com.mich.trazabilidad.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Deja la consola H2 (/h2-console) con la conexión de este servicio ya escrita: nombre, JDBC URL y usuario se toman de
 * spring.datasource, así que solo hay que pulsar "Conectar". Le indica a H2 que lea sus conexiones guardadas de
 * ./data/h2-console en vez de ~/.h2.server.properties, que comparten todos los servicios.
 */
@Configuration
@ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
public class H2ConsolaConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(H2ConsolaConfiguration.class);
    private static final String SERVLET_H2 = "org.h2.server.web.JakartaWebServlet";
    private static final Path CARPETA = Path.of("data", "h2-console").toAbsolutePath();
    private static final String ARCHIVO = ".h2.server.properties";
    private static final String DRIVER_H2 = "org.h2.Driver";

    @Bean
    static BeanPostProcessor consolaH2Precargada(Environment environment) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof ServletRegistrationBean<?> registro
                        && SERVLET_H2.equals(registro.getServlet().getClass().getName())) {
                    escribirConexion(environment);
                    registro.addInitParameter("properties", CARPETA.toString());
                }
                return bean;
            }
        };
    }

    private static void escribirConexion(Environment environment) {
        String nombre = environment.getProperty("spring.application.name", "trazabilidad");
        String url = environment.getRequiredProperty("spring.datasource.url");
        String usuario = environment.getProperty("spring.datasource.username", "sa");
        Properties conexiones = new Properties();
        conexiones.setProperty("0", String.join("|", nombre + " (local)", DRIVER_H2, url, usuario));
        conexiones.setProperty("webAllowOthers", "false");
        try {
            Files.createDirectories(CARPETA);
            try (OutputStream salida = Files.newOutputStream(CARPETA.resolve(ARCHIVO))) {
                conexiones.store(salida, "Generado al arrancar por H2ConsolaConfiguration");
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        LOGGER.info("Consola H2 lista con la conexión {} (usuario {}, sin clave)", url, usuario);
    }
}
