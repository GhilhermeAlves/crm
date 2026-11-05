package com.becommerce.crm.automation.ai.application.service;

import com.becommerce.crm.automation.ai.application.context.AiApplicationContext;
import com.becommerce.crm.automation.ai.application.context.AiPermissionContext;
import com.becommerce.crm.automation.ai.application.context.AiScreenCatalog;
import com.becommerce.crm.automation.ai.application.context.ResolvedAiContext;
import com.becommerce.crm.automation.ai.application.dto.AiActionResponse;
import com.becommerce.crm.automation.ai.application.dto.AiChatRequest;
import com.becommerce.crm.automation.ai.application.dto.AiChatResponse;
import com.becommerce.crm.automation.ai.application.dto.AiContextPayload;
import com.becommerce.crm.automation.ai.application.dto.AiConversationResponse;
import com.becommerce.crm.automation.ai.application.dto.AiMessageResponse;
import com.becommerce.crm.automation.ai.application.port.input.AiAssistantUseCase;
import com.becommerce.crm.automation.ai.application.port.output.AiChatRepository;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.application.tool.AiToolContext;
import com.becommerce.crm.automation.ai.application.tool.AiToolRegistry;
import com.becommerce.crm.automation.ai.application.tool.AiToolResult;
import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.automation.ai.domain.AiConversation;
import com.becommerce.crm.automation.ai.domain.AiConversationNotFoundException;
import com.becommerce.crm.automation.ai.domain.AiMessage;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Orquestrador do assistente de IA (AI-01/AI-02/AI-03). Recebe a mensagem +
 * dica de contexto, resolve o contexto REAL de CRM (via {@link AiContextResolver}),
 * monta o prompt (sistema + contexto + histórico + pergunta), chama o
 * {@link AiProvider} e — em AI-03 — permite que o modelo solicite Tools de
 * leitura. Cada Tool é executada pelo backend (via {@link AiToolRegistry},
 * respeitando tenant/permissions), o resultado estruturado volta ao modelo e a
 * resposta final é persistida e auditada.
 *
 * <p>Somente leitura nesta milestone — nenhuma ação de escrita é executada.
 * O isolamento por tenant é garantido pelo {@link TenantContext} (RLS) e por
 * checagens explícitas de posse da conversa. As Tools nunca confiam em
 * companyId/userId/permissions vindos do LLM: usam o {@link AiToolContext}
 * derivado do {@code CurrentUser}.</p>
 */
@Service
public class AiAssistantService implements AiAssistantUseCase {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    /** Número máximo de mensagens recentes enviadas como histórico ao modelo. */
    private static final int HISTORY_LIMIT = 20;

    /** Iterações máximas do loop de Tool Calling (evita laço infinito). */
    private static final int MAX_TOOL_ITERATIONS = 8;

    static final String FINAL_SUMMARY_INSTRUCTION = "Você atingiu o limite de passos com ferramentas nesta "
            + "resposta. Sem chamar ferramentas, resuma para o usuário o que já foi feito (incluindo propostas "
            + "deixadas para confirmação) e o que ainda falta.";

    static final String NO_FINAL_ANSWER = "Não consegui produzir uma resposta final.";

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String ROLE_TOOL = "tool";
    private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter NOW_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"));

    private final AiChatRepository chatRepository;
    private final AiProvider aiProvider;
    private final AiContextResolver contextResolver;
    private final AiToolRegistry toolRegistry;
    private final TenantAuditRecorder auditor;
    private Clock clock = Clock.system(CLINIC_ZONE);

    public AiAssistantService(AiChatRepository chatRepository,
                              AiProvider aiProvider,
                              AiContextResolver contextResolver,
                              AiToolRegistry toolRegistry,
                              TenantAuditRecorder auditor) {
        this.chatRepository = chatRepository;
        this.aiProvider = aiProvider;
        this.contextResolver = contextResolver;
        this.toolRegistry = toolRegistry;
        this.auditor = auditor;
    }

    /**
     * Sem transação envolvendo o chat todo: cada gravação usa a sua. Assim uma Tool
     * cujo service lança exceção (ex.: id inexistente) não marca a conversa inteira
     * como rollback-only (UnexpectedRollbackException), e nenhuma conexão fica presa
     * durante a chamada ao modelo. O TenantContext é restabelecido antes de cada
     * acesso ao banco, pois services e Tools o limpam ao terminar.
     */
    @Override
    public AiChatResponse chat(UUID companyId, UUID userId, List<String> permissions, AiChatRequest request) {
        try {
            TenantContext.setCompanyId(companyId);

            AiConversation conversation = resolveConversation(companyId, userId, request);
            ResolvedAiContext resolvedContext = contextResolver.resolve(companyId, userId, permissions, request.context());
            String crmContext = resolvedContext.crmContext();
            TenantContext.setCompanyId(companyId);

            chatRepository.saveMessage(AiMessage.create(companyId, conversation.getId(), ROLE_USER, request.message()));

List<AiProvider.ChatMessage> messages = buildPrompt(conversation, crmContext, request.message(),
                    resolvedContext.application());
            ChatLoopResult loop = runToolLoop(companyId, userId, permissions, conversation.getId(), messages);

            // Tools chamam services que limpam o TenantContext em finally;
            // re-estabelecemos antes das operações de persistência do orquestrador.
            TenantContext.setCompanyId(companyId);

            chatRepository.saveMessage(AiMessage.create(companyId, conversation.getId(), ROLE_ASSISTANT, loop.content()));
            conversation.touch();
            chatRepository.saveConversation(conversation);

            audit(companyId, userId, conversation, request, crmContext);

            return new AiChatResponse(conversation.getId(), loop.content(), aiProvider.providerName(),
                    loop.actions());
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiConversationResponse> listConversations(UUID companyId, UUID userId) {
        try {
            TenantContext.setCompanyId(companyId);
            return chatRepository.findConversationsByUser(companyId, userId).stream()
                    .map(AiConversationResponse::from)
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiMessageResponse> getConversationMessages(UUID companyId, UUID userId, UUID conversationId) {
        try {
            TenantContext.setCompanyId(companyId);
            AiConversation conversation = chatRepository.findConversationById(conversationId)
                    .orElseThrow(() -> new AiConversationNotFoundException(
                            "Conversa não encontrada ou sem acesso."));
            if (!conversation.getCompanyId().equals(companyId) || !conversation.getUserId().equals(userId)) {
                throw new AiConversationNotFoundException("Conversa não encontrada ou sem acesso.");
            }
            return chatRepository.findMessagesByConversation(conversationId).stream()
                    .filter(m -> ROLE_USER.equals(m.getRole()) || ROLE_ASSISTANT.equals(m.getRole()))
                    .map(AiMessageResponse::from)
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

/**
     * Loop de Tool Calling (AI-03): envia o prompt ao modelo com as Tools
     * registradas; se o modelo solicitar uma ou mais Tools, cada uma é
     * executada pelo backend (via {@link AiToolRegistry}, com verificação de
     * permissão) e o resultado estruturado retorna ao modelo. Repete até o
     * modelo produzir texto final ou atingir o limite de iterações.
     *
     * <p>AI-05: a conversa em curso é passada no {@link AiToolContext} para que
     * write tools vinculem propostas a ela. Propostas criadas durante o loop
     * são coletadas ({@code AiActionResponse}) e retornadas na resposta.</p>
     */
    private ChatLoopResult runToolLoop(UUID companyId, UUID userId, List<String> permissions,
                                       UUID conversationId, List<AiProvider.ChatMessage> messages) {
        AiToolContext toolCtx = new AiToolContext(companyId, userId, new AiPermissionContext(permissions),
                conversationId);
        List<AiProvider.ChatMessage> working = new ArrayList<>(messages);
        List<AiActionResponse> actions = new ArrayList<>();

        AiProvider.ChatResult result = aiProvider.chatWithTools(new AiProvider.ChatRequest(
                companyId, userId, working, toolRegistry.toolDefinitions()));

        // Propostas já criadas neste turno, por ferramenta + parâmetros normalizados:
        // uma repetição devolve a proposta existente em vez de criar outra AiAction.
        Map<String, AiToolResult> proposalsThisTurn = new HashMap<>();

        int iterations = 0;
        while (result.hasToolCalls() && iterations < MAX_TOOL_ITERATIONS) {
            iterations++;
            // Protocolo OpenAI: a mensagem do assistente que solicitou as tools
            // deve ser devolvida COM o array tool_calls; cada resposta de Tool
            // referencia a chamada correspondente via tool_call_id.
            working.add(new AiProvider.ChatMessage(ROLE_ASSISTANT, result.content(), result.toolCalls(), null));
            for (AiProvider.ToolCall call : result.toolCalls()) {
                String key = proposalKey(call);
                AiToolResult toolResult = proposalsThisTurn.get(key);
                if (toolResult == null) {
                    toolResult = toolRegistry.execute(call.name(), toolCtx, call.arguments());
                    TenantContext.setCompanyId(companyId);
                    if (toolResult.success() && toolResult.data() instanceof AiActionResponse proposal) {
                        actions.add(proposal);
                        proposalsThisTurn.put(key, toolResult);
                    }
                    auditToolCall(companyId, userId, call.name(), toolResult);
                }
                working.add(new AiProvider.ChatMessage(ROLE_TOOL, serialize(toolResult), null, call.id()));
            }
            result = aiProvider.chatWithTools(new AiProvider.ChatRequest(
                    companyId, userId, working, toolRegistry.toolDefinitions()));
        }

        if (result.hasToolCalls()) {
            // Limite atingido com tools pendentes: uma última chamada SEM ferramentas
            // pedindo o resumo do que foi feito e do que falta.
            working.add(new AiProvider.ChatMessage("system", FINAL_SUMMARY_INSTRUCTION));
            result = aiProvider.chatWithTools(new AiProvider.ChatRequest(
                    companyId, userId, working, List.of()));
        }

        String content = result.content();
        if (content == null || content.isBlank()) {
            content = fallbackFromProposals(actions);
        }
        return new ChatLoopResult(content, actions);
    }

    private String proposalKey(AiProvider.ToolCall call) {
        Map<String, Object> args = call.arguments() == null ? Map.of() : new TreeMap<>(call.arguments());
        return call.name() + "|" + args;
    }

    static String fallbackFromProposals(List<AiActionResponse> actions) {
        if (actions.isEmpty()) {
            return NO_FINAL_ANSWER;
        }
        StringBuilder sb = new StringBuilder("Preparei ").append(actions.size())
                .append(actions.size() == 1 ? " ação" : " ações").append(" para você confirmar: ");
        for (int i = 0; i < actions.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(actions.get(i).description());
        }
        return sb.append('.').toString();
    }

    private record ChatLoopResult(String content, List<AiActionResponse> actions) {
    }

    private String serialize(AiToolResult result) {
        if (!result.success()) {
            return "{\"success\":false,\"error\":\"" + safeJson(result.error()) + "\"}";
        }
        if (result.data() == null) {
            return "{\"success\":true,\"data\":null}";
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(result.data());
        } catch (Exception e) {
            return "{\"success\":true,\"data\":\"" + safeJson(result.data().toString()) + "\"}";
        }
    }

    private String safeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void auditToolCall(UUID companyId, UUID userId, String toolName, AiToolResult result) {
        try {
            auditor.record(companyId, AuditAction.CUSTOM, AuditModule.AI, "AiTool",
                    toolName,
                    (result.success() ? "Tool executada com sucesso: " : "Tool falhou: ") + toolName,
                    userId,
                    Map.of("tool", toolName, "success", result.success()));
        } catch (Exception e) {
            log.warn("Falha ao registrar auditoria de Tool {} (company={}, userId={}): {}",
                    toolName, companyId, userId, e.getMessage());
        }
    }

    private AiConversation resolveConversation(UUID companyId, UUID userId, AiChatRequest request) {
        if (request.conversationId() == null) {
            AiContextPayload p = request.context();
            String screen = p != null ? p.screen() : null;
            UUID recordId = p != null ? p.recordId() : null;
            return chatRepository.saveConversation(
                    AiConversation.create(companyId, userId, screen, recordId, firstWords(request.message())));
        }
        AiConversation existing = chatRepository.findConversationById(request.conversationId())
                .orElseThrow(() -> new AiConversationNotFoundException(
                        "Conversa não encontrada ou sem acesso."));
        if (!existing.getCompanyId().equals(companyId) || !existing.getUserId().equals(userId)) {
            throw new AiConversationNotFoundException("Conversa não encontrada ou sem acesso.");
        }
        return existing;
    }

    private List<AiProvider.ChatMessage> buildPrompt(AiConversation conversation, String context,
                                                     String userMessage, AiApplicationContext application) {
        List<AiProvider.ChatMessage> messages = new ArrayList<>();
        messages.add(new AiProvider.ChatMessage("system", systemPrompt(application)));

        if (context != null && !context.isBlank()) {
            messages.add(new AiProvider.ChatMessage("system", context));
        }

        List<AiMessage> history = chatRepository.findMessagesByConversation(conversation.getId());
        int start = Math.max(0, history.size() - HISTORY_LIMIT);
        for (int i = start; i < history.size(); i++) {
            AiMessage m = history.get(i);
            if (!ROLE_USER.equals(m.getRole()) && !ROLE_ASSISTANT.equals(m.getRole())) {
                continue;
            }
            messages.add(new AiProvider.ChatMessage(m.getRole(), m.getContent()));
        }

        messages.add(new AiProvider.ChatMessage(ROLE_USER, userMessage));
        return messages;
    }

    /** Relógio do prompt (data/hora atual); substituível em testes. */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    String systemPrompt(AiApplicationContext application) {
        StringBuilder sb = new StringBuilder()
                .append("Você é o Léo, o assistente de inteligência comercial de um CRM (Customer 360). ")
                .append("Quando alguém se dirigir a você ou pedir para se apresentar, use o nome Léo. ")
                .append("Responda em português, de forma objetiva e profissional, com base APENAS nos ")
                .append("dados fornecidos no contexto e nos resultados das ferramentas. Se a informação não ")
                .append("estiver disponível, diga claramente que não tem os dados, sem inventar. ")
                .append("Nunca mencione que recebeu um bloco de contexto técnico.\n\n")
                .append("Ações: você consulta dados com as ferramentas. Para desmarcar ou remarcar consultas, ")
                .append("criar tarefas/atividades, alterar oportunidades, lançar receitas/despesas, dar baixa ")
                .append("em lançamentos e registrar movimentações de estoque, use a ferramenta correspondente: ")
                .append("ela cria uma PROPOSTA que o usuário confirma no card exibido no chat. Nunca diga que ")
                .append("já executou — diga que deixou pronto para confirmação. Antes de desmarcar ou ")
                .append("remarcar, localize a consulta com search_appointments; se houver mais de uma ")
                .append("possível, pergunte qual. Use SEMPRE o id exato devolvido pela busca — nunca ")
                .append("invente ids. Se o usuário perguntar se algo já foi feito (ex.: \"a consulta foi ")
                .append("desmarcada?\"), CONSULTE com a ferramenta de busca e informe o status; não repita a ")
                .append("ação. Horários são sempre no fuso de Brasília.\n\n")
                .append("Limites: se não houver ferramenta para o que foi pedido, diga claramente \"isso eu ainda ")
                .append("não consigo fazer\" — nunca dê a entender que fez. Pedidos com dependência (ex.: ")
                .append("paciente novo e depois procedimento ou consulta para ele): proponha primeiro o cadastro e ")
                .append("explique que os próximos passos vêm depois da confirmação. Não repita uma proposta ")
                .append("idêntica já feita nesta resposta.\n\n");

        ZonedDateTime now = ZonedDateTime.now(clock.withZone(CLINIC_ZONE));
        sb.append("Agora é ").append(now.format(NOW_FORMAT)).append(" (horário de Brasília).\n");
        AiScreenCatalog.Screen current = application == null ? null : AiScreenCatalog.find(application.route());
        if (current != null) {
            sb.append("O usuário está na tela: ").append(current.name()).append(" (")
                    .append(application.route()).append(").\n");
        }
        sb.append('\n').append(AiScreenCatalog.render())
                .append("\nPara indicar uma tela, cite o nome e a rota (ex.: Agenda — /agenda).");
        return sb.toString();
    }

    private String firstWords(String message) {
        if (message == null) {
            return "Nova conversa";
        }
        String trimmed = message.trim();
        if (trimmed.length() <= 60) {
            return trimmed;
        }
        return trimmed.substring(0, 60) + "...";
    }

    private void audit(UUID companyId, UUID userId, AiConversation conversation, AiChatRequest request,
                       String context) {
        try {
            auditor.record(companyId, AuditAction.CUSTOM, AuditModule.AI, "AiConversation",
                    conversation.getId().toString(),
                    "Assistente de IA: " + request.message(),
                    userId,
                    Map.of("screen", conversation.getScreen() != null ? conversation.getScreen() : "",
                            "recordId", conversation.getRecordId() != null ? conversation.getRecordId().toString() : "",
                            "hasContext", context != null && !context.isBlank(),
                            "provider", aiProvider.providerName()));
        } catch (Exception e) {
            log.warn("Falha ao registrar auditoria de IA (company={}, userId={}): {}",
                    companyId, userId, e.getMessage());
        }
    }
}
