package com.becommerce.crm.communication.omnichannel.infrastructure.instagram;

import com.becommerce.crm.communication.omnichannel.application.port.output.WhatsAppProvider;
import com.becommerce.crm.communication.omnichannel.domain.OmnichannelProviderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Envio de mensagens do Instagram Direct (Instagram API com login do Instagram).
 * Usado pelos canais {@code META_INSTAGRAM} via
 * {@link com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp.RoutingWhatsAppProvider}.
 *
 * <p>Contrato: {@code POST https://graph.instagram.com/{version}/me/messages} com
 * {@code {"recipient":{"id":"<IGSID>"},"message":{"text":"…"}}} e
 * {@code Authorization: Bearer <token do Instagram>}; resposta {@code {"recipient_id","message_id"}}.
 * O texto tem limite de 1000 bytes: mensagens maiores são enviadas em partes.
 * Só é possível responder até 24h depois da última mensagem do cliente.
 *
 * <p>O token vem do env apontado pelo {@code secretsRef} do canal, com fallback
 * para {@code META_INSTAGRAM_ACCESS_TOKEN}. Nunca loga nem persiste o token.
 * Voz e download de mídia não são suportados (defaults da porta).
 */
@Component
public class MetaInstagramProvider implements WhatsAppProvider {

    static final String DEFAULT_TOKEN_ENV = "META_INSTAGRAM_ACCESS_TOKEN";
    static final int MAX_TEXT_BYTES = 1000;

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiVersion;
    private final Function<String, String> env;

    @Autowired
    public MetaInstagramProvider(
            @Value("${omnichannel.instagram.base-url:https://graph.instagram.com}") String baseUrl,
            @Value("${omnichannel.instagram.api-version:v25.0}") String apiVersion,
            RestClient.Builder restClientBuilder) {
        this(baseUrl, apiVersion, restClientBuilder, System::getenv);
    }

    MetaInstagramProvider(String baseUrl, String apiVersion, RestClient.Builder restClientBuilder,
                          Function<String, String> env) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        this.apiVersion = apiVersion;
        this.restClient = restClientBuilder.build();
        this.env = env;
    }

    @Override
    public SendResult send(SendRequest request) {
        if (request.to() == null || request.to().isBlank()) {
            throw new OmnichannelProviderException("Mensagem do Instagram sem destinatário (IGSID)");
        }
        String token = resolveToken(request.secretsRef());
        String lastId = null;
        for (String part : split(request.body())) {
            lastId = sendText(token, request.to(), part);
        }
        return new SendResult(lastId);
    }

    @Override
    public String providerName() {
        return "META_INSTAGRAM";
    }

    private String sendText(String token, String recipientId, String text) {
        Map<String, Object> body = Map.of(
                "recipient", Map.of("id", recipientId),
                "message", Map.of("text", text));
        try {
            Map<?, ?> response = restClient.post()
                    .uri(baseUrl + "/{version}/me/messages", apiVersion)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new OmnichannelProviderException("Instagram HTTP " + res.getStatusCode().value());
                    })
                    .body(Map.class);
            Object id = response == null ? null : response.get("message_id");
            if (id == null || String.valueOf(id).isBlank()) {
                throw new OmnichannelProviderException("Instagram resposta sem message_id");
            }
            return String.valueOf(id);
        } catch (OmnichannelProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new OmnichannelProviderException("Falha Instagram: " + e.getMessage(), e);
        }
    }

    /** Quebra o texto em partes de até {@value #MAX_TEXT_BYTES} bytes, preferindo quebras de linha e espaços. */
    static List<String> split(String text) {
        String remaining = text == null ? "" : text.strip();
        List<String> parts = new ArrayList<>();
        while (bytes(remaining) > MAX_TEXT_BYTES) {
            int cut = 0;
            int bytes = 0;
            int lastBreak = -1;
            for (int i = 0; i < remaining.length(); ) {
                int cp = remaining.codePointAt(i);
                int n = Character.charCount(cp);
                int cpBytes = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length;
                if (bytes + cpBytes > MAX_TEXT_BYTES) {
                    break;
                }
                bytes += cpBytes;
                i += n;
                cut = i;
                if (cp == '\n' || cp == ' ') {
                    lastBreak = i;
                }
            }
            int end = lastBreak > MAX_TEXT_BYTES / 2 ? lastBreak : cut;
            parts.add(remaining.substring(0, end).strip());
            remaining = remaining.substring(end).strip();
        }
        if (!remaining.isEmpty() || parts.isEmpty()) {
            parts.add(remaining);
        }
        return parts;
    }

    private static int bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8).length;
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
                    "Credencial do Instagram não configurada (" + DEFAULT_TOKEN_ENV + ")");
        }
        return token;
    }
}
