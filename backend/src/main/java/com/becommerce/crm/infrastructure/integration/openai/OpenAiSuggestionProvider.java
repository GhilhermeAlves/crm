package com.becommerce.crm.infrastructure.integration.openai;

import com.becommerce.crm.application.ai.port.output.AiSuggestionProvider;
import com.becommerce.crm.domain.ai.AiProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Adapter de produção do OpenAI (Chat Completions) para sugestão de resposta
 * (Sprint 20, Módulo de IA). Ativo quando {@code app.ai.provider=openai} (default).
 *
 * <p>A API key vem de config/cofre ({@code app.ai.api-key}); nunca é logada nem
 * persistida. Usa {@link WebClient} (spring-boot-starter-webflux já presente).
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiSuggestionProvider implements AiSuggestionProvider {

    private static final String SYSTEM_PROMPT = "Você é um assistente de atendimento de um CRM. "
            + "Com base no histórico da conversa, gere UMA resposta curta e profissional em português "
            + "que o agente deve enviar ao cliente. Responda apenas com o texto da mensagem, sem aspas, "
            + "sem prefixos e sem quebras de linha.";

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public OpenAiSuggestionProvider(
            WebClient.Builder webClientBuilder,
            @Value("${app.ai.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.model:gpt-4o-mini}") String model) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String suggest(SuggestRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("Chave de API do OpenAI não configurada (app.ai.api-key).");
        }

        List<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        for (MessageLine line : request.history()) {
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
                            "max_tokens", 300,
                            "temperature", 0.7))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractSuggestion(body);
        } catch (AiProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AiProviderException("Falha ao gerar sugestão no OpenAI: " + e.getMessage(), e);
        }
    }

    @Override
    public String providerName() {
        return "OPENAI";
    }

    private String extractSuggestion(Map<?, ?> body) {
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
                    if (content != null) {
                        String text = content.toString().trim();
                        if (!text.isBlank()) {
                            return text;
                        }
                    }
                }
            }
        }
        throw new AiProviderException("Resposta do OpenAI sem conteúdo.");
    }

    private String mapRole(String role) {
        return "customer".equals(role) ? "user" : "assistant";
    }
}