# Microservicio Mensajería — Plazoleta de comidas

Envía SMS. Lo usa Plazoleta para enviar al cliente el PIN de su pedido listo (HU14). Puerto `8084`, sin base de datos. Ver el [README general](../README.md).

## Ejecución

```bash
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
./mvnw test
```

En NetBeans 21+ usa *Run* (`nbactions.xml`). Swagger: http://localhost:8084/swagger-ui.html

## Proveedor de SMS (`sms.provider`)

| Perfil | Proveedor | Comportamiento |
|--------|-----------|----------------|
| `local` (por defecto) | `log` | `LogSmsAdapter` imprime el SMS en la consola: `SMS para +57...: Tu pedido #1 está listo...` |
| `prod` | `twilio` | `TwilioSmsAdapter` llama a la API REST de Twilio con `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER` |

`JWT_SECRET` es obligatorio en `prod`.

## Endpoint

| Método | Ruta | Acceso | Descripción |
|--------|------|--------|-------------|
| POST | `/mensajes/sms` | Autenticado | Cuerpo `{celular, mensaje}`. Responde `204`. Valida el celular (numérico, máximo 13 caracteres, `+` opcional) |

## Arquitectura

`domain/exception` (ExcepcionesDominio) → `application` (SmsService, SmsUseCase, port/output/SmsPort) → `infrastructure` (SmsController, adaptadores `adapter/output/sms`, JwtService, config).

