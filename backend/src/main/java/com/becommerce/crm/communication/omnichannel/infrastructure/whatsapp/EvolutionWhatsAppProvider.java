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

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Adapter Evolution API v2 (self-hosted) para WhatsApp — implementação isolada
 * atrás de {@link WhatsAppProvider}. Ativo SOMENTE quando
 * {@code omnichannel.whatsapp.provider=evolution}.
 *
 * <p>Contrato (doc.evolution-api.com, v2):
 * <ul>
 *   <li>POST {@code /message/sendText/{instance}} com body {@code {"number":"…","text":"…"}}</li>
 *   <li>{@code delay} (ms) opcional: a Evolution exibe "digitando…" antes de enviar</li>
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
        return sendMessage("/message/sendText/{instance}", request, Map.of("number", request.to(),
                "text", request.body(), "delay", typingDelayMillis(request.body())));
    }

    /**
     * Nota de voz: {@code POST /message/sendWhatsAppAudio/{instance}} com o áudio em
     * base64; a Evolution converte para ogg/opus e mostra "gravando áudio…" no delay.
     */
    @Override
    public SendResult sendVoice(SendRequest request, byte[] audio) {
        return sendMessage("/message/sendWhatsAppAudio/{instance}", request, Map.of("number", request.to(),
                "audio", Base64.getEncoder().encodeToString(audio), "delay", 1_500));
    }

    /** {@code POST /chat/getBase64FromMediaMessage/{instance}} → {@code {base64, mimetype, fileName?}}. */
    @Override
    public Optional<MediaContent> downloadMedia(String instance, String externalMessageId, String secretsRef) {
        requireConfigured(instance);
        try {
            Map<?, ?> response = restClient.post()
                    .uri(baseUrl + "/chat/getBase64FromMediaMessage/{instance}", instance)
                    .header("apikey", resolveApiKey(secretsRef))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", Map.of("key", Map.of("id", externalMessageId)),
                            "convertToMp4", false))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new OmnichannelProviderException("Evolution HTTP " + res.getStatusCode().value());
                    })
                    .body(Map.class);
            Object base64 = response != null ? response.get("base64") : null;
            if (base64 == null || String.valueOf(base64).isBlank()) {
                return Optional.empty();
            }
            Object mime = response.get("mimetype");
            Object fileName = response.get("fileName");
            return Optional.of(new MediaContent(Base64.getMimeDecoder().decode(String.valueOf(base64)),
                    mime != null ? String.valueOf(mime) : null, fileName != null ? String.valueOf(fileName) : null));
        } catch (OmnichannelProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new OmnichannelProviderException("Falha Evolution: " + e.getMessage(), e);
        }
    }

    private SendResult sendMessage(String path, SendRequest request, Map<String, Object> body) {
        String instance = request.phoneNumberId();
        requireConfigured(instance);
        String apiKey = resolveApiKey(request.secretsRef());
        try {
            Map<?, ?> response = restClient.post()
                    .uri(baseUrl + path, instance)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
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

    private void requireConfigured(String instance) {
        if (baseUrl.isBlank()) {
            throw new OmnichannelProviderException("EVOLUTION_BASE_URL não configurada");
        }
        if (instance == null || instance.isBlank()) {
            throw new OmnichannelProviderException("Canal sem instância Evolution (externalId)");
        }
    }

    @Override
    public String providerName() {
        return "EVOLUTION";
    }

    /**
     * Tempo de "digitando…" antes da mensagem (a Evolution mostra a presença
     * {@code composing} durante o {@code delay}): proporcional ao tamanho do
     * texto, como uma pessoa digitando, entre 1,5 s e 7 s.
     */
    static int typingDelayMillis(String text) {
        int length = text == null ? 0 : text.length();
        return Math.max(1_500, Math.min(7_000, 1_000 + length * 35));
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
