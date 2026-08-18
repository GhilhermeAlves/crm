package com.becommerce.crm.infrastructure.integration.openai;

import com.becommerce.crm.application.ai.port.output.AiProvider;
import com.becommerce.crm.domain.ai.AiProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapter de produção do OpenAI (Chat Completions) para o assistente de IA
 * (AI-01). Ativo SOMENTE quando {@code app.ai.provider=openai}; por padrão
 * usa-se o {@link FakeAiChatProvider}.
 *
 * <p>A API key vem de config/cofre ({@code app.ai.api-key}); nunca é logada nem
 * persistida. Reutiliza o mesmo endpoint/config do provider de sugestão.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiChatProvider implements AiProvider {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public OpenAiChatProvider(
            WebClient.Builder webClientBuilder,
            @Value("${app.ai.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.model:gpt-4o-mini}") String model) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String chat(ChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("Chave de API do OpenAI não configurada (app.ai.api-key).");
        }

        List<Map<String, String>> messages = new ArrayList<>();
        for (ChatMessage line : request.messages()) {
            messages.add(Map.of("role", mapRole(line.role()), "content", line.content()));
        }

        try {
            Map<?, ?> body = webClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "model", model,
                            "messages", messages,
                            "max_tokens", 600,
                            "temperature", 0.5))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractContent(body);
        } catch (AiProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AiProviderException("Falha ao consultar o OpenAI: " + e.getMessage(), e);
        }
    }

    @Override
    public String providerName() {
        return "OPENAI";
    }

    private String extractContent(Map<?, ?> body) {
        if (body == null) {
            throw new AiProviderException("Resposta do OpenAI vazia.");
        }
        Object choices = body.get("choices");
        if (choices instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> choice) {
                Object message = choice.get("message");
                if (message instanceof Map<?, ?> msg) {
                    Object content = msg.get("content");
                    if (content != null && !content.toString().trim().isBlank()) {
                        return content.toString().trim();
                    }
                }
            }
        }
        throw new AiProviderException("Resposta do OpenAI sem conteúdo.");
    }

    private String mapRole(String role) {
        return "user".equals(role) || "assistant".equals(role) ? role : "user";
    }
}
