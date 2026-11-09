package com.mich.mensajeria.infrastructure.adapter.output.sms;

import com.mich.mensajeria.application.port.output.SmsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sms.provider", havingValue = "log")
public class LogSmsAdapter implements SmsPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogSmsAdapter.class);

    @Override
    public void enviar(String celular, String mensaje) {
        LOGGER.info("SMS para {}: {}", celular, mensaje);
    }
}
