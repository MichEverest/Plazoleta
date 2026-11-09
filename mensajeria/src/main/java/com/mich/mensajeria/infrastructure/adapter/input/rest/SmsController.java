package com.mich.mensajeria.infrastructure.adapter.input.rest;

import com.mich.mensajeria.application.service.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mensajes")
@Tag(name = "Mensajes")
public class SmsController {

    public record SmsRequest(String celular, String mensaje) {
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
}
