package com.mich.mensajeria.application.port.output;

import com.mich.mensajeria.domain.model.Sms;

import java.util.List;

public interface SmsHistorialPort {

    void registrar(Sms sms);

    List<Sms> listarRecientes();
}
