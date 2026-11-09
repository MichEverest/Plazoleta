package com.mich.mensajeria.application.service;

public interface SmsService {

    void enviarSms(String celular, String mensaje);
}
