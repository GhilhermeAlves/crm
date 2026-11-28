package com.becommerce.crm.clinical.trichology.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataTrichologyAiRuns extends JpaRepository<TrichologyJpaEntities.AiRunEntity, UUID> {
    List<TrichologyJpaEntities.AiRunEntity> findByCompanyIdAndAssessmentIdOrderByCreatedAt(UUID companyId,
                                                                                          UUID assessmentId);
}
