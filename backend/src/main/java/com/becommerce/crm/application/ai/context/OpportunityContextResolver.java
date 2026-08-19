package com.becommerce.crm.application.ai.context;

import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.application.pipeline.port.output.StageRepository;
import com.becommerce.crm.domain.ai.AiRecordType;
import com.becommerce.crm.domain.contact.Contact;
import com.becommerce.crm.domain.pipeline.Opportunity;
import com.becommerce.crm.domain.pipeline.OpportunityStatus;
import com.becommerce.crm.domain.pipeline.Stage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolver de contexto para {@code OPPORTUNITY} (AI-02). Monta o contexto da
 * oportunidade em foco: título, valor, estágio/probabilidade, status, contato
 * vinculado e prazo de fechamento. Exige {@code opportunity:read}.
 */
@Component
public class OpportunityContextResolver implements AiRecordContextResolver {

    public static final String PERMISSION = "opportunity:read";

    private final OpportunityRepository opportunityRepository;
    private final StageRepository stageRepository;
    private final ContactRepository contactRepository;

    public OpportunityContextResolver(OpportunityRepository opportunityRepository,
                                      StageRepository stageRepository,
                                      ContactRepository contactRepository) {
        this.opportunityRepository = opportunityRepository;
        this.stageRepository = stageRepository;
        this.contactRepository = contactRepository;
    }

    @Override
    public AiRecordType type() {
        return AiRecordType.OPPORTUNITY;
    }

    @Override
    public String requiredPermission() {
        return PERMISSION;
    }

    @Override
    public String resolve(UUID companyId, UUID recordId) {
        Optional<Opportunity> maybe = opportunityRepository.findById(recordId);
        if (maybe.isEmpty()) {
            return null;
        }
        Opportunity o = maybe.get();
        StringBuilder sb = new StringBuilder();
        sb.append("Oportunidade: ").append(o.getTitle()).append('\n');
        sb.append("Valor: R$ ").append(safe(o.getValue())).append('\n');
        sb.append("Status: ").append(statusLabel(o.getStatus())).append('\n');

        if (o.getStageId() != null) {
            stageRepository.findById(o.getStageId()).ifPresent(stage -> {
                sb.append("Estágio: ").append(stage.getName()).append('\n');
                sb.append("Probabilidade: ").append(stage.getProbability()).append("%").append('\n');
            });
        }

        if (o.getExpectedCloseDate() != null) {
            sb.append("Previsão de fechamento: ").append(o.getExpectedCloseDate().toLocalDate()).append('\n');
        }
        if (o.getContactId() != null) {
            contactRepository.findById(o.getContactId()).ifPresent(c ->
                    sb.append("Contato vinculado: ").append(fullName(c)).append('\n'));
        }
        if (o.getLossReason() != null && !o.getLossReason().isBlank()) {
            sb.append("Motivo da perda: ").append(o.getLossReason()).append('\n');
        }
        if (o.getNotes() != null && !o.getNotes().isBlank()) {
            sb.append("Notas: ").append(o.getNotes()).append('\n');
        }
        return sb.toString();
    }

    private static String statusLabel(OpportunityStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case OPEN -> "ABERTA";
            case WON -> "GANHA";
            case LOST -> "PERDIDA";
        };
    }

    private static String fullName(Contact c) {
        return (c.getFirstName() + " " + (c.getLastName() == null ? "" : c.getLastName())).trim();
    }

    private static String safe(BigDecimal value) {
        return value == null ? "0,00" : value.toPlainString();
    }
}