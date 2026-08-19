package com.becommerce.crm.application.ai.context;

import com.becommerce.crm.application.activity.port.output.ActivityRepository;
import com.becommerce.crm.domain.activity.Activity;
import com.becommerce.crm.domain.ai.AiRecordType;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolver de contexto para {@code ACTIVITY} (AI-02). Monta o contexto da
 * atividade em foco: tipo, assunto, descrição e data. Exige {@code activity:read}.
 */
@Component
public class ActivityContextResolver implements AiRecordContextResolver {

    public static final String PERMISSION = "activity:read";

    private final ActivityRepository activityRepository;

    public ActivityContextResolver(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Override
    public AiRecordType type() {
        return AiRecordType.ACTIVITY;
    }

    @Override
    public String requiredPermission() {
        return PERMISSION;
    }

    @Override
    public String resolve(UUID companyId, UUID recordId) {
        Optional<Activity> maybe = activityRepository.findById(recordId);
        if (maybe.isEmpty()) {
            return null;
        }
        Activity a = maybe.get();
        StringBuilder sb = new StringBuilder();
        sb.append("Atividade (").append(a.getType()).append("): ").append(a.getSubject()).append('\n');
        if (a.getDescription() != null && !a.getDescription().isBlank()) {
            sb.append("Detalhe: ").append(a.getDescription()).append('\n');
        }
        if (a.getActivityAt() != null) {
            sb.append("Data da atividade: ").append(a.getActivityAt()).append('\n');
        }
        return sb.toString();
    }
}