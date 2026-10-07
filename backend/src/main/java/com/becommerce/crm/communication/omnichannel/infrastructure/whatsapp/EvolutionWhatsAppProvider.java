package com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp;

import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppProvider;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelProviderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.function.Function;

/**
 * Adapter Evolution API v2 (self-hosted) para WhatsApp — implementação isolada
 * atrás de {@link WhatsAppProvider}. Ativo SOMENTE quando
 * {@code omnichannel.whatsapp.provider=evolution}.
 *
 * <p>Contrato (doc.evolution-api.com, v2):
 * <ul>
 *   <li>POST {@code /message/sendText/{instance}} com body {@code {"number":"…","text":"…"}}</li>
 *   <li>Header {@code apikey: <key>}</li>
 *   <li>Resposta contém {@code key.id} (id da mensagem no WhatsApp)</li>
 * </ul>
 *
 * <p>A instância é o {@code externalId} do canal. A apikey vem do env apontado
 * pelo {@code secretsRef} do canal, com fallback para {@code EVOLUTION_API_KEY}.
 * Nunca loga nem persiste a chave.
 */
@Service
@ConditionalOnProperty(name = "omnichannel.whatsapp.provider", havingValue = "evolution")
public class EvolutionWhatsAppProvider implements WhatsAppProvider {

    static final String DEFAULT_KEY_ENV = "EVOLUTION_API_KEY";

    private final RestClient restClient;
    private final String baseUrl;
    private final Function<String, String> env;

    @Autowired
    public EvolutionWhatsAppProvider(
            @Value("${omnichannel.whatsapp.evolution.base-url:}") String baseUrl,
            RestClient.Builder restClientBuilder) {
        this(baseUrl, restClientBuilder, System::getenv);
    }

    EvolutionWhatsAppProvider(String baseUrl, RestClient.Builder restClientBuilder,
                              Function<String, String> env) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        this.restClient = restClientBuilder.build();
        this.env = env;
    }

    @Override
    public SendResult send(SendRequest request) {
        if (baseUrl.isBlank()) {
            throw new OmnichannelProviderException("EVOLUTION_BASE_URL não configurada");
        }
        String instance = request.phoneNumberId();
        if (instance == null || instance.isBlank()) {
            throw new OmnichannelProviderException("Canal sem instância Evolution (externalId)");
        }
        String apiKey = resolveApiKey(request.secretsRef());
        try {
            Map<?, ?> response = restClient.post()
                    .uri(baseUrl + "/message/sendText/{instance}", instance)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("number", request.to(), "text", request.body()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new OmnichannelProviderException(
                                "Evolution HTTP " + res.getStatusCode().value());
                    })
                    .body(Map.class);

            String externalId = extractMessageId(response);
            if (externalId == null || externalId.isBlank()) {
                throw new OmnichannelProviderException("Evolution resposta sem id de mensagem");
            }
            return new SendResult(externalId);
        } catch (OmnichannelProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new OmnichannelProviderException("Falha Evolution: " + e.getMessage(), e);
        }
    }

    @Override
    public String providerName() {
        return "EVOLUTION";
    }

    private String resolveApiKey(String secretsRef) {
        if (secretsRef != null && !secretsRef.isBlank()) {
            String channelKey = env.apply(secretsRef);
            if (channelKey != null && !channelKey.isBlank()) {
                return channelKey;
            }
        }
        String key = env.apply(DEFAULT_KEY_ENV);
        if (key == null || key.isBlank()) {
            throw new OmnichannelProviderException(
                    "Credencial Evolution não configurada (" + DEFAULT_KEY_ENV + ")");
        }
        return key;
    }

    private static String extractMessageId(Map<?, ?> response) {
        if (response != null && response.get("key") instanceof Map<?, ?> key && key.get("id") != null) {
            return String.valueOf(key.get("id"));
        }
        return null;
    }
}
