package com.becommerce.crm.infrastructure.omnichannel.whatsapp;

import com.becommerce.crm.application.omnichannel.port.output.WhatsAppProvider;
import com.becommerce.crm.domain.omnichannel.OmnichannelProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Adapter de produção da WhatsApp Cloud API (Meta), FASE 4/24.
 * Ativo SOMENTE quando {@code omnichannel.whatsapp.provider=cloud-api};
 * por padrão (sem credenciais) usa-se o {@link FakeWhatsAppProvider}.
 *
 * <p>O token é injetado de config/cofre (secrets_ref no Channel aponta para ele);
 * nunca é logado nem persistido no banco.
 */
@Service
@ConditionalOnProperty(name = "omnichannel.whatsapp.provider", havingValue = "cloud-api")
public class WhatsAppCloudApiProvider implements WhatsAppProvider {

    private final RestClient restClient;
    private final String graphUrl;

    public WhatsAppCloudApiProvider(
            @Value("${omnichannel.whatsapp.graph-url:https://graph.facebook.com/v19.0}") String graphUrl,
            RestClient.Builder restClientBuilder) {
        this.graphUrl = graphUrl;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public SendResult send(SendRequest request) {
        String token = resolveToken(request);
        try {
            Map<?, ?> body = restClient.post()
                    .uri(graphUrl + "/{phoneNumberId}/messages", request.phoneNumberId())
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "messaging_product", "whatsapp",
                            "to", request.to(),
                            "type", "text",
                            "text", Map.of("preview_url", false, "body", request.body())))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new OmnichannelProviderException("WhatsApp Cloud API HTTP " + res.getStatusCode().value());
                    })
                    .body(Map.class);
            Object wamid = body != null ? body.get("messages") : null;
            String externalId = (wamid instanceof java.util.List<?> list && !list.isEmpty())
                    ? String.valueOf(((Map<?, ?>) list.get(0)).get("id")) : null;
            if (externalId == null || externalId.isBlank()) {
                throw new OmnichannelProviderException("Resposta do provider sem wamid");
            }
            return new SendResult(externalId);
        } catch (OmnichannelProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new OmnichannelProviderException("Falha ao enviar mensagem no WhatsApp: " + e.getMessage(), e);
        }
    }

    @Override
    public String providerName() {
        return "WHATSAPP_CLOUD_API";
    }

    /** Token resolvido de config/cofre via a referência do canal (secrets_ref). */
    private String resolveToken(SendRequest request) {
        String token = System.getenv("CRM_WHATSAPP_ACCESS_TOKEN");
        if (token == null || token.isBlank()) {
            throw new OmnichannelProviderException("Credencial de WhatsApp não configurada (CRM_WHATSAPP_ACCESS_TOKEN)");
        }
        return token;
    }
}
