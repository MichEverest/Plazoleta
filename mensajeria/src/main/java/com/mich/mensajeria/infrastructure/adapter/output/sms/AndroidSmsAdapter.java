package com.mich.mensajeria.infrastructure.adapter.output.sms;

import com.mich.mensajeria.application.port.output.SmsPort;
import com.mich.mensajeria.domain.exception.ExcepcionesDominio.ProveedorSmsException;
import com.mich.mensajeria.domain.model.EstadoEnvio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * SMS real enviado desde un celular Android con la app "SMS Gateway for Android" (https://sms-gate.app):
 * el SMS sale de la SIM de ese teléfono, con su número y su plan. Funciona en dos modos:
 * <ul>
 *     <li>Servidor local: el PC y el celular en la misma Wi-Fi. URL {@code http://<ip-del-celular>:8080}.</li>
 *     <li>Nube: {@code https://api.sms-gate.app/3rdparty/v1}, sin importar la red.</li>
 * </ul>
 * El celular llega con 10 dígitos y aquí se le agrega +57, porque la app exige formato internacional.
 */
public class AndroidSmsAdapter implements SmsPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(AndroidSmsAdapter.class);
    private static final String INDICATIVO_COLOMBIA = "+57";
    private static final String MARCA_NUBE = "/3rdparty/";
    private static final String RUTA_LOCAL = "/message";
    private static final String RUTA_NUBE = "/messages";
    private static final String RUTA_SALUD = "/health";
    private static final Duration ESPERA_CONEXION = Duration.ofSeconds(5);
    private static final Duration ESPERA_RESPUESTA = Duration.ofSeconds(15);
    private static final String MENSAJE_CREDENCIALES = "la app SMS Gateway rechazó el usuario o la clave: revisa android.usuario y "
            + "android.clave en mensajeria/sms.properties (en la app: sección Local Server o Cloud Server)";
    private static final String MENSAJE_SIN_CONEXION_LOCAL = "No se pudo conectar con el celular en %s. Revisa que la app SMS Gateway "
            + "esté abierta con el servidor local iniciado, que el PC y el celular estén en la misma Wi-Fi y que la IP no haya cambiado";
    private static final String MENSAJE_SIN_CONEXION_NUBE = "No se pudo conectar con la nube de SMS Gateway (%s). Revisa tu conexión a internet";
    private static final String MENSAJE_RECHAZADO = "El celular Android no pudo enviar el SMS a %s: %s";

    private final RestClient restClient;
    private final String url;
    private final boolean nube;

    public AndroidSmsAdapter(String url, String usuario, String clave) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.nube = this.url.contains(MARCA_NUBE);
        // HTTP/1.1 sin intento de pasar a HTTP/2 y cuerpo con Content-Length (no "chunked"): es lo más compatible con
        // el servidor embebido en el celular. Corta a tiempo si el teléfono no responde.
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(ESPERA_CONEXION)
                .build());
        fabrica.setReadTimeout(ESPERA_RESPUESTA);
        this.restClient = RestClient.builder()
                .baseUrl(this.url)
                .requestFactory(fabrica)
                .defaultHeaders(headers -> headers.setBasicAuth(usuario, clave))
                .build();
    }

    @Override
    public void enviar(String celular, String mensaje) {
        // Formato documentado en docs.sms-gate.app: {"textMessage":{"text":"..."},"phoneNumbers":["+57..."]}
        byte[] cuerpo = ("{\"textMessage\":{\"text\":" + json(mensaje) + "},\"phoneNumbers\":["
                + json(INDICATIVO_COLOMBIA + celular) + "]}").getBytes(StandardCharsets.UTF_8);
        try {
            restClient.post()
                    .uri(nube ? RUTA_NUBE : RUTA_LOCAL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .toBodilessEntity();
            LOGGER.info("SMS entregado al celular Android para enviarlo a {}{}", INDICATIVO_COLOMBIA, celular);
        } catch (RestClientResponseException ex) {
            String detalle = ex.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)
                    ? MENSAJE_CREDENCIALES
                    : "respondió HTTP " + ex.getStatusCode().value() + " " + ex.getResponseBodyAsString();
            LOGGER.warn("SMS Gateway rechazó el SMS a {}: {}", celular, detalle);
            throw new ProveedorSmsException(String.format(MENSAJE_RECHAZADO, celular, detalle));
        } catch (ResourceAccessException ex) {
            String mensajeError = String.format(nube ? MENSAJE_SIN_CONEXION_NUBE : MENSAJE_SIN_CONEXION_LOCAL, url);
            LOGGER.warn("{} (causa: {})", mensajeError, ex.getMessage());
            throw new ProveedorSmsException(mensajeError);
        }
    }

    @Override
    public EstadoEnvio estado() {
        return EstadoEnvio.REAL;
    }

    @Override
    public String proveedor() {
        return nube ? "celular Android (nube SMS Gateway)" : "celular Android (" + url + ")";
    }

    private static String json(String texto) {
        StringBuilder salida = new StringBuilder("\"");
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> salida.append("\\\"");
                case '\\' -> salida.append("\\\\");
                case '\n' -> salida.append("\\n");
                case '\r' -> salida.append("\\r");
                case '\t' -> salida.append("\\t");
                default -> {
                    if (c < 0x20) {
                        salida.append(String.format("\\u%04x", (int) c));
                    } else {
                        salida.append(c);
                    }
                }
            }
        }
        return salida.append('"').toString();
    }

    /**
     * En modo local comprueba al arrancar que el celular responde. No detiene el servicio: solo deja el resultado en el log.
     */
    public void verificarConexion() {
        if (nube) {
            LOGGER.info("SMS Gateway (nube): se usará {}. El celular debe tener la app abierta y conectada a internet", url);
            return;
        }
        try {
            restClient.get().uri(RUTA_SALUD).retrieve().toBodilessEntity();
            LOGGER.info("SMS Gateway: el celular responde en {}", url);
        } catch (RestClientResponseException ex) {
            LOGGER.warn("SMS Gateway: el celular respondió HTTP {} en {}{}", ex.getStatusCode().value(), url, RUTA_SALUD);
        } catch (ResourceAccessException ex) {
            LOGGER.error("{} (causa: {})", String.format(MENSAJE_SIN_CONEXION_LOCAL, url), ex.getMessage());
        }
    }
}
