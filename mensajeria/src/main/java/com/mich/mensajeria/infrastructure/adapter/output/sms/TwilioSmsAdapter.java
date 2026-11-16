package com.mich.mensajeria.infrastructure.adapter.output.sms;

import com.mich.mensajeria.application.port.output.SmsPort;
import com.mich.mensajeria.domain.exception.ExcepcionesDominio.ProveedorSmsException;
import com.mich.mensajeria.domain.model.EstadoEnvio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * SMS real con la API REST de Twilio. El celular llega con 10 dígitos y aquí se le agrega el indicativo +57,
 * porque Twilio exige el formato internacional. Los errores de Twilio se traducen a instrucciones en español.
 */
public class TwilioSmsAdapter implements SmsPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwilioSmsAdapter.class);
    private static final String MESSAGES_URI = "/2010-04-01/Accounts/{accountSid}/Messages.json";
    private static final String ACCOUNT_URI = "/2010-04-01/Accounts/{accountSid}.json";
    private static final String INDICATIVO_COLOMBIA = "+57";
    private static final Pattern MENSAJE_TWILIO = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern CODIGO_TWILIO = Pattern.compile("\"code\"\\s*:\\s*(\\d+)");
    private static final Pattern NOMBRE_CUENTA = Pattern.compile("\"friendly_name\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern TIPO_CUENTA = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]*)\"");
    private static final Map<Integer, String> AYUDA_POR_CODIGO = Map.of(
            20003, "las credenciales de Twilio no son válidas: revisa twilio.account-sid y twilio.auth-token",
            21211, "Twilio no acepta ese número como destino",
            21408, "tu cuenta de Twilio no tiene permiso para enviar a Colombia: actívalo en Messaging > Settings > Geo permissions",
            21606, "el número remitente (twilio.from-number) no puede enviar SMS: usa un número de tu cuenta de Twilio con SMS",
            21608, "tu cuenta de Twilio es de prueba y ese número no está verificado: verifícalo en Phone Numbers > "
                    + "Verified Caller IDs o activa la cuenta (Upgrade)",
            21610, "el destinatario bloqueó los SMS de tu número de Twilio (respondió STOP)",
            21659, "el número remitente (twilio.from-number) no pertenece a tu cuenta de Twilio",
            572006, "tu cuenta de Twilio es de prueba (trial) y solo permite plantillas fijas de Twilio, sin texto propio "
                    + "como el PIN: activa la cuenta con pago (Upgrade) o usa otro proveedor");
    private static final String MENSAJE_RECHAZADO = "Twilio no pudo enviar el SMS a %s: %s";
    private static final String MENSAJE_SIN_CONEXION = "No se pudo conectar con Twilio para enviar el SMS. Revisa tu conexión a internet";

    private final RestClient restClient;
    private final String accountSid;
    private final String fromNumber;

    public TwilioSmsAdapter(String baseUrl, String accountSid, String authToken, String fromNumber) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(accountSid, authToken))
                .build();
        this.accountSid = accountSid;
        this.fromNumber = fromNumber;
    }

    @Override
    public void enviar(String celular, String mensaje) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", INDICATIVO_COLOMBIA + celular);
        form.add("From", fromNumber);
        form.add("Body", mensaje);
        try {
            restClient.post()
                    .uri(MESSAGES_URI, accountSid)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            LOGGER.info("SMS enviado por Twilio a {}{}", INDICATIVO_COLOMBIA, celular);
        } catch (RestClientResponseException ex) {
            String detalle = explicar(ex.getResponseBodyAsString(), ex.getStatusText());
            LOGGER.warn("Twilio rechazó el SMS a {}: {}", celular, detalle);
            throw new ProveedorSmsException(String.format(MENSAJE_RECHAZADO, celular, detalle));
        } catch (ResourceAccessException ex) {
            LOGGER.warn(MENSAJE_SIN_CONEXION, ex);
            throw new ProveedorSmsException(MENSAJE_SIN_CONEXION);
        }
    }

    @Override
    public EstadoEnvio estado() {
        return EstadoEnvio.REAL;
    }

    /**
     * Comprueba al arrancar que Twilio acepta las credenciales, para detectar errores antes del primer pedido.
     * No detiene el servicio: solo deja el resultado en el log.
     */
    public void verificarCuenta() {
        try {
            String cuenta = restClient.get().uri(ACCOUNT_URI, accountSid).retrieve().body(String.class);
            LOGGER.info("Twilio: credenciales válidas (cuenta \"{}\", tipo {}). Remitente {}",
                    extraer(NOMBRE_CUENTA, cuenta, "?"), extraer(TIPO_CUENTA, cuenta, "?"), fromNumber);
            if ("Trial".equalsIgnoreCase(extraer(TIPO_CUENTA, cuenta, ""))) {
                LOGGER.info("Twilio: cuenta de prueba. Solo llegan SMS a los números verificados en Phone Numbers > Verified Caller IDs");
            }
        } catch (RestClientResponseException ex) {
            LOGGER.error("Twilio RECHAZÓ las credenciales ({}): {}. Los SMS no llegarán hasta corregir mensajeria/sms.properties",
                    ex.getStatusCode().value(), explicar(ex.getResponseBodyAsString(), ex.getStatusText()));
        } catch (ResourceAccessException ex) {
            LOGGER.warn("Twilio: no se pudo verificar la cuenta porque no hay conexión con api.twilio.com");
        }
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

    @Override
    public String proveedor() {
        return "Twilio";
    }
}
