package com.becommerce.crm.automation.ai.application.agent;

import com.becommerce.crm.automation.ai.application.agent.context.AgentContext;
import com.becommerce.crm.automation.ai.application.agent.context.ClinicContext;
import com.becommerce.crm.automation.ai.application.agent.context.ConversationHistory;
import com.becommerce.crm.automation.ai.application.agent.context.KnowledgeContext;
import com.becommerce.crm.automation.ai.application.agent.context.PatientContext;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.domain.AgentBehavior;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import com.becommerce.crm.automation.ai.domain.AgentIdentity;
import com.becommerce.crm.automation.ai.domain.AgentMemory;
import com.becommerce.crm.automation.ai.domain.MemoryType;
import com.becommerce.crm.shared.calendar.BrazilianHolidays;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Converte um {@link AgentContext} em mensagens do provider (formato chat com
 * tool calling). Estrutura, em ordem:
 * <ol>
 *   <li>{@code system} — configuração permanente: identidade + comportamento
 *       (ou o prompt legado, intacto, quando não há perfil estruturado);</li>
 *   <li>{@code system} — contexto dinâmico em seções rotuladas: agora, clínica,
 *       paciente, memórias relevantes, conhecimento, canal;</li>
 *   <li>{@code system} — regras de uso das ferramentas disponíveis (as
 *       definições vão estruturadas no campo {@code tools} da requisição);</li>
 *   <li>histórico recente como mensagens {@code user}/{@code assistant} reais;</li>
 *   <li>mensagem atual ({@code user}).</li>
 * </ol>
 * Responsabilidades continuam separadas no {@link AgentContext}; só aqui viram texto.
 */
public final class AgentContextRenderer {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter NOW_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", PT_BR);
    private static final DateTimeFormatter APPOINTMENT_FORMAT = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm", PT_BR);
    private static final DateTimeFormatter HOLIDAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM", PT_BR);
    static final int HOLIDAY_LOOKAHEAD_DAYS = 30;

    private AgentContextRenderer() {
    }

    public static List<AiProvider.ChatMessage> render(AgentContext ctx) {
        List<AiProvider.ChatMessage> messages = new ArrayList<>();
        messages.add(system(agentConfiguration(ctx.agent())));
        messages.add(system(dynamicContext(ctx)));
        if (!ctx.tools().isEmpty() && !ctx.tools().guidance().isEmpty()) {
            messages.add(system(String.join("\n\n", ctx.tools().guidance())));
        }
        for (ConversationHistory.Entry entry : ctx.history().entries()) {
            messages.add(new AiProvider.ChatMessage(entry.role(), entry.content()));
        }
        messages.add(new AiProvider.ChatMessage("user", ctx.currentMessage()));
        return messages;
    }

    /** Camada permanente: quem o agente é e como se comporta. */
    static String agentConfiguration(AgentConfig agent) {
        if (!agent.hasStructuredProfile()) {
            return agent.getLegacyPrompt();
        }
        StringBuilder sb = new StringBuilder();
        AgentIdentity identity = agent.getIdentity();
        if (!identity.isEmpty()) {
            sb.append("IDENTIDADE\n");
            line(sb, "Nome", identity.name());
            line(sb, "Descrição", identity.description());
            if (identity.persona() != null) {
                sb.append(identity.persona()).append('\n');
            }
        }
        AgentBehavior behavior = agent.getBehavior();
        if (!behavior.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append("COMPORTAMENTO\n");
            line(sb, "Objetivo", behavior.objective());
            line(sb, "Tom de voz", behavior.tone());
            bullets(sb, "Regras", behavior.rules());
            bullets(sb, "Instruções", behavior.instructions());
        }
        return sb.toString().trim();
    }

    /** Camada dinâmica: dados reais desta interação, sem misturar com a persona. */
    static String dynamicContext(AgentContext ctx) {
        StringBuilder sb = new StringBuilder("Contexto desta conversa (dados atuais do CRM; não repita literalmente):\n");
        ClinicContext clinic = ctx.clinic();
        sb.append("- Agora: ").append(now(clinic.now())).append(" (").append(clinic.zone().getId()).append(").\n");
        List<BrazilianHolidays.Holiday> holidays = BrazilianHolidays.between(clinic.now().toLocalDate(),
                clinic.now().toLocalDate().plusDays(HOLIDAY_LOOKAHEAD_DAYS));
        if (!holidays.isEmpty()) {
            sb.append("- Feriados nacionais próximos (sem atendimento): ").append(String.join(", ", holidays.stream()
                    .map(h -> h.date().getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR) + " "
                            + h.date().format(HOLIDAY_FORMAT) + " (" + h.name() + ")").toList())).append(".\n");
        }

        if (clinic.name() != null || clinic.phone() != null || clinic.address() != null
                || clinic.businessHours() != null) {
            sb.append("\nCLÍNICA\n");
            line(sb, "- Nome", clinic.name());
            line(sb, "- Telefone", clinic.phone());
            line(sb, "- Endereço", clinic.address());
            line(sb, "- Horário de funcionamento", clinic.businessHours());
        }

        PatientContext patient = ctx.patient();
        sb.append("\nPACIENTE\n");
        line(sb, "- Nome no cadastro", patient.name());
        line(sb, "- Nome do perfil no WhatsApp", patient.profileName());
        patient.firstName().ifPresentOrElse(
                first -> sb.append("- Como chamar o paciente: \"").append(first)
                        .append("\". Cumprimente usando esse primeiro nome e volte a usá-lo de vez em quando ")
                        .append("(ao confirmar um horário, na despedida), sem repetir em toda mensagem.\n"),
                () -> sb.append("- Nome do paciente ainda desconhecido: não invente um nome; quando for agendar, ")
                        .append("pergunte o nome de forma natural.\n"));
        if (!patient.isIdentified()) {
            sb.append("- Contato ainda não identificado no cadastro.\n");
        } else if (patient.upcomingAppointments().isEmpty()) {
            sb.append("- Sem agendamentos futuros na agenda.\n");
        } else {
            sb.append("- Próximos agendamentos (agenda do CRM):\n");
            for (PatientContext.UpcomingAppointment a : patient.upcomingAppointments()) {
                sb.append("  • ").append(a.title()).append(" — ").append(weekday(a.start())).append(" ")
                        .append(a.start().format(APPOINTMENT_FORMAT))
                        .append(a.status() != null ? " (" + statusLabel(a.status()) + ")" : "").append('\n');
            }
        }

        if (!ctx.memory().memories().isEmpty()) {
            sb.append("\nMEMÓRIAS RELEVANTES (de atendimentos anteriores; se divergirem do CRM, vale o CRM)\n");
            for (AgentMemory m : ctx.memory().memories()) {
                sb.append("- [").append(memoryLabel(m.getType())).append("] ").append(m.getContent()).append('\n');
            }
        }

        KnowledgeContext knowledge = ctx.knowledge();
        if (!knowledge.snippets().isEmpty()) {
            sb.append("\nCONHECIMENTO\n");
            for (KnowledgeContext.Snippet s : knowledge.snippets()) {
                sb.append("- ").append(s.source() != null ? "(" + s.source() + ") " : "").append(s.content()).append('\n');
            }
        }

        sb.append("\nCANAL\n- Escreva como uma pessoa no WhatsApp: frases curtas e naturais, sem formatação de lista longa, ")
                .append("e não repita a saudação ou o menu se a conversa já começou.");
        return sb.toString();
    }

    private static AiProvider.ChatMessage system(String content) {
        return new AiProvider.ChatMessage("system", content);
    }

    private static void line(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(value.trim()).append('\n');
        }
    }

    private static void bullets(StringBuilder sb, String label, List<String> items) {
        if (items.isEmpty()) {
            return;
        }
        sb.append(label).append(":\n");
        for (String item : items) {
            sb.append("- ").append(item).append('\n');
        }
    }

    private static String now(ZonedDateTime now) {
        return weekday(now) + ", " + now.format(NOW_FORMAT);
    }

    private static String weekday(ZonedDateTime when) {
        return when.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR);
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "CONFIRMED" -> "confirmado";
            case "SCHEDULED" -> "agendado";
            case "COMPLETED" -> "realizado";
            default -> status.toLowerCase(Locale.ROOT);
        };
    }

    private static String memoryLabel(MemoryType type) {
        return switch (type) {
            case PREFERENCE -> "Preferência";
            case FACT -> "Fato";
            case PROFILE -> "Perfil";
            case CONTEXTUAL -> "Pendência";
        };
    }
}
