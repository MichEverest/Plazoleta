package com.mich.mensajeria.infrastructure.adapter.output.sms;

import com.mich.mensajeria.application.port.output.SmsPort;
import com.mich.mensajeria.domain.model.EstadoEnvio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMS de desarrollo: solo lo escribe en la consola. Únicamente se usa si se pide explícitamente con sms.provider=log.
 */
public class LogSmsAdapter implements SmsPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogSmsAdapter.class);

    @Override
    public void enviar(String celular, String mensaje) {
        LOGGER.info("SMS (simulado, sms.provider=log) para {}: {}", celular, mensaje);
    }

    @Override
    public EstadoEnvio estado() {
        return EstadoEnvio.SIMULADO;
    }

    @Override
    public String proveedor() {
        return "consola (simulado)";
    }
}
