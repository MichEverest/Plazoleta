package com.mich.mensajeria.application.usecase;

import com.mich.mensajeria.application.port.output.SmsHistorialPort;
import com.mich.mensajeria.application.port.output.SmsPort;
import com.mich.mensajeria.application.port.output.VerificacionPort;
import com.mich.mensajeria.application.service.SmsService;
import com.mich.mensajeria.domain.exception.ExcepcionesDominio.ValidacionException;
import com.mich.mensajeria.domain.model.EstadoEnvio;
import com.mich.mensajeria.domain.model.Sms;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

public class SmsUseCase implements SmsService {

    private static final Pattern PATRON_CELULAR = Pattern.compile("^3\\d{9}$");
    private static final String MENSAJE_CELULAR_INVALIDO = "El celular debe ser un número colombiano de 10 dígitos que empiece por 3, sin +57 (ejemplo: 3023066544)";
    private static final String MENSAJE_TEXTO_OBLIGATORIO = "El mensaje es obligatorio";

    private static final String MENSAJE_PIN_OBLIGATORIO = "El PIN es obligatorio";
    private static final String MENSAJE_CODIGO_OBLIGATORIO = "El código es obligatorio";
    private static final String HISTORIAL_CODIGO_VERIFY = "Código de entrega generado y enviado por %s (el texto y el código los pone Twilio)";

    private final SmsPort smsPort;
    private final SmsHistorialPort smsHistorialPort;
    private final VerificacionPort verificacionPort;

    public SmsUseCase(SmsPort smsPort, SmsHistorialPort smsHistorialPort, VerificacionPort verificacionPort) {
        this.smsPort = smsPort;
        this.smsHistorialPort = smsHistorialPort;
        this.verificacionPort = verificacionPort;
    }

    @Override
    public boolean enviarPin(String celular, String mensaje, String pin) {
        validarCelular(celular);
        if (verificacionPort.disponible()) {
            verificacionPort.enviarCodigo(celular);
            smsHistorialPort.registrar(new Sms(celular, String.format(HISTORIAL_CODIGO_VERIFY, verificacionPort.proveedor()),
                    LocalDateTime.now()));
            return true;
        }
        if (pin == null || pin.isBlank()) {
            throw new ValidacionException(MENSAJE_PIN_OBLIGATORIO);
        }
        enviarSms(celular, mensaje);
        return false;
    }

    @Override
    public boolean verificarPin(String celular, String codigo) {
        validarCelular(celular);
        if (codigo == null || codigo.isBlank()) {
            throw new ValidacionException(MENSAJE_CODIGO_OBLIGATORIO);
        }
        return verificacionPort.disponible() && verificacionPort.comprobarCodigo(celular, codigo.trim());
    }

    private void validarCelular(String celular) {
        if (celular == null || !PATRON_CELULAR.matcher(celular).matches()) {
            throw new ValidacionException(MENSAJE_CELULAR_INVALIDO);
        }
    }

    @Override
    public void enviarSms(String celular, String mensaje) {
        validarCelular(celular);
        if (mensaje == null || mensaje.isBlank()) {
            throw new ValidacionException(MENSAJE_TEXTO_OBLIGATORIO);
        }
        smsPort.enviar(celular, mensaje);
        smsHistorialPort.registrar(new Sms(celular, mensaje, LocalDateTime.now()));
    }

    @Override
    public EstadoEnvio estadoEnvio() {
        return verificacionPort.disponible() ? EstadoEnvio.REAL : smsPort.estado();
    }

    @Override
    public String proveedor() {
        return verificacionPort.disponible() ? verificacionPort.proveedor() : smsPort.proveedor();
    }

    @Override
    public List<Sms> listarEnviados() {
        return smsHistorialPort.listarRecientes();
    }
}
