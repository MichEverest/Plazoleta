package com.mich.mensajeria.infrastructure.adapter.input.rest;

import com.mich.mensajeria.application.service.SmsService;
import com.mich.mensajeria.domain.model.EstadoEnvio;
import com.mich.mensajeria.domain.model.Sms;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/mensajes")
@Tag(name = "Mensajes")
public class SmsController {

    public record SmsRequest(String celular, String mensaje) {
    }

    public record SmsResponse(String celular, String mensaje, LocalDateTime fecha) {

        static SmsResponse de(Sms sms) {
            return new SmsResponse(sms.celular(), sms.mensaje(), sms.fecha());
        }
    }

    private final SmsService smsService;

    public SmsController(SmsService smsService) {
        this.smsService = smsService;
    }

    @Operation(summary = "Enviar SMS (autenticado)")
    @PostMapping("/sms")
    public ResponseEntity<Void> enviarSms(@RequestBody SmsRequest request) {
        smsService.enviarSms(request.celular(), request.mensaje());
        return ResponseEntity.noContent().build();
    }

    public record PinRequest(String celular, String mensaje, String pin) {
    }

    public record PinResponse(boolean verificacionExterna) {
    }

    public record VerificacionRequest(String celular, String codigo) {
    }

    public record VerificacionResponse(boolean valido) {
    }

    @Operation(summary = "Enviar el PIN de un pedido (uso interno de Plazoleta). verificacionExterna=true si el código lo genera el proveedor (Twilio Verify)")
    @PostMapping("/pin")
    public ResponseEntity<PinResponse> enviarPin(@RequestBody PinRequest request) {
        return ResponseEntity.ok(new PinResponse(smsService.enviarPin(request.celular(), request.mensaje(), request.pin())));
    }

    @Operation(summary = "Comprobar con el proveedor el código que dio el cliente (uso interno de Plazoleta)")
    @PostMapping("/pin/verificacion")
    public ResponseEntity<VerificacionResponse> verificarPin(@RequestBody VerificacionRequest request) {
        return ResponseEntity.ok(new VerificacionResponse(smsService.verificarPin(request.celular(), request.codigo())));
    }

    public record ModoResponse(EstadoEnvio estado, boolean real, String proveedor, String descripcion) {
    }

    @Operation(summary = "Estado del envío de SMS: REAL, NO_CONFIGURADO o SIMULADO (autenticado)")
    @GetMapping("/modo")
    public ResponseEntity<ModoResponse> consultarModo() {
        EstadoEnvio estado = smsService.estadoEnvio();
        String descripcion = switch (estado) {
            case REAL -> "Twilio Verify".equals(smsService.proveedor())
                    ? "Los códigos de entrega los genera y envía Twilio Verify por SMS real (vencen a los 10 minutos)"
                    : "Los SMS se envían de verdad desde " + smsService.proveedor() + ": el PIN llega al celular del cliente";
            case NO_CONFIGURADO -> "El envío de SMS no está configurado: no se puede enviar ningún PIN y los pedidos no pasarán a LISTO. "
                    + "Ejecuta ./mensajeria/configurar-sms.sh (con tu celular Android o con Twilio) y reinicia";
            case SIMULADO -> "Modo de desarrollo (sms.provider=log): los SMS solo se escriben en la terminal, no llegan al celular";
        };
        return ResponseEntity.ok(new ModoResponse(estado, estado == EstadoEnvio.REAL, smsService.proveedor(), descripcion));
    }

    @Operation(summary = "Últimos SMS enviados, el más reciente primero (ADMINISTRADOR)")
    @GetMapping("/sms")
    public ResponseEntity<List<SmsResponse>> listarEnviados() {
        return ResponseEntity.ok(smsService.listarEnviados().stream().map(SmsResponse::de).toList());
    }
}
