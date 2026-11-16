package com.mich.mensajeria.infrastructure.adapter.output.memoria;

import com.mich.mensajeria.application.port.output.SmsHistorialPort;
import com.mich.mensajeria.domain.model.Sms;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Guarda en memoria los últimos SMS enviados. Se pierde al reiniciar el servicio.
 */
@Component
public class MemoriaSmsHistorialAdapter implements SmsHistorialPort {

    private static final int MAXIMO_MENSAJES = 100;

    private final Deque<Sms> mensajes = new ConcurrentLinkedDeque<>();

    @Override
    public void registrar(Sms sms) {
        mensajes.addFirst(sms);
        while (mensajes.size() > MAXIMO_MENSAJES) {
            mensajes.pollLast();
        }
    }

    @Override
    public List<Sms> listarRecientes() {
        return new ArrayList<>(mensajes);
    }
}
