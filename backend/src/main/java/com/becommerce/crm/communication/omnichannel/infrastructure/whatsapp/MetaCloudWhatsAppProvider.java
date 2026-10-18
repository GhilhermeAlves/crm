package com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp;

import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppProvider;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelProviderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Adapter WhatsApp Cloud API (Meta) atrás de {@link WhatsAppProvider}. Sempre
 * registrado; usado pelos canais com provider {@code META_CLOUD} via
 * {@link RoutingWhatsAppProvider}.
 *
 * <p>Contrato (Graph API):
 * <ul>
 *   <li>POST {@code /{version}/{phone-number-id}/messages} com
 *       {@code {"messaging_product":"whatsapp","to":"…","type":"text","text":{…}}}</li>
 *   <li>Header {@code Authorization: Bearer <token>}</li>
 *   <li>Resposta contém {@code messages[0].id} (wamid)</li>
 * </ul>
 *
 * <p>O {@code phone_number_id} é o {@code externalId} do canal. O token vem do env
 * apontado pelo {@code secretsRef} do canal, com fallback para
 * {@code META_WHATSAPP_ACCESS_TOKEN}. Nunca loga nem persiste o token.
 * Voz e download de mídia ainda não são suportados (defaults da porta).
 */
@Component
public class MetaCloudWhatsAppProvider implements WhatsAppProvider {

    static final String DEFAULT_TOKEN_ENV = "META_WHATSAPP_ACCESS_TOKEN";

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiVersion;
    private final Function<String, String> env;

    @Autowired
    public MetaCloudWhatsAppProvider(
            @Value("${omnichannel.whatsapp.meta.base-url:https://graph.facebook.com}") String baseUrl,
            @Value("${omnichannel.whatsapp.meta.api-version:v20.0}") String apiVersion,
            RestClient.Builder restClientBuilder) {
        this(baseUrl, apiVersion, restClientBuilder, System::getenv);
    }

    MetaCloudWhatsAppProvider(String baseUrl, String apiVersion, RestClient.Builder restClientBuilder,
                              Function<String, String> env) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        this.apiVersion = apiVersion;
        this.restClient = restClientBuilder.build();
        this.env = env;
    }

    @Override
    public SendResult send(SendRequest request) {
        String phoneNumberId = request.phoneNumberId();
        if (phoneNumberId == null || phoneNumberId.isBlank()) {
            throw new OmnichannelProviderException("Canal sem phone_number_id da Meta (externalId)");
        }
        String token = resolveToken(request.secretsRef());
        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", request.to(),
                "type", "text",
                "text", Map.of("preview_url", false, "body", request.body()));
        try {
            Map<?, ?> response = restClient.post()
                    .uri(baseUrl + "/{version}/{phoneNumberId}/messages", apiVersion, phoneNumberId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new OmnichannelProviderException("Meta HTTP " + res.getStatusCode().value());
                    })
                    .body(Map.class);

            String externalId = extractMessageId(response);
            if (externalId == null || externalId.isBlank()) {
                throw new OmnichannelProviderException("Meta resposta sem id de mensagem");
            }
            return new SendResult(externalId);
        } catch (OmnichannelProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new OmnichannelProviderException("Falha Meta: " + e.getMessage(), e);
        }
    }

    @Override
    public String providerName() {
        return "META_CLOUD";
    }

    private String resolveToken(String secretsRef) {
        if (secretsRef != null && !secretsRef.isBlank()) {
            String channelToken = env.apply(secretsRef);
            if (channelToken != null && !channelToken.isBlank()) {
                return channelToken;
            }
        }
        String token = env.apply(DEFAULT_TOKEN_ENV);
        if (token == null || token.isBlank()) {
            throw new OmnichannelProviderException(
                    "Credencial Meta não configurada (" + DEFAULT_TOKEN_ENV + ")");
        }
        return token;
    }

    private static String extractMessageId(Map<?, ?> response) {
        if (response != null && response.get("messages") instanceof List<?> messages && !messages.isEmpty()
                && messages.get(0) instanceof Map<?, ?> first && first.get("id") != null) {
            return String.valueOf(first.get("id"));
        }
        return null;
    }
}
