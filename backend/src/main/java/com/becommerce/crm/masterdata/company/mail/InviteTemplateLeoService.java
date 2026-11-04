package com.becommerce.crm.masterdata.company.mail;

import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.domain.AiProviderException;
import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.InviteSuggestion;
import com.becommerce.crm.masterdata.company.mail.CompanyMailDtos.InviteSuggestionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Leo (assistente de IA) sugere os textos do modelo do convite: título,
 * mensagem de boas-vindas e rodapé. Só sugere: a empresa revisa e salva.
 * Sem provider de IA configurado ({@code app.ai.provider}), responde 503.
 */
@Service
public class InviteTemplateLeoService {

    private static final Logger log = LoggerFactory.getLogger(InviteTemplateLeoService.class);

    private static final Map<String, String> TONES = Map.of(
            "ACOLHEDOR", "acolhedor e caloroso",
            "PROFISSIONAL", "profissional e direto",
            "DESCONTRAIDO", "descontraído e leve");

    private final ObjectProvider<AiProvider> aiProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InviteTemplateLeoService(ObjectProvider<AiProvider> aiProvider) {
        this.aiProvider = aiProvider;
    }

    public boolean isAvailable() {
        return aiProvider.getIfAvailable() != null;
    }

    public InviteSuggestion suggest(UUID companyId, UUID userId, String companyName,
                                    InviteSuggestionRequest request) {
        AiProvider provider = aiProvider.getIfAvailable();
        if (provider == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O Leo não está disponível no momento.");
        }
        String tone = TONES.getOrDefault(request.tone() == null ? "" : request.tone(), TONES.get("ACOLHEDOR"));
        List<AiProvider.ChatMessage> messages = List.of(
                new AiProvider.ChatMessage("system", systemPrompt()),
                new AiProvider.ChatMessage("user", userPrompt(companyName, tone, request.notes())));
        try {
            String raw = provider.chatStructured(new AiProvider.ChatRequest(companyId, userId, messages, List.of(),
                    new AiProvider.GenerationParams(null, 0.7, 500, Duration.ofSeconds(30))));
            return parse(raw);
        } catch (AiProviderException e) {
            log.warn("Leo não conseguiu sugerir o convite (empresa={}): {}", companyId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "O Leo não conseguiu sugerir agora. Tente de novo em instantes.");
        }
    }

    static String systemPrompt() {
        return """
                Você é o Leo, assistente do CRM. Escreva em português do Brasil os textos do e-mail que
                convida uma pessoa a criar a conta no CRM da empresa. O e-mail já tem, fora do seu texto,
                o botão "Criar minha conta", um passo a passo de 4 passos e o link do convite: não os repita.
                Responda SOMENTE um JSON com as chaves:
                  "title": título curto (até 60 caracteres), sem emojis em excesso;
                  "message": 2 a 4 frases de boas-vindas (até 600 caracteres), usando os marcadores
                             {empresa} (nome da empresa) e {perfil} (perfil de acesso) exatamente assim;
                  "footer": uma linha curta de assinatura ou slogan (até 120 caracteres).
                Não invente endereço, telefone, preços nem promessas. Não use HTML nem markdown.
                """;
    }

    static String userPrompt(String companyName, String tone, String notes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Empresa: ").append(companyName == null || companyName.isBlank() ? "(não informado)" : companyName)
                .append("\nTom desejado: ").append(tone);
        if (notes != null && !notes.isBlank()) {
            sb.append("\nSobre a empresa (informado pelo usuário): ").append(notes.trim());
        }
        return sb.toString();
    }

    InviteSuggestion parse(String raw) {
        try {
            String json = raw == null ? "{}" : raw.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            JsonNode node = objectMapper.readTree(json);
            return new InviteSuggestion(
                    clip(node.path("title").asText(""), 150),
                    clip(node.path("message").asText(""), 1500),
                    clip(node.path("footer").asText(""), 200));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "O Leo respondeu num formato inesperado. Tente de novo.");
        }
    }

    private static String clip(String value, int max) {
        String v = value == null ? "" : value.trim();
        return v.length() <= max ? v : v.substring(0, max).trim();
    }
}
