package com.becommerce.crm.application.ai.service;

import com.becommerce.crm.application.ai.dto.AiContextPayload;
import com.becommerce.crm.application.customer360.dto.ContactSummaryResponse;
import com.becommerce.crm.application.customer360.dto.Customer360Response;
import com.becommerce.crm.application.customer360.dto.NextActionResponse;
import com.becommerce.crm.application.customer360.dto.OpportunityItemResponse;
import com.becommerce.crm.application.customer360.dto.TaskItemResponse;
import com.becommerce.crm.application.customer360.service.Customer360Service;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resolvedor de contexto do assistente de IA (AI-01). Traduz a "dica de tela"
 * ({@link AiContextPayload}) em contexto de CRM REAL, reutilizando os services
 * existentes (nunca duplica regra de negócio).
 *
 * <p>O backend resolve o contexto a partir de {@code companyId} + {@code screen}
 * + {@code recordId} e carrega os dados de fato; não confia em texto enviado
 * pelo frontend. O contexto montado é injetado no prompt do modelo.
 */
@Component
public class AiContextResolver {

    /** Telas que resolvem para um Customer 360 (registro = contactId). */
    private static final String SCREEN_CUSTOMER = "customer";
    private static final String SCREEN_CUSTOMER360 = "customer360";
    private static final String SCREEN_CONTACT = "contact";

    private final Customer360Service customer360Service;

    public AiContextResolver(Customer360Service customer360Service) {
        this.customer360Service = customer360Service;
    }

    /**
     * Monta o bloco de contexto textual do registro em foco, ou {@code null} se
     * não há contexto de registro aplicável.
     */
    public String resolve(UUID companyId, AiContextPayload context) {
        if (context == null || context.recordId() == null) {
            return null;
        }
        String screen = context.screen() == null ? "" : context.screen();
        if (isCustomerScreen(screen)) {
            return customerContext(companyId, context.recordId());
        }
        return null;
    }

    private boolean isCustomerScreen(String screen) {
        return SCREEN_CUSTOMER.equalsIgnoreCase(screen)
                || SCREEN_CUSTOMER360.equalsIgnoreCase(screen)
                || SCREEN_CONTACT.equalsIgnoreCase(screen);
    }

    private String customerContext(UUID companyId, UUID contactId) {
        Customer360Response c360 = customer360Service.build(companyId, contactId);
        StringBuilder sb = new StringBuilder();

        ContactSummaryResponse c = c360.contact();
        sb.append("Cliente: ").append(c.fullName()).append('\n');
        if (c.email() != null) sb.append("E-mail: ").append(c.email()).append('\n');
        if (c.phone() != null) sb.append("Telefone: ").append(c.phone()).append('\n');
        if (c.notes() != null && !c.notes().isBlank()) sb.append("Notas: ").append(c.notes()).append('\n');
        sb.append("Risco: ").append(Boolean.TRUE.equals(c.atRisk()) ? "ALTO" : "BAIXO")
                .append(c.riskMessage() != null ? " (" + c.riskMessage() + ")" : "").append('\n');

        sb.append("Oportunidades abertas: ").append(c360.openOpportunities()).append('\n');
        sb.append("Valor potencial: R$ ").append(safe(c360.openValue())).append('\n');

        if (!c360.opportunities().isEmpty()) {
            sb.append("Oportunidades:\n");
            for (OpportunityItemResponse o : c360.opportunities()) {
                sb.append("  - ").append(o.title()).append(" | ").append(o.stageName())
                        .append(" | prob. ").append(o.probability()).append("%")
                        .append(" | valor R$ ").append(safe(o.value()))
                        .append(" | ").append(o.statusLabel()).append('\n');
            }
        }

        if (!c360.tasks().isEmpty()) {
            sb.append("Tarefas:\n");
            for (TaskItemResponse t : c360.tasks()) {
                sb.append("  - ").append(t.title()).append(" | ").append(t.status())
                        .append(t.overdue() ? " | VENCIDA" : "").append('\n');
            }
        }

        NextActionResponse n = c360.nextAction();
        if (n != null) {
            sb.append("Próxima ação recomendada: ").append(n.title())
                    .append(" — ").append(n.description()).append('\n');
        }

        return sb.toString();
    }

    private static String safe(BigDecimal value) {
        return value == null ? "0,00" : value.toPlainString();
    }
}