package com.mich.mensajeria.application.usecase;

import com.mich.mensajeria.application.port.output.SmsHistorialPort;
import com.mich.mensajeria.application.port.output.SmsPort;
import com.mich.mensajeria.application.port.output.VerificacionPort;
import com.mich.mensajeria.domain.exception.ExcepcionesDominio.SmsNoConfiguradoException;
import com.mich.mensajeria.domain.exception.ExcepcionesDominio.ValidacionException;
import com.mich.mensajeria.domain.model.EstadoEnvio;
import com.mich.mensajeria.domain.model.Sms;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsUseCaseTest {

    @Mock
    private SmsPort smsPort;

    @Mock
    private SmsHistorialPort smsHistorialPort;

    @Mock
    private VerificacionPort verificacionPort;

    @InjectMocks
    private SmsUseCase smsUseCase;

    @Test
    void enviarSmsDelegaEnElPuerto() {
        smsUseCase.enviarSms("3000000003", "Tu PIN es 123456");

        verify(smsPort).enviar("3000000003", "Tu PIN es 123456");
    }

    @Test
    void enviarSmsRegistraElMensajeEnElHistorial() {
        smsUseCase.enviarSms("3000000003", "Tu PIN es 123456");

        ArgumentCaptor<Sms> captor = ArgumentCaptor.forClass(Sms.class);
        verify(smsHistorialPort).registrar(captor.capture());
        assertEquals("3000000003", captor.getValue().celular());
        assertEquals("Tu PIN es 123456", captor.getValue().mensaje());
        assertNotNull(captor.getValue().fecha());
    }

    @Test
    void enviarSmsInvalidoNoSeRegistraEnElHistorial() {
        assertThrows(ValidacionException.class, () -> smsUseCase.enviarSms("300abc", "Hola"));
        verify(smsHistorialPort, never()).registrar(any());
    }

    @Test
    void estadoEnvioDependeDelProveedor() {
        when(smsPort.estado()).thenReturn(EstadoEnvio.NO_CONFIGURADO);

        assertEquals(EstadoEnvio.NO_CONFIGURADO, smsUseCase.estadoEnvio());
    }

    @Test
    void enviarSmsSinProveedorConfiguradoPropagaElErrorYNoLoRegistra() {
        doThrow(new SmsNoConfiguradoException("sin Twilio")).when(smsPort).enviar(anyString(), anyString());

        assertThrows(SmsNoConfiguradoException.class, () -> smsUseCase.enviarSms("3023066544", "Tu PIN es 123456"));
        verify(smsHistorialPort, never()).registrar(any());
    }

    @Test
    void listarEnviadosDevuelveElHistorial() {
        List<Sms> historial = List.of(new Sms("3000000003", "Hola", LocalDateTime.now()));
        when(smsHistorialPort.listarRecientes()).thenReturn(historial);

        assertEquals(historial, smsUseCase.listarEnviados());
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
    void enviarSmsFallaSiCelularIncluyeIndicativo() {
        assertThrows(ValidacionException.class, () -> smsUseCase.enviarSms("+573000000003", "Hola"));
    }

    @Test
    void enviarSmsFallaSiCelularNoTieneDiezDigitos() {
        assertThrows(ValidacionException.class, () -> smsUseCase.enviarSms("30000000031", "Hola"));
    }

    @Test
    void enviarSmsFallaSiMensajeEstaVacio() {
        assertThrows(ValidacionException.class, () -> smsUseCase.enviarSms("3000000003", " "));
        verify(smsPort, never()).enviar(anyString(), anyString());
    }

    @Test
    void enviarPinSinVerificadorEnviaElTextoConElPinPropio() {
        when(verificacionPort.disponible()).thenReturn(false);

        assertFalse(smsUseCase.enviarPin("3023066544", "Tu PIN es 123456", "123456"));
        verify(smsPort).enviar("3023066544", "Tu PIN es 123456");
        verify(verificacionPort, never()).enviarCodigo(anyString());
    }

    @Test
    void enviarPinConTwilioVerifyPideElCodigoAlProveedorYNoEnviaElTexto() {
        when(verificacionPort.disponible()).thenReturn(true);
        when(verificacionPort.proveedor()).thenReturn("Twilio Verify");

        assertTrue(smsUseCase.enviarPin("3023066544", "Tu PIN es 123456", "123456"));
        verify(verificacionPort).enviarCodigo("3023066544");
        verify(smsPort, never()).enviar(anyString(), anyString());
        verify(smsHistorialPort).registrar(any());
    }

    @Test
    void verificarPinConsultaAlProveedor() {
        when(verificacionPort.disponible()).thenReturn(true);
        when(verificacionPort.comprobarCodigo("3023066544", "918273")).thenReturn(true);

        assertTrue(smsUseCase.verificarPin("3023066544", " 918273 "));
    }

    @Test
    void verificarPinSinVerificadorDevuelveFalso() {
        when(verificacionPort.disponible()).thenReturn(false);

        assertFalse(smsUseCase.verificarPin("3023066544", "918273"));
    }

    @Test
    void enviarPinFallaConCelularInvalido() {
        assertThrows(ValidacionException.class, () -> smsUseCase.enviarPin("+573023066544", "hola", "123456"));
        verify(verificacionPort, never()).enviarCodigo(anyString());
    }

    @Test
    void estadoEnvioEsRealSiHayVerificador() {
        when(verificacionPort.disponible()).thenReturn(true);

        assertEquals(EstadoEnvio.REAL, smsUseCase.estadoEnvio());
    }
}
