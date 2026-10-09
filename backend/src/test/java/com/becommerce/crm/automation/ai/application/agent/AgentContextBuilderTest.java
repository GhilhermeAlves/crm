package com.becommerce.crm.automation.ai.application.agent;

import com.becommerce.crm.automation.ai.application.agent.context.AgentContext;
import com.becommerce.crm.automation.ai.application.agent.context.AgentRuntimeInput;
import com.becommerce.crm.automation.ai.application.agent.context.ClinicContext;
import com.becommerce.crm.automation.ai.application.agent.context.ClinicContextProvider;
import com.becommerce.crm.automation.ai.application.agent.context.ConversationHistory;
import com.becommerce.crm.automation.ai.application.agent.context.KnowledgeContext;
import com.becommerce.crm.automation.ai.application.agent.context.PatientContext;
import com.becommerce.crm.automation.ai.application.agent.context.PatientContextProvider;
import com.becommerce.crm.automation.ai.application.agent.knowledge.KnowledgeRetriever;
import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolProvider;
import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolSession;
import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolbox;
import com.becommerce.crm.automation.ai.application.memory.AgentMemoryService;
import com.becommerce.crm.automation.ai.application.memory.MemoryQuery;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.domain.AgentBehavior;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import com.becommerce.crm.automation.ai.domain.AgentIdentity;
import com.becommerce.crm.automation.ai.domain.AgentMemory;
import com.becommerce.crm.automation.ai.domain.MemorySource;
import com.becommerce.crm.automation.ai.domain.MemoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O contexto final é montado centralmente e contém, separadas e em ordem:
 * identidade, comportamento, clínica, paciente, histórico, memórias,
 * ferramentas e mensagem atual.
 */
class AgentContextBuilderTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final UUID companyId = UUID.randomUUID();
    private final UUID contactId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();

    private final ClinicContextProvider clinicProvider = mock(ClinicContextProvider.class);
    private final PatientContextProvider patientProvider = mock(PatientContextProvider.class);
    private final AgentMemoryService memoryService = mock(AgentMemoryService.class);
    private final KnowledgeRetriever knowledgeRetriever = mock(KnowledgeRetriever.class);

    private final AgentToolProvider agendaTool = new AgentToolProvider() {
        @Override
        public String id() {
            return "agenda";
        }

        @Override
        public boolean isAvailable(AgentConfig agent, AgentToolSession session) {
            return true;
        }

        @Override
        public List<AiProvider.ToolDefinition> definitions() {
            return List.of(new AiProvider.ToolDefinition("consultar_horarios_livres", "Consulta a agenda", Map.of()));
        }

        @Override
        public Optional<String> guidance(AgentConfig agent, AgentToolSession session) {
            return Optional.of("Use SEMPRE consultar_horarios_livres antes de oferecer horários.");
        }

        @Override
        public String execute(AgentToolSession session, AiProvider.ToolCall call) {
            return "ok:" + session.companyId();
        }
    };

    private AgentContextBuilder builder;
    private AgentConfig anaLaura;

    @BeforeEach
    void setUp() {
        builder = new AgentContextBuilder(clinicProvider, patientProvider, memoryService, knowledgeRetriever,
                new AgentToolbox(List.of(agendaTool)));
        anaLaura = AgentConfig.reconstitute(UUID.randomUUID(), companyId, true, true, "PROMPT LEGADO", null, null,
                        null, 60, 1000, LocalDateTime.now(), LocalDateTime.now())
                .withProfile(new AgentIdentity("Ana Laura", "Assistente virtual da clínica odontológica.",
                                "Você é Ana Laura, uma assistente virtual."),
                        new AgentBehavior("Atender pacientes e auxiliar no agendamento.",
                                "Profissional, acolhedor e objetivo.",
                                List.of("Não inventar informações.", "Não confirmar agendamento sem consultar a agenda."),
                                List.of("Confirmar data e horário antes de finalizar.")),
                        true, false);

        when(clinicProvider.load(companyId)).thenReturn(new ClinicContext("Clínica Sorriso", "(11) 4000-0000",
                "Rua A, 10 - Centro - São Paulo/SP", "Seg a sex, 08:00 às 18:00", ZONE,
                ZonedDateTime.of(2026, 10, 8, 9, 0, 0, 0, ZONE)));
        when(patientProvider.load(eq(companyId), eq(contactId), any(), any(), any())).thenReturn(
                new PatientContext(contactId, "Carlos Silva", "Carlos", List.of(new PatientContext.UpcomingAppointment(
                        "Limpeza — Carlos", ZonedDateTime.of(2026, 10, 15, 10, 0, 0, 0, ZONE), "CONFIRMED"))));
        when(memoryService.retrieveRelevant(any())).thenReturn(List.of(AgentMemory.create(companyId,
                anaLaura.getId(), contactId, MemoryType.PREFERENCE, "Prefere atendimento pela manhã.", 4,
                MemorySource.AGENT, null, Map.of(), null)));
        when(knowledgeRetriever.retrieve(any(), any(), anyString())).thenReturn(KnowledgeContext.EMPTY);
    }

    private AgentRuntimeInput input(UUID contact) {
        return new AgentRuntimeInput(companyId, conversationId, contact, "5511999990000", "Carlos",
                UUID.randomUUID(), "Quero marcar uma consulta.", new ConversationHistory(List.of(
                new ConversationHistory.Entry("user", "Oi"),
                new ConversationHistory.Entry("assistant", "Olá! Como posso ajudar?"))));
    }

    @Test
    void build_collectsEverySectionSeparately() {
        AgentContext ctx = builder.build(anaLaura, input(contactId));

        assertEquals("Ana Laura", ctx.agent().getIdentity().name());
        assertEquals("Clínica Sorriso", ctx.clinic().name());
        assertEquals("Carlos Silva", ctx.patient().name());
        assertEquals(2, ctx.history().entries().size());
        assertEquals(1, ctx.memory().memories().size());
        assertEquals(1, ctx.tools().definitions().size());
        assertEquals("Quero marcar uma consulta.", ctx.currentMessage());
        verify(memoryService).retrieveRelevant(new MemoryQuery(companyId, anaLaura.getId(), contactId,
                "Quero marcar uma consulta.", MemoryQuery.DEFAULT_LIMIT));
    }

    @Test
    void render_ordersSystemContextHistoryAndCurrentMessage() {
        List<AiProvider.ChatMessage> messages = AgentContextRenderer.render(builder.build(anaLaura, input(contactId)));

        // 0: identidade+comportamento | 1: contexto dinâmico | 2: ferramentas | 3-4: histórico | 5: atual
        assertEquals(6, messages.size());
        String config = messages.get(0).content();
        assertTrue(config.indexOf("IDENTIDADE") < config.indexOf("COMPORTAMENTO"), config);
        assertTrue(config.contains("Nome: Ana Laura"), config);
        assertTrue(config.contains("Objetivo: Atender pacientes"), config);
        assertTrue(config.contains("Tom de voz: Profissional"), config);
        assertTrue(config.contains("- Não confirmar agendamento sem consultar a agenda."), config);
        assertTrue(config.contains("Instruções:\n- Confirmar data e horário"), config);
        assertFalse(config.contains("PROMPT LEGADO"), "perfil estruturado substitui o legado");
        assertFalse(config.contains("Clínica Sorriso"), "dados da clínica não entram na persona");

        String dynamic = messages.get(1).content();
        assertTrue(dynamic.indexOf("CLÍNICA") < dynamic.indexOf("PACIENTE"), dynamic);
        assertTrue(dynamic.indexOf("PACIENTE") < dynamic.indexOf("MEMÓRIAS RELEVANTES"), dynamic);
        assertTrue(dynamic.contains("Horário de funcionamento: Seg a sex, 08:00 às 18:00"), dynamic);
        assertTrue(dynamic.contains("Nome no cadastro: Carlos Silva"), dynamic);
        assertTrue(dynamic.contains("Como chamar o paciente: \"Carlos\""), dynamic);
        assertTrue(dynamic.contains("Limpeza — Carlos — quinta-feira 15/10 às 10:00 (confirmado)"), dynamic);
        assertTrue(dynamic.contains("[Preferência] Prefere atendimento pela manhã."), dynamic);
        assertTrue(dynamic.contains("quinta-feira, 08/10/2026 às 09:00"), dynamic);

        assertTrue(messages.get(2).content().contains("consultar_horarios_livres"));
        assertEquals("user", messages.get(3).role());
        assertEquals("assistant", messages.get(4).role());
        assertEquals(new AiProvider.ChatMessage("user", "Quero marcar uma consulta."), messages.get(5));
    }

    @Test
    void legacyAgent_rendersPromptVerbatim() {
        AgentConfig legacy = AgentConfig.reconstitute(UUID.randomUUID(), companyId, true, true,
                "Você é a recepcionista.", null, null, null, 60, 1000, LocalDateTime.now(), LocalDateTime.now());

        List<AiProvider.ChatMessage> messages = AgentContextRenderer.render(builder.build(legacy, input(contactId)));

        assertEquals("Você é a recepcionista.", messages.get(0).content());
    }

    @Test
    void memoryDisabled_doesNotRetrieveMemories() {
        AgentConfig noMemory = anaLaura.withProfile(null, null, false, null);

        AgentContext ctx = builder.build(noMemory, input(contactId));

        assertTrue(ctx.memory().memories().isEmpty());
        verify(memoryService, never()).retrieveRelevant(any());
    }

    @Test
    void unidentifiedContact_hasNoMemoriesAndNoMemoryScope() {
        when(patientProvider.load(eq(companyId), eq(null), any(), any(), any()))
                .thenReturn(PatientContext.unknown("Carlos"));

        AgentContext ctx = builder.build(anaLaura, input(null));

        assertFalse(ctx.patient().isIdentified());
        assertTrue(ctx.memory().memories().isEmpty());
        verify(memoryService, never()).retrieveRelevant(any());
        String dynamic = AgentContextRenderer.render(ctx).get(1).content();
        assertTrue(dynamic.contains("Contato ainda não identificado"), dynamic);
        assertTrue(dynamic.contains("Como chamar o paciente: \"Carlos\""), "usa o nome do perfil sem cadastro");
    }

    @Test
    void patientFirstName_prefersCrmNameAndIgnoresProfilesThatAreNotNames() {
        assertEquals(Optional.of("Raquel"),
                new PatientContext(contactId, "RAQUEL aguiar", "Loja da Raquel", List.of()).firstName());
        assertEquals(Optional.of("João"), PatientContext.unknown("joão pedro").firstName());
        assertEquals(Optional.empty(), PatientContext.unknown("🦷 Sorriso").firstName());
        assertEquals(Optional.empty(), PatientContext.unknown("Loja2024").firstName());
        assertEquals(Optional.empty(), PatientContext.unknown(null).firstName());
    }

    @Test
    void unknownName_tellsAgentNotToInventOne() {
        when(patientProvider.load(eq(companyId), eq(null), any(), any(), any()))
                .thenReturn(PatientContext.unknown("🦷"));

        String dynamic = AgentContextRenderer.render(builder.build(anaLaura, input(null))).get(1).content();

        assertTrue(dynamic.contains("Nome do paciente ainda desconhecido"), dynamic);
        assertFalse(dynamic.contains("Como chamar o paciente"), dynamic);
    }

    @Test
    void toolSession_usesRuntimeScopeNotModelArguments() {
        AgentContext ctx = builder.build(anaLaura, input(contactId));
        AgentToolSession session = builder.toolSession(anaLaura, input(contactId), ctx.patient());

        String result = ctx.tools().execute(session, new AiProvider.ToolCall("1", "consultar_horarios_livres",
                Map.of("companyId", UUID.randomUUID().toString())));

        assertEquals("ok:" + companyId, result);
        assertEquals(contactId, session.contactId());
        assertEquals("Ferramenta indisponível: inexistente",
                ctx.tools().execute(session, new AiProvider.ToolCall("2", "inexistente", Map.of())));
    }

    @Test
    void failingOptionalSources_degradeWithoutBreakingContext() {
        when(memoryService.retrieveRelevant(any())).thenThrow(new RuntimeException("db down"));
        when(knowledgeRetriever.retrieve(any(), any(), anyString())).thenThrow(new RuntimeException("kb down"));

        AgentContext ctx = builder.build(anaLaura, input(contactId));

        assertTrue(ctx.memory().memories().isEmpty());
        assertTrue(ctx.knowledge().snippets().isEmpty());
        assertEquals("Quero marcar uma consulta.", ctx.currentMessage());
    }
}
