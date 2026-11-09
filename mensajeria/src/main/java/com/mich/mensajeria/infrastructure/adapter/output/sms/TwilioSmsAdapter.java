package com.mich.mensajeria.infrastructure.adapter.output.sms;

import com.mich.mensajeria.application.port.output.SmsPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "sms.provider", havingValue = "twilio")
public class TwilioSmsAdapter implements SmsPort {

    private static final String MESSAGES_URI = "/2010-04-01/Accounts/{accountSid}/Messages.json";

    private final RestClient restClient;
    private final String accountSid;
    private final String fromNumber;

    public TwilioSmsAdapter(@Value("${twilio.base-url}") String baseUrl,
                            @Value("${twilio.account-sid}") String accountSid,
                            @Value("${twilio.auth-token}") String authToken,
                            @Value("${twilio.from-number}") String fromNumber) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(accountSid, authToken))
                .build();
        this.accountSid = accountSid;
        this.fromNumber = fromNumber;
    }

    @Override
    public void enviar(String celular, String mensaje) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", celular);
        form.add("From", fromNumber);
        form.add("Body", mensaje);
        restClient.post()
                .uri(MESSAGES_URI, accountSid)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }
}
