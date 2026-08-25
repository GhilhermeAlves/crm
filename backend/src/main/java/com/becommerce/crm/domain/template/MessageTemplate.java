package com.becommerce.crm.domain.template;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template de mensagem por canal (Sprint 17), tabela {@code message_templates} (V055).
 * Variáveis na sintaxe {@code {{nome}}}; versionamento incrementa {@code version}.
 */
public class MessageTemplate {

    public static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.]+)\\s*}}");

    private final UUID id;
    private final UUID companyId;
    private String name;
    private String channelType;
    private String subject;
    private String body;
    private String variables;
    private TemplateStatus status;
    private int version;
    private String externalTemplateId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private MessageTemplate(UUID id, UUID companyId, String name, String channelType,
                            String subject, String body, String variables, TemplateStatus status,
                            int version, String externalTemplateId,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.channelType = channelType;
        this.subject = subject;
        this.body = body;
        this.variables = variables;
        this.status = status;
        this.version = version;
        this.externalTemplateId = externalTemplateId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MessageTemplate create(UUID companyId, String name, String channelType,
                                         String subject, String body, String variables,
                                         String externalTemplateId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nome do template é obrigatório.");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Conteúdo do template é obrigatório.");
        }
        LocalDateTime now = LocalDateTime.now();
        return new MessageTemplate(UUID.randomUUID(), companyId, name,
                channelType != null ? channelType : "WHATSAPP", subject, body,
                variables != null ? variables : "[]", TemplateStatus.ACTIVE, 1,
                externalTemplateId, now, now);
    }

    public static MessageTemplate reconstitute(UUID id, UUID companyId, String name,
                                               String channelType, String subject, String body,
                                               String variables, TemplateStatus status, int version,
                                               String externalTemplateId,
                                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new MessageTemplate(id, companyId, name, channelType, subject, body, variables,
                status, version, externalTemplateId, createdAt, updatedAt);
    }

    /** Extrai os nomes de variáveis declaradas no corpo ({{var}}). */
    public List<String> extractVariables() {
        List<String> names = new ArrayList<>();
        if (body == null) {
            return names;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(body);
        while (matcher.find()) {
            String var = matcher.group(1);
            if (!names.contains(var)) {
                names.add(var);
            }
        }
        return names;
    }

    /**
     * Renderiza o corpo substituindo variáveis. Variável ausente no mapa é
     * mantida como texto vazio — falha de render não bloqueia a campanha
     * (registrada como conteúdo parcial).
     */
    public String render(Map<String, String> values) {
        if (body == null) {
            return null;
        }
        if (values == null || values.isEmpty()) {
            return VARIABLE_PATTERN.matcher(body).replaceAll("");
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(body);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String replacement = values.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /** Edição cria nova versão (versionamento imutável por execução). */
    public void updateContent(String subject, String body, String variables) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Conteúdo do template é obrigatório.");
        }
        this.subject = subject;
        this.body = body;
        this.variables = variables != null ? variables : this.variables;
        this.version++;
        touch();
    }

    public void rename(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        touch();
    }

    public void archive() {
        this.status = TemplateStatus.ARCHIVED;
        touch();
    }

    public boolean isActive() {
        return status == TemplateStatus.ACTIVE;
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getName() { return name; }
    public String getChannelType() { return channelType; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public String getVariables() { return variables; }
    public TemplateStatus getStatus() { return status; }
    public int getVersion() { return version; }
    public String getExternalTemplateId() { return externalTemplateId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
