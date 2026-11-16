package com.mich.mensajeria.application.service;

import com.mich.mensajeria.domain.model.EstadoEnvio;
import com.mich.mensajeria.domain.model.Sms;

import java.util.List;

public interface SmsService {

    void enviarSms(String celular, String mensaje);

    /**
     * Envía el PIN de un pedido. Devuelve true si el proveedor generó su propio código (Twilio Verify): en ese caso el
     * PIN recibido no se usa y la entrega se comprueba con {@link #verificarPin}.
     */
    boolean enviarPin(String celular, String mensaje, String pin);

    boolean verificarPin(String celular, String codigo);

    List<Sms> listarEnviados();

    EstadoEnvio estadoEnvio();

    String proveedor();
}
