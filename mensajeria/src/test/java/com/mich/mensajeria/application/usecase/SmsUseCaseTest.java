package com.mich.mensajeria.application.usecase;

import com.mich.mensajeria.application.port.output.SmsPort;
import com.mich.mensajeria.domain.exception.ExcepcionesDominio.ValidacionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmsUseCaseTest {

    @Mock
    private SmsPort smsPort;

    @InjectMocks
    private SmsUseCase smsUseCase;

    @Test
    void enviarSmsDelegaEnElPuerto() {
        smsUseCase.enviarSms("+573000000003", "Tu PIN es 123456");

        verify(smsPort).enviar("+573000000003", "Tu PIN es 123456");
    }

    @Test
    void enviarSmsFallaSiCelularEsNulo() {
        assertThrows(ValidacionException.class, () -> smsUseCase.enviarSms(null, "Hola"));
        verify(smsPort, never()).enviar(anyString(), anyString());
    }

    @Test
    void enviarSmsFallaSiCelularTieneLetras() {
        assertThrows(ValidacionException.class, () -> smsUseCase.enviarSms("300abc", "Hola"));
    }

    @Test
    void enviarSmsFallaSiCelularSuperaTreceCaracteres() {
        assertThrows(ValidacionException.class, () -> smsUseCase.enviarSms("+5730000000031", "Hola"));
    }

    @Test
    void enviarSmsFallaSiMensajeEstaVacio() {
        assertThrows(ValidacionException.class, () -> smsUseCase.enviarSms("+573000000003", " "));
        verify(smsPort, never()).enviar(anyString(), anyString());
    }
}
