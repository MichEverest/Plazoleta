# Microservicio Mensajería — Plazoleta de comidas

Envía SMS. Lo usa Plazoleta para enviar al cliente el PIN de su pedido listo (HU14). Puerto `8084`, sin base de datos. Ver el [README general](../README.md).

## Ejecución

```bash
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
./mvnw test
```

En NetBeans 21+ usa *Run* (`nbactions.xml`). Consola web: http://localhost:8084 · Swagger: http://localhost:8084/swagger-ui.html

## SMS reales

El PIN **siempre** se intenta enviar de verdad; no hay modo simulado por defecto. El celular llega con 10 dígitos sin indicativo (`3023066544`) y el adaptador le agrega `+57`. Hay dos formas de enviar:

| Proveedor | De dónde sale el SMS | Qué necesitas |
|-----------|----------------------|---------------|
| **Celular Android propio** (`AndroidSmsAdapter`) | De la SIM de tu celular, con tu número y tu plan | La app gratuita *SMS Gateway for Android* ([sms-gate.app](https://sms-gate.app)), Android 5+. Modo local (PC y celular en la misma Wi-Fi, `http://<ip>:8080`) o nube (`https://api.sms-gate.app/3rdparty/v1`) |
| **Twilio con texto propio** (`TwilioSmsAdapter`) | De un número de Twilio, con nuestro texto (restaurante, PIN, teléfono) | Cuenta de Twilio **activada con pago**: la cuenta gratis solo permite plantillas fijas (error 572006) |
| **Twilio Verify** (`TwilioVerifyAdapter`) | De Twilio; **Twilio genera el código**, lo envía y lo valida | Cuenta de Twilio **gratis** basta (hacia números verificados). El texto lo pone Twilio y el código vence a los 10 minutos |

**Configurarlo (una vez):** desde la raíz del proyecto ejecuta

```bash
./mensajeria/configurar-sms.sh
```

Pregunta qué proveedor usar, pide los datos, **comprueba la conexión antes de guardar** en `mensajeria/sms.properties` (en `.gitignore`, permisos 600) y envía un SMS de prueba. También se puede llenar a mano (ver `sms.properties.example`); para Twilio también sirven las variables `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` y `TWILIO_FROM_NUMBER`.

Con `sms.provider=auto` (por defecto en `local`) se usa el celular Android si está configurado y, si no, Twilio. Si además está `twilio.verify-service-sid`, el **PIN de los pedidos** va por Twilio Verify: `POST /mensajes/pin` responde `verificacionExterna=true`, Plazoleta no guarda PIN propio y en la entrega pregunta a `POST /mensajes/pin/verificacion` si el código es válido. El script `configurar-sms.sh` (opción 3) crea el servicio Verify y prueba un código real. `android` o `twilio` fuerzan uno; `log` simula los envíos (solo para desarrollo).

| Estado (`GET /mensajes/modo`) | Cuándo | Qué pasa al marcar un pedido listo |
|-------------------------------|--------|------------------------------------|
| `REAL` | Hay un proveedor configurado | El SMS sale hacia el celular del cliente. Al arrancar se verifica la conexión (Android local) o la cuenta (Twilio) |
| `NO_CONFIGURADO` | No hay proveedor | Se rechaza con `503` y un mensaje claro; el pedido sigue EN_PREPARACION. Nunca se pierde un PIN en silencio |
| `SIMULADO` | Solo con `SMS_PROVIDER=log` | El SMS solo se escribe en la consola |

Si el envío falla (celular apagado o fuera de la Wi-Fi, clave de la app incorrecta, número no verificado en Twilio, Colombia sin permiso en *Geo permissions*…) responde `502` con el motivo en español y Plazoleta deja el pedido en EN_PREPARACION para reintentar. Si el pedido ya está LISTO y al cliente no le llegó el SMS, el empleado puede usar **Reenviar PIN**.

`JWT_SECRET` es obligatorio en `prod`.

## Endpoints

| Método | Ruta | Acceso | Descripción |
|--------|------|--------|-------------|
| POST | `/mensajes/sms` | Autenticado | Cuerpo `{celular, mensaje}`. Responde `204`. Valida el celular: 10 dígitos que empiezan por 3, sin `+57` |
| POST | `/mensajes/pin` | Autenticado | Uso interno de Plazoleta: envía el PIN de un pedido `{celular, mensaje, pin}` y responde `{verificacionExterna}` |
| POST | `/mensajes/pin/verificacion` | Autenticado | Uso interno de Plazoleta: `{celular, codigo}` → `{valido}` (Twilio Verify) |
| GET | `/mensajes/modo` | Autenticado | `{estado, real, descripcion}`: `REAL`, `NO_CONFIGURADO` o `SIMULADO` |
| GET | `/mensajes/sms` | ADMINISTRADOR | Últimos 100 SMS enviados `{celular, mensaje, fecha}`, el más reciente primero |

## Historial

Cada SMS enviado se guarda en un historial **en memoria** (`MemoriaSmsHistorialAdapter`, máximo 100). No hay base de datos, así que el historial se reinicia con el servicio.

## Consola web

`src/main/resources/front/`, servida en http://localhost:8084. El login se hace contra Usuarios (8081). Muestra en verde o rojo si los SMS están configurados. Cualquier usuario puede enviar un SMS de prueba. El ADMINISTRADOR (`mich@gmail.com`) ve el historial con una columna que extrae el PIN, así no hace falta buscarlo en la terminal para entregar un pedido.

## Arquitectura

`domain` (ExcepcionesDominio, Sms, EstadoEnvio) → `application` (SmsService, SmsUseCase, port/output/SmsPort y SmsHistorialPort) → `infrastructure` (SmsController, adaptadores `adapter/output/sms` (Android, Twilio, NoConfigurado, Log) y `adapter/output/memoria`, JwtService, config).

