package com.mich.mensajeria.infrastructure.config;

import com.mich.mensajeria.application.port.output.SmsHistorialPort;
import com.mich.mensajeria.application.port.output.SmsPort;
import com.mich.mensajeria.application.port.output.VerificacionPort;
import com.mich.mensajeria.application.service.SmsService;
import com.mich.mensajeria.application.usecase.SmsUseCase;
import com.mich.mensajeria.infrastructure.adapter.output.sms.AndroidSmsAdapter;
import com.mich.mensajeria.infrastructure.adapter.output.sms.LogSmsAdapter;
import com.mich.mensajeria.infrastructure.adapter.output.sms.NoConfiguradoSmsAdapter;
import com.mich.mensajeria.infrastructure.adapter.output.sms.SinVerificacionAdapter;
import com.mich.mensajeria.infrastructure.adapter.output.sms.TwilioSmsAdapter;
import com.mich.mensajeria.infrastructure.adapter.output.sms.TwilioVerifyAdapter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanConfiguration.class);
    private static final String BEARER_JWT = "bearerJwt";
    private static final String PROVEEDOR_LOG = "log";
    private static final String PROVEEDOR_ANDROID = "android";
    private static final String PROVEEDOR_TWILIO = "twilio";
    private static final String LINEA = "=".repeat(100);

    /**
     * Elige por dónde salen los SMS. Con sms.provider=auto (por defecto en local) usa el primero que esté configurado:
     * 1) un celular Android propio con la app SMS Gateway (android.*), 2) Twilio (twilio.*).
     * Si ninguno lo está, cada envío se rechaza con un error claro: nunca se pierde un PIN en silencio.
     * android o twilio fuerzan ese proveedor; log simula los envíos (solo para pruebas de desarrollo).
     */
    @Bean
    public SmsPort smsPort(@Value("${sms.provider:auto}") String proveedor,
                           @Value("${android.url:}") String androidUrl,
                           @Value("${android.usuario:}") String androidUsuario,
                           @Value("${android.clave:}") String androidClave,
                           @Value("${twilio.base-url}") String twilioBaseUrl,
                           @Value("${twilio.account-sid:}") String accountSid,
                           @Value("${twilio.auth-token:}") String authToken,
                           @Value("${twilio.from-number:}") String fromNumber,
                           @Value("${twilio.verify-service-sid:}") String verifyServiceSid) {
        if (PROVEEDOR_LOG.equals(proveedor)) {
            LOGGER.warn("SMS SIMULADOS (sms.provider=log): los PIN NO llegan al celular, solo a esta consola");
            return new LogSmsAdapter();
        }
        boolean hayAndroid = estanTodos(androidUrl, androidUsuario, androidClave);
        boolean hayTwilio = estanTodos(accountSid, authToken, fromNumber);
        if (hayAndroid && !PROVEEDOR_TWILIO.equals(proveedor)) {
            AndroidSmsAdapter android = new AndroidSmsAdapter(androidUrl.trim(), androidUsuario.trim(), androidClave.trim());
            LOGGER.info("SMS REALES activados: se envían desde tu {}", android.proveedor());
            android.verificarConexion();
            return android;
        }
        if (hayTwilio && !PROVEEDOR_ANDROID.equals(proveedor)) {
            LOGGER.info("SMS REALES activados: se envían con Twilio desde {}", fromNumber.trim());
            TwilioSmsAdapter twilio = new TwilioSmsAdapter(twilioBaseUrl, accountSid.trim(), authToken.trim(), fromNumber.trim());
            twilio.verificarCuenta();
            return twilio;
        }
        if (estanTodos(accountSid, authToken, verifyServiceSid)) {
            LOGGER.info("SMS de texto libre no configurados: los códigos de entrega se envían con Twilio Verify");
            return new NoConfiguradoSmsAdapter();
        }
        LOGGER.warn("""

                {}
                  SMS NO CONFIGURADOS: los PIN no se pueden enviar y los pedidos no pasarán a LISTO.
                  Para que lleguen al celular ejecuta:  ./mensajeria/configurar-sms.sh
                  (usa tu propio celular Android o Twilio) y reinicia.
                {}""", LINEA, LINEA);
        return new NoConfiguradoSmsAdapter();
    }

    private static boolean estanTodos(String... valores) {
        for (String valor : valores) {
            if (valor == null || valor.isBlank()) {
                return false;
            }
        }
        return true;
    }

    @Bean
    public SmsService smsService(SmsPort smsPort, SmsHistorialPort smsHistorialPort, VerificacionPort verificacionPort) {
        return new SmsUseCase(smsPort, smsHistorialPort, verificacionPort);
    }

    /**
     * Twilio Verify: si hay credenciales de Twilio y twilio.verify-service-sid, Twilio genera, envía y valida el código de
     * entrega (sirve con la cuenta gratis). Si no, el PIN de Plazoleta se envía como texto con el SmsPort.
     */
    @Bean
    public VerificacionPort verificacionPort(@Value("${sms.provider:auto}") String proveedor,
                                             @Value("${twilio.verify-base-url}") String verifyBaseUrl,
                                             @Value("${twilio.account-sid:}") String accountSid,
                                             @Value("${twilio.auth-token:}") String authToken,
                                             @Value("${twilio.verify-service-sid:}") String servicio) {
        if (PROVEEDOR_LOG.equals(proveedor) || PROVEEDOR_ANDROID.equals(proveedor)
                || !estanTodos(accountSid, authToken, servicio)) {
            return new SinVerificacionAdapter();
        }
        LOGGER.info("CÓDIGOS REALES activados: Twilio Verify genera, envía por SMS y valida el código de entrega");
        TwilioVerifyAdapter verify = new TwilioVerifyAdapter(verifyBaseUrl, accountSid.trim(), authToken.trim(), servicio.trim());
        verify.verificarServicio();
        return verify;
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
