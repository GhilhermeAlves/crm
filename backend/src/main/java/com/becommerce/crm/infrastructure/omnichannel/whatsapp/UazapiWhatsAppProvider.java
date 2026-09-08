package com.becommerce.crm.infrastructure.omnichannel.whatsapp;

import com.becommerce.crm.application.omnichannel.port.output.WhatsAppProvider;
import com.becommerce.crm.domain.omnichannel.OmnichannelProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.function.Function;

/**
 * Adapter UAZAPI V2 (uazapiGO) para WhatsApp — implementação isolada
 * atrás de {@link WhatsAppProvider}. Ativo SOMENTE quando
 * {@code omnichannel.whatsapp.provider=uazapi}.
 *
 * <p>Contrato oficial (docs.uazapi.com):
 * <ul>
 *   <li>POST {@code /send/text} com body {@code {"number":"…","text":"…"}}</li>
 *   <li>Header customizado {@code token: <instance-token>} (não Bearer)</li>
 *   <li>Base URL por instância: {@code https://<subdomain>.uazapi.com}</li>
 * </ul>
 *
 * <p>Token resolution: {@code secretsRef} do canal → env de mesmo nome →
 * fallback {@code UAZAPI_TOKEN} global. Nunca loga nem persiste.
 */
@Service
@ConditionalOnProperty(name = "omnichannel.whatsapp.provider", havingValue = "uazapi")
public class UazapiWhatsAppProvider implements WhatsAppProvider {

    private static final Logger log = LoggerFactory.getLogger(UazapiWhatsAppProvider.class);

    private final RestClient restClient;
    private final String baseUrl;
    private final Function<String, String> env;

    @Autowired
    public UazapiWhatsAppProvider(
            @Value("${omnichannel.whatsapp.uazapi.base-url:}") String baseUrl,
            RestClient.Builder restClientBuilder) {
        this(baseUrl, restClientBuilder, System::getenv);
    }

    UazapiWhatsAppProvider(String baseUrl, RestClient.Builder restClientBuilder,
                           Function<String, String> env) {
        this.baseUrl = baseUrl;
        this.restClient = restClientBuilder.build();
        this.env = env;
    }

    @Override
    public SendResult send(SendRequest request) {
        String token = resolveToken(request);
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new OmnichannelProviderException("UAZAPI_BASE_URL não configurada");
        }
        String url = baseUrl.replaceAll("/$", "") + "/send/text";
        try {
            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .header("token", token)
                    .header("Content-Type", "application/json")
                    .body(Map.of("number", request.to(), "text", request.body()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new OmnichannelProviderException(
                                "UAZAPI HTTP " + res.getStatusCode().value());
                    })
                    .body(Map.class);

            String externalId = extractExternalId(response);
            if (externalId == null || externalId.isBlank()) {
                throw new OmnichannelProviderException("UAZAPI resposta sem id de mensagem");
            }
            return new SendResult(externalId);
        } catch (OmnichannelProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new OmnichannelProviderException("Falha UAZAPI: " + e.getMessage(), e);
        }
    }

    @Override
    public String providerName() {
        return "UAZAPI";
    }

    /**
     * Token resolvido por canal (via {@code secretsRef}) com fallback global.
     * Nunca loga o valor.
     */
    private String resolveToken(SendRequest request) {
        String ref = request.secretsRef();
        if (ref != null && !ref.isBlank()) {
            String channelToken = env.apply(ref);
            if (channelToken != null && !channelToken.isBlank()) {
                return channelToken;
            }
        }
        String token = env.apply("UAZAPI_TOKEN");
        if (token == null || token.isBlank()) {
            throw new OmnichannelProviderException(
                    "Credencial UAZAPI não configurada (UAZAPI_TOKEN)");
        }
        return token;
    }

    /**
     * Extrai o id da mensagem da resposta uazapiGO V2.
     * Formatos conhecidos: {@code data.key.id}, {@code data.id}, top-level {@code id},
     * ou um prefixo único quando o provider não retorna id estruturado.
     */
    @SuppressWarnings("unchecked")
    private static String extractExternalId(Map<?, ?> response) {
        if (response == null) {
            return null;
        }
        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object keyId = dataMap.get("key");
            if (keyId instanceof Map<?, ?> keyMap && keyMap.get("id") != null) {
                return String.valueOf(keyMap.get("id"));
            }
            if (dataMap.get("id") != null) {
                return String.valueOf(dataMap.get("id"));
            }
            Object messageId = dataMap.get("message");
            if (messageId instanceof Map<?, ?> messageMap && messageMap.get("id") != null) {
                return String.valueOf(messageMap.get("id"));
            }
        }
        if (response.get("id") != null) {
            return String.valueOf(response.get("id"));
        }
        // Gera id estável para providers que retornam 200 sem id explícito.
        Object success = response.get("success");
        if (Boolean.TRUE.equals(success)) {
            return "UAZAPI_" + java.util.UUID.randomUUID();
        }
        return null;
    }
}
