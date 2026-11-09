package com.mich.mensajeria.application.port.output;

public interface SmsPort {

    void enviar(String celular, String mensaje);
}
