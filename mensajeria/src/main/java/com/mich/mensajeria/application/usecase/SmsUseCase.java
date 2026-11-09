package com.mich.mensajeria.application.usecase;

import com.mich.mensajeria.application.port.output.SmsPort;
import com.mich.mensajeria.application.service.SmsService;
import com.mich.mensajeria.domain.exception.ExcepcionesDominio.ValidacionException;

import java.util.regex.Pattern;

public class SmsUseCase implements SmsService {

    private static final Pattern PATRON_CELULAR = Pattern.compile("^\\+?\\d{1,12}$|^\\d{13}$");
    private static final String MENSAJE_CELULAR_INVALIDO = "El celular debe ser numérico, de máximo 13 caracteres y puede iniciar con +";
    private static final String MENSAJE_TEXTO_OBLIGATORIO = "El mensaje es obligatorio";

    private final SmsPort smsPort;

    public SmsUseCase(SmsPort smsPort) {
        this.smsPort = smsPort;
    }

    @Override
    public void enviarSms(String celular, String mensaje) {
        if (celular == null || !PATRON_CELULAR.matcher(celular).matches()) {
            throw new ValidacionException(MENSAJE_CELULAR_INVALIDO);
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new ValidacionException(MENSAJE_TEXTO_OBLIGATORIO);
        }
        smsPort.enviar(celular, mensaje);
    }
}
