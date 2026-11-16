#!/usr/bin/env bash
# Configura el envío real de SMS de Mensajería y lo verifica antes de guardar en mensajeria/sms.properties.
# Opciones:
#   1) Tu propio celular Android con la app gratuita "SMS Gateway for Android" (https://sms-gate.app):
#      los SMS salen de tu SIM, con tu número y tu plan.
#   2) Twilio (https://www.twilio.com): los SMS salen de un número de Twilio.
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
ARCHIVO="$DIR/sms.properties"
API_TWILIO="${TWILIO_BASE_URL:-https://api.twilio.com}"
API_VERIFY="${TWILIO_VERIFY_BASE_URL:-https://verify.twilio.com}"
NUBE_ANDROID="https://api.sms-gate.app/3rdparty/v1"
RESPUESTA="$(mktemp)"
trap 'rm -f "$RESPUESTA"' EXIT

campo() { grep -oE "\"$1\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" "$RESPUESTA" | head -1 | sed -E 's/.*:[[:space:]]*"(.*)"/\1/'; }
limpiar() { echo "$1" | tr -d '[:space:]'; }

guardar() {
    umask 077
    {
        echo "# Configuración de SMS generada por configurar-sms.sh el $(date '+%Y-%m-%d %H:%M'). No se sube a git."
        cat
    } > "$ARCHIVO"
    echo "Guardado en $ARCHIVO"
}

pedir_celular_prueba() {
    read -rp "Celular para un SMS de prueba, 10 dígitos sin +57 (Enter para omitir): " CELULAR
    CELULAR="$(limpiar "$CELULAR")"
    if [[ -n "$CELULAR" && ! "$CELULAR" =~ ^3[0-9]{9}$ ]]; then
        echo "El celular debe tener 10 dígitos y empezar por 3. Se omite la prueba."
        CELULAR=""
    fi
}

configurar_android() {
    echo
    echo "Celular Android con SMS Gateway"
    echo "1. Instala en tu celular la app \"SMS Gateway for Android\" (https://sms-gate.app o su GitHub capcom6/android-sms-gateway)"
    echo "   y dale permiso de enviar SMS."
    echo "2. Elige el modo:"
    echo "   [L] Local: el PC y el celular en la misma Wi-Fi. En la app activa \"Local Server\" y pulsa Start."
    echo "   [N] Nube: funciona en cualquier red. En la app activa \"Cloud Server\" y pulsa Start."
    read -rp "Modo (L/N): " MODO
    MODO="$(limpiar "$MODO" | tr '[:lower:]' '[:upper:]')"
    if [[ "$MODO" == "N" ]]; then
        URL="$NUBE_ANDROID"
        echo "Copia el usuario y la clave de la sección Cloud Server de la app."
    else
        read -rp "IP del celular que muestra la app (ej. 192.168.1.50): " IP
        IP="$(limpiar "$IP")"
        URL="http://${IP}:8080"
        [[ "$IP" == http* ]] && URL="$IP"
        echo "Copia el usuario y la clave de la sección Local Server de la app."
    fi
    URL="${ANDROID_URL_PRUEBA:-$URL}"
    read -rp "Usuario: " USUARIO
    read -rsp "Clave (no se muestra al escribir): " CLAVE; echo
    USUARIO="$(limpiar "$USUARIO")"; CLAVE="$(limpiar "$CLAVE")"

    if [[ "$URL" != *"/3rdparty/"* ]]; then
        echo "Comprobando que el celular responde en $URL ..."
        CODIGO="$(curl -s -m 8 -o "$RESPUESTA" -w '%{http_code}' -u "$USUARIO:$CLAVE" "$URL/health" || echo 000)"
        if [[ "$CODIGO" == "000" ]]; then
            echo "Error: no hay conexión con el celular en $URL."
            echo "Revisa que la app esté abierta con el servidor local iniciado y que el PC y el celular estén en la misma Wi-Fi. No se guardó nada."
            exit 1
        fi
        echo "El celular responde (HTTP $CODIGO)."
    fi

    pedir_celular_prueba
    if [[ -n "$CELULAR" ]]; then
        RUTA="/message"; [[ "$URL" == *"/3rdparty/"* ]] && RUTA="/messages"
        CODIGO="$(curl -s -m 20 -o "$RESPUESTA" -w '%{http_code}' -u "$USUARIO:$CLAVE" -H 'Content-Type: application/json' \
            -d "{\"textMessage\":{\"text\":\"Plazoleta: prueba de SMS. Si lees esto, los PIN de los pedidos te van a llegar.\"},\"phoneNumbers\":[\"+57$CELULAR\"]}" \
            "$URL$RUTA" || echo 000)"
        case "$CODIGO" in
            2??) echo "El celular aceptó el SMS de prueba para +57$CELULAR. Debería llegar en unos segundos." ;;
            401) echo "Error: la app rechazó el usuario o la clave. No se guardó nada."; exit 1 ;;
            000) echo "Error: no hubo respuesta al enviar. Revisa la conexión. No se guardó nada."; exit 1 ;;
            *)   echo "Error: la app respondió HTTP $CODIGO: $(cat "$RESPUESTA"). No se guardó nada."; exit 1 ;;
        esac
    else
        echo "Aviso: sin SMS de prueba no se puede confirmar el usuario y la clave; se guardan igual."
    fi

    guardar <<PROPIEDADES
android.url=$URL
android.usuario=$USUARIO
android.clave=$CLAVE
PROPIEDADES
}

configurar_twilio() {
    echo
    echo "Twilio: copia los datos de https://console.twilio.com (sección Account Info)."
    read -rp "Account SID (empieza por AC): " SID
    read -rsp "Auth Token (no se muestra al escribir): " TOKEN; echo
    echo "Número que ENVÍA: el que te dio Twilio (en la consola aparece como \"Desde\" / \"From\", ej. +17372508034)."
    echo "No es tu celular: tu celular se pide al final, para el SMS de prueba."
    read -rp "Número de Twilio que envía, con + (ej. +15017122661): " FROM
    SID="$(limpiar "$SID")"; TOKEN="$(limpiar "$TOKEN")"; FROM="$(limpiar "$FROM")"
    if [[ "$FROM" =~ ^(\+?57)?3[0-9]{9}$ ]]; then
        echo "Error: $FROM es un celular colombiano (el que RECIBE). Aquí va el número de Twilio que ENVÍA,"
        echo "el que aparece como \"Desde\" en la consola de Twilio (ej. +17372508034). No se guardó nada."
        exit 1
    fi
    if [[ ! "$FROM" =~ ^\+[0-9]{8,15}$ ]]; then
        echo "Error: el número remitente debe ir con + y solo dígitos (ej. +15017122661). No se guardó nada."
        exit 1
    fi
    echo "Verificando las credenciales con Twilio..."
    CODIGO="$(curl -s -m 15 -o "$RESPUESTA" -w '%{http_code}' -u "$SID:$TOKEN" "$API_TWILIO/2010-04-01/Accounts/$SID.json" || echo 000)"
    if [[ "$CODIGO" == "000" ]]; then
        echo "Error: no hay conexión con Twilio. Revisa tu internet. No se guardó nada."; exit 1
    fi
    if [[ "$CODIGO" != "200" ]]; then
        echo "Error: Twilio rechazó las credenciales (HTTP $CODIGO): $(campo message). No se guardó nada."; exit 1
    fi
    TIPO="$(campo type)"
    echo "Credenciales correctas. Cuenta: \"$(campo friendly_name)\" (tipo $TIPO)."
    guardar <<PROPIEDADES
twilio.account-sid=$SID
twilio.auth-token=$TOKEN
twilio.from-number=$FROM
PROPIEDADES
    if [[ "$TIPO" == "Trial" ]]; then
        echo "Cuenta de prueba: solo llegan SMS a números verificados en Twilio (Phone Numbers > Verified Caller IDs)."
    fi
    pedir_celular_prueba
    if [[ -n "$CELULAR" ]]; then
        CODIGO="$(curl -s -m 20 -o "$RESPUESTA" -w '%{http_code}' -u "$SID:$TOKEN" "$API_TWILIO/2010-04-01/Accounts/$SID/Messages.json" \
            --data-urlencode "To=+57$CELULAR" --data-urlencode "From=$FROM" \
            --data-urlencode "Body=Plazoleta: prueba de SMS. Si lees esto, los PIN de los pedidos te van a llegar." || echo 000)"
        if [[ "$CODIGO" == "201" ]]; then
            echo "SMS de prueba enviado a +57$CELULAR."
        else
            echo "Twilio no envió el SMS de prueba (HTTP $CODIGO): $(campo message)"
            if grep -q '572006' "$RESPUESTA"; then
                echo "Motivo: las cuentas de prueba de Twilio solo envían plantillas fijas, sin texto propio como el PIN."
                echo "Para enviar el PIN hay que activar la cuenta con pago (Upgrade) o usar otro proveedor."
            fi
            echo "Revisa: número no verificado (cuenta de prueba) o Colombia sin permiso en Messaging > Settings > Geo permissions."
        fi
    fi
}

configurar_verify() {
    echo
    echo "Twilio Verify (sirve con la cuenta GRATIS): Twilio genera el código, lo envía por SMS y lo valida."
    echo "El texto del SMS lo pone Twilio y el código vence a los 10 minutos (en la app: botón Reenviar PIN)."
    SID=""; TOKEN=""
    if [[ -f "$ARCHIVO" ]]; then
        SID="$(grep -E '^twilio.account-sid=' "$ARCHIVO" | cut -d= -f2- | tr -d '[:space:]' || true)"
        TOKEN="$(grep -E '^twilio.auth-token=' "$ARCHIVO" | cut -d= -f2- | tr -d '[:space:]' || true)"
    fi
    if [[ -n "$SID" && -n "$TOKEN" ]]; then
        read -rp "Ya hay credenciales de Twilio guardadas ($SID). ¿Usarlas? (S/n): " USAR
        if [[ "$(limpiar "$USAR" | tr '[:upper:]' '[:lower:]')" == "n" ]]; then SID=""; TOKEN=""; fi
    fi
    if [[ -z "$SID" || -z "$TOKEN" ]]; then
        echo "Copia los datos de https://console.twilio.com (Cuenta SID y Token de autenticación)."
        read -rp "Account SID (empieza por AC): " SID
        read -rsp "Auth Token (no se muestra al escribir): " TOKEN; echo
        SID="$(limpiar "$SID")"; TOKEN="$(limpiar "$TOKEN")"
    fi

    echo "Verificando las credenciales con Twilio..."
    CODIGO="$(curl -s -m 15 -o "$RESPUESTA" -w '%{http_code}' -u "$SID:$TOKEN" "$API_TWILIO/2010-04-01/Accounts/$SID.json" || echo 000)"
    if [[ "$CODIGO" == "000" ]]; then echo "Error: no hay conexión con Twilio. No se guardó nada."; exit 1; fi
    if [[ "$CODIGO" != "200" ]]; then
        echo "Error: Twilio rechazó las credenciales (HTTP $CODIGO): $(campo message). No se guardó nada."; exit 1
    fi
    echo "Credenciales correctas. Cuenta: \"$(campo friendly_name)\" (tipo $(campo type))."

    echo "Creando el servicio de Verify \"Plazoleta\" en tu cuenta..."
    CODIGO="$(curl -s -m 15 -o "$RESPUESTA" -w '%{http_code}' -u "$SID:$TOKEN" "$API_VERIFY/v2/Services" \
        --data-urlencode "FriendlyName=Plazoleta" --data-urlencode "CodeLength=6" || echo 000)"
    SERVICIO="$(grep -oE '"sid"[[:space:]]*:[[:space:]]*"VA[0-9a-fA-F]{32}"' "$RESPUESTA" | grep -oE 'VA[0-9a-fA-F]{32}' | head -1 || true)"
    if [[ "$CODIGO" != "201" || -z "$SERVICIO" ]]; then
        echo "Twilio no dejó crear el servicio automáticamente (HTTP $CODIGO): $(campo message)"
        echo "Créalo a mano en la consola: Verify > Services > Create new (nombre Plazoleta) y copia su Service SID (empieza por VA)."
        read -rp "Service SID de Verify (VA...): " SERVICIO
        SERVICIO="$(limpiar "$SERVICIO")"
        if [[ ! "$SERVICIO" =~ ^VA[0-9a-fA-F]{32}$ ]]; then echo "Error: el Service SID debe empezar por VA. No se guardó nada."; exit 1; fi
    else
        echo "Servicio creado: $SERVICIO"
    fi

    guardar <<PROPIEDADES
twilio.account-sid=$SID
twilio.auth-token=$TOKEN
twilio.verify-service-sid=$SERVICIO
PROPIEDADES

    echo
    echo "Prueba real: Twilio enviará un código a tu celular y luego lo comprobamos."
    pedir_celular_prueba
    if [[ -z "$CELULAR" ]]; then return; fi
    CODIGO="$(curl -s -m 20 -o "$RESPUESTA" -w '%{http_code}' -u "$SID:$TOKEN" "$API_VERIFY/v2/Services/$SERVICIO/Verifications" \
        --data-urlencode "To=+57$CELULAR" --data-urlencode "Channel=sms" --data-urlencode "Locale=es" || echo 000)"
    if [[ "$CODIGO" != "201" ]]; then
        echo "Twilio no envió el código (HTTP $CODIGO): $(campo message)"
        echo "Revisa: número verificado en Phone Numbers > Verified Caller IDs y Colombia activo en Geo permissions."
        echo "La configuración quedó guardada; corrige eso en Twilio y vuelve a probar."
        return
    fi
    echo "Código enviado a +57$CELULAR. Revisa tu celular."
    read -rp "Escribe el código que te llegó: " RECIBIDO
    RECIBIDO="$(limpiar "$RECIBIDO")"
    curl -s -m 20 -o "$RESPUESTA" -u "$SID:$TOKEN" "$API_VERIFY/v2/Services/$SERVICIO/VerificationCheck" \
        --data-urlencode "To=+57$CELULAR" --data-urlencode "Code=$RECIBIDO" > /dev/null || true
    if [[ "$(campo status)" == "approved" ]]; then
        echo "¡Funciona! Twilio validó el código. Así se van a entregar los pedidos."
    else
        echo "Twilio no validó ese código (estado: $(campo status)). Revisa que lo hayas escrito bien; la configuración sí quedó guardada."
    fi
}

echo "Configuración de SMS reales para la Plazoleta"
echo "  1) Celular Android propio: REQUIERE INSTALAR en ese celular la app \"SMS Gateway for Android\"."
echo "     Los SMS salen del número de ese celular."
echo "  2) Twilio con texto propio: NO requiere instalar nada, pero la cuenta debe estar ACTIVADA CON PAGO."
echo "     El SMS lleva nuestro texto: restaurante, PIN y teléfono."
echo "  3) Twilio Verify: NO requiere instalar nada y sirve con la cuenta GRATIS. (Recomendado si no pagas)"
echo "     Twilio genera el código, lo envía y lo valida; el texto lo pone Twilio."
read -rp "Opción (1/2/3): " OPCION
case "$(limpiar "$OPCION")" in
    1) configurar_android ;;
    2) configurar_twilio ;;
    3) configurar_verify ;;
    *) echo "Opción no válida. No se guardó nada."; exit 1 ;;
esac

echo
echo "Listo. Reinicia los servicios (Ctrl+C y ./mvnw spring-boot:run). En la terminal debe salir 'SMS REALES activados'"
echo "o, con la opción 3, 'CÓDIGOS REALES activados: Twilio Verify'."
