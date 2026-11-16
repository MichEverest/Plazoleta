package com.mich.mensajeria.domain.model;

import java.time.LocalDateTime;

public record Sms(String celular, String mensaje, LocalDateTime fecha) {
}
