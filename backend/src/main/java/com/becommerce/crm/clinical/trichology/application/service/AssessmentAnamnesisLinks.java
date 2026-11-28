package com.becommerce.crm.clinical.trichology.application.service;

import com.becommerce.crm.clinical.anamnesis.application.service.AnamnesisCompletionListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Vínculo exame tricológico ↔ anamnese ({@code trichology_assessments.anamnesis_id}, V163).
 * Ao concluir uma anamnese, exames ainda abertos do paciente sem anamnese passam a usá-la.
 */
@Component
public class AssessmentAnamnesisLinks implements AnamnesisCompletionListener {

    private final JdbcTemplate jdbc;

    public AssessmentAnamnesisLinks(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UUID> anamnesisOf(UUID companyId, UUID assessmentId) {
        List<UUID> ids = jdbc.queryForList("SELECT anamnesis_id FROM trichology_assessments "
                + "WHERE company_id = ? AND id = ? AND anamnesis_id IS NOT NULL", UUID.class, companyId, assessmentId);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    public void link(UUID companyId, UUID assessmentId, UUID anamnesisId) {
        jdbc.update("UPDATE trichology_assessments SET anamnesis_id = ? WHERE company_id = ? AND id = ?",
                anamnesisId, companyId, assessmentId);
    }

    @Override
    public void onCompleted(UUID companyId, UUID contactId, UUID anamnesisId) {
        jdbc.update("UPDATE trichology_assessments SET anamnesis_id = ? WHERE company_id = ? AND contact_id = ? "
                + "AND anamnesis_id IS NULL AND status NOT IN ('PUBLISHED', 'CANCELLED')",
                anamnesisId, companyId, contactId);
    }
}
