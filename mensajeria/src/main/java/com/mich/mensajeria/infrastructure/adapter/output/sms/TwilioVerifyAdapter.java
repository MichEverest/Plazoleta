package com.mich.mensajeria.infrastructure.adapter.output.sms;

import com.mich.mensajeria.application.port.output.VerificacionPort;
import com.mich.mensajeria.domain.exception.ExcepcionesDominio.ProveedorSmsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Códigos de entrega con Twilio Verify: Twilio genera el código, lo envía por SMS y luego dice si el que da el cliente
 * es correcto. Funciona con la cuenta gratis (trial) hacia números verificados, que no permite enviar texto propio.
 * Los códigos vencen a los 10 minutos y cada envío nuevo reemplaza al anterior.
 */
public class TwilioVerifyAdapter implements VerificacionPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwilioVerifyAdapter.class);
    private static final String INDICATIVO_COLOMBIA = "+57";
    private static final String VERIFICACIONES_URI = "/v2/Services/{servicio}/Verifications";
    private static final String COMPROBACION_URI = "/v2/Services/{servicio}/VerificationCheck";
    private static final String SERVICIO_URI = "/v2/Services/{servicio}";
    private static final String ESTADO_APROBADO = "approved";
    private static final Pattern MENSAJE_TWILIO = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern CODIGO_TWILIO = Pattern.compile("\"code\"\\s*:\\s*(\\d+)");
    private static final Pattern ESTADO = Pattern.compile("\"status\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern NOMBRE = Pattern.compile("\"friendly_name\"\\s*:\\s*\"([^\"]*)\"");
    private static final Map<Integer, String> AYUDA_POR_CODIGO = Map.of(
            20003, "las credenciales de Twilio no son válidas: revisa twilio.account-sid y twilio.auth-token",
            20404, "no existe el servicio de Verify: revisa twilio.verify-service-sid (ejecuta de nuevo configurar-sms.sh)",
            21608, "tu cuenta de Twilio es de prueba y ese número no está verificado: verifícalo en Phone Numbers > "
                    + "Verified Caller IDs o activa la cuenta (Upgrade)",
            60200, "Twilio no acepta ese número",
            60202, "se superó el número de intentos con este código: pulsa Reenviar PIN para enviar uno nuevo",
            60203, "se enviaron demasiados códigos a este número: espera unos 10 minutos e inténtalo de nuevo",
            60205, "ese número no puede recibir SMS",
            60410, "Twilio bloqueó temporalmente los envíos a ese número por seguridad: inténtalo más tarde");
    private static final String MENSAJE_RECHAZADO = "Twilio Verify no pudo %s a %s: %s";
    private static final String MENSAJE_SIN_CONEXION = "No se pudo conectar con Twilio Verify. Revisa tu conexión a internet";

    private final RestClient restClient;
    private final String servicio;

    public TwilioVerifyAdapter(String baseUrl, String accountSid, String authToken, String servicio) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(accountSid, authToken))
                .build();
        this.servicio = servicio;
    }

    @Override
    public boolean disponible() {
        return true;
    }

    @Override
    public String proveedor() {
        return "Twilio Verify";
    }

    @Override
    public void enviarCodigo(String celular) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", INDICATIVO_COLOMBIA + celular);
        form.add("Channel", "sms");
        form.add("Locale", "es");
        try {
            restClient.post().uri(VERIFICACIONES_URI, servicio)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            LOGGER.info("Twilio Verify envió un código por SMS a {}{}", INDICATIVO_COLOMBIA, celular);
        } catch (RestClientResponseException ex) {
            throw rechazo("enviar el código", celular, ex);
        } catch (ResourceAccessException ex) {
            throw new ProveedorSmsException(MENSAJE_SIN_CONEXION);
        }
    }

    @Override
    public boolean comprobarCodigo(String celular, String codigo) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", INDICATIVO_COLOMBIA + celular);
        form.add("Code", codigo);
        try {
            String respuesta = restClient.post().uri(COMPROBACION_URI, servicio)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            String estado = extraer(ESTADO, respuesta, "");
            LOGGER.info("Twilio Verify: código para {}{} -> {}", INDICATIVO_COLOMBIA, celular, estado);
            return ESTADO_APROBADO.equals(estado);
        } catch (RestClientResponseException ex) {
            // 404: no hay un código pendiente para ese número (venció, ya se usó o nunca se envió).
            if (ex.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                LOGGER.info("Twilio Verify: no hay código vigente para {}{}", INDICATIVO_COLOMBIA, celular);
                return false;
            }
            throw rechazo("comprobar el código", celular, ex);
        } catch (ResourceAccessException ex) {
            throw new ProveedorSmsException(MENSAJE_SIN_CONEXION);
        }
    }

    /**
     * Comprueba al arrancar que el servicio de Verify existe y las credenciales sirven. Solo deja el resultado en el log.
     */
    public void verificarServicio() {
        try {
            String respuesta = restClient.get().uri(SERVICIO_URI, servicio).retrieve().body(String.class);
            LOGGER.info("Twilio Verify: servicio \"{}\" ({}) listo. Twilio genera y valida los códigos de entrega",
                    extraer(NOMBRE, respuesta, "?"), servicio);
        } catch (RestClientResponseException ex) {
            LOGGER.error("Twilio Verify RECHAZÓ la configuración ({}): {}", ex.getStatusCode().value(),
                    explicar(ex.getResponseBodyAsString(), ex.getStatusText()));
        } catch (ResourceAccessException ex) {
            LOGGER.warn("Twilio Verify: no se pudo comprobar el servicio porque no hay conexión con verify.twilio.com");
        }
    }

    private ProveedorSmsException rechazo(String accion, String celular, RestClientResponseException ex) {
        String detalle = explicar(ex.getResponseBodyAsString(), ex.getStatusText());
        LOGGER.warn("Twilio Verify no pudo {} a {}: {}", accion, celular, detalle);
        return new ProveedorSmsException(String.format(MENSAJE_RECHAZADO, accion, celular, detalle));
    }

    private String explicar(String cuerpo, String porDefecto) {
        String texto = cuerpo == null ? "" : cuerpo;
        String mensajeTwilio = extraer(MENSAJE_TWILIO, texto, porDefecto);
        Matcher codigo = CODIGO_TWILIO.matcher(texto);
        if (codigo.find()) {
            String ayuda = AYUDA_POR_CODIGO.get(Integer.parseInt(codigo.group(1)));
            if (ayuda != null) {
                return ayuda + " (Twilio " + codigo.group(1) + ": " + mensajeTwilio + ")";
            }
        }
        return mensajeTwilio;
    }

    private static String extraer(Pattern patron, String texto, String porDefecto) {
        Matcher matcher = patron.matcher(texto == null ? "" : texto);
        return matcher.find() ? matcher.group(1) : porDefecto;
    }
}
