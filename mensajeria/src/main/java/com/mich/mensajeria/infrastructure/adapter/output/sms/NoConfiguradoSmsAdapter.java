package com.mich.mensajeria.infrastructure.adapter.output.sms;

import com.mich.mensajeria.application.port.output.SmsPort;
import com.mich.mensajeria.domain.exception.ExcepcionesDominio.SmsNoConfiguradoException;
import com.mich.mensajeria.domain.model.EstadoEnvio;

/**
 * Se usa cuando falta configurar Twilio. Rechaza cada envío con un error claro en lugar de descartar el SMS:
 * así un PIN nunca se pierde en silencio y el pedido no pasa a LISTO sin que el cliente lo reciba.
 */
public class NoConfiguradoSmsAdapter implements SmsPort {

    private static final String MENSAJE = "No se pudo enviar el SMS a %s: el envío de SMS no está configurado. "
            + "Ejecuta ./mensajeria/configurar-sms.sh (con tu propio celular Android o con Twilio) y reinicia los servicios";

    @Override
    public void enviar(String celular, String mensaje) {
        throw new SmsNoConfiguradoException(String.format(MENSAJE, celular));
    }

    @Override
    public EstadoEnvio estado() {
        return EstadoEnvio.NO_CONFIGURADO;
    }

    @Override
    public String proveedor() {
        return "ninguno";
    }
}
