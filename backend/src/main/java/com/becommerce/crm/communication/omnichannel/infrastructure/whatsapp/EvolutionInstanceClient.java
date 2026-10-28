package com.becommerce.crm.communication.omnichannel.infrastructure.whatsapp;

import com.becommerce.crm.communication.omnichannel.domain.OmnichannelProviderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Gestão da instância na Evolution API v2 (conexão por QR code), separada do
 * envio de mensagens ({@link EvolutionWhatsAppProvider}). Usa a chave global
 * {@code EVOLUTION_API_KEY} (criar instância exige a chave global).
 *
 * <ul>
 *   <li>POST {@code /instance/create} {@code {instanceName, qrcode, integration, webhook}}</li>
 *   <li>GET {@code /instance/connect/{instance}} → {@code {base64, code, pairingCode}}</li>
 *   <li>GET {@code /instance/connectionState/{instance}} → {@code {instance: {state}}}</li>
 *   <li>DELETE {@code /instance/logout/{instance}}</li>
 * </ul>
 */
@Component
public class EvolutionInstanceClient {

    static final List<String> WEBHOOK_EVENTS = List.of("MESSAGES_UPSERT", "MESSAGES_UPDATE");

    private final RestClient restClient;
    private final String baseUrl;
    private final String webhookUrl;
    private final Function<String, String> env;

    @Autowired
    public EvolutionInstanceClient(
            @Value("${omnichannel.whatsapp.evolution.base-url:}") String baseUrl,
            @Value("${omnichannel.whatsapp.evolution.webhook-url:}") String webhookUrl,
            RestClient.Builder restClientBuilder) {
        this(baseUrl, webhookUrl, restClientBuilder, System::getenv);
    }

    EvolutionInstanceClient(String baseUrl, String webhookUrl, RestClient.Builder restClientBuilder,
                            Function<String, String> env) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        this.restClient = restClientBuilder.build();
        this.env = env;
    }

    /** QR code em data URL ({@code data:image/png;base64,...}) e código de pareamento. */
    public record QrCode(String base64, String pairingCode) {
    }

    public boolean isConfigured() {
        return !baseUrl.isBlank() && apiKeyOrNull() != null;
    }

    /** Cria a instância já com o webhook do CRM. Instância existente não é erro. */
    public void create(String instance) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("instanceName", instance);
        body.put("qrcode", true);
        body.put("integration", "WHATSAPP-BAILEYS");
        if (!webhookUrl.isBlank()) {
            body.put("webhook", Map.of("url", webhookUrl, "byEvents", false, "base64", false,
                    "events", WEBHOOK_EVENTS));
        }
        try {
            restClient.post().uri(url("/instance/create"))
                    .header("apikey", apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Forbidden e) {
            // Evolution responde 403 "already in use" quando o nome já existe: segue para o QR.
            if (!e.getResponseBodyAsString().contains("already in use")) {
                throw new OmnichannelProviderException("Evolution recusou criar a instância", e);
            }
        } catch (RuntimeException e) {
            throw new OmnichannelProviderException("Falha ao criar instância na Evolution: " + e.getMessage(), e);
        }
    }

    public QrCode connect(String instance) {
        try {
            Map<?, ?> response = restClient.get().uri(url("/instance/connect/{instance}"), instance)
                    .header("apikey", apiKey())
                    .retrieve()
                    .body(Map.class);
            String base64 = response == null ? null : str(response.get("base64"));
            String pairing = response == null ? null : str(response.get("pairingCode"));
            return new QrCode(base64, pairing);
        } catch (RuntimeException e) {
            throw new OmnichannelProviderException("Falha ao gerar o QR code na Evolution: " + e.getMessage(), e);
        }
    }

    /** {@code open} (conectado), {@code connecting} (aguardando QR), {@code close} ou {@code not_found}. */
    public String state(String instance) {
        try {
            Map<?, ?> response = restClient.get().uri(url("/instance/connectionState/{instance}"), instance)
                    .header("apikey", apiKey())
                    .retrieve()
                    .body(Map.class);
            if (response != null && response.get("instance") instanceof Map<?, ?> data) {
                String state = str(data.get("state"));
                return state == null ? "close" : state;
            }
            return "close";
        } catch (HttpClientErrorException.NotFound e) {
            return "not_found";
        } catch (RuntimeException e) {
            throw new OmnichannelProviderException("Falha ao consultar a Evolution: " + e.getMessage(), e);
        }
    }

    public void logout(String instance) {
        try {
            restClient.delete().uri(url("/instance/logout/{instance}"), instance)
                    .header("apikey", apiKey())
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            // Já desconectada: nada a fazer.
        } catch (RuntimeException e) {
            throw new OmnichannelProviderException("Falha ao desconectar na Evolution: " + e.getMessage(), e);
        }
    }

    private String url(String path) {
        if (baseUrl.isBlank()) {
            throw new OmnichannelProviderException("EVOLUTION_BASE_URL não configurada");
        }
        return baseUrl + path;
    }

    private String apiKeyOrNull() {
        String key = env.apply(EvolutionWhatsAppProvider.DEFAULT_KEY_ENV);
        return key == null || key.isBlank() ? null : key;
    }

    private String apiKey() {
        String key = apiKeyOrNull();
        if (key == null) {
            throw new OmnichannelProviderException(
                    "Credencial Evolution não configurada (" + EvolutionWhatsAppProvider.DEFAULT_KEY_ENV + ")");
        }
        return key;
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }
}
