package com.becommerce.crm.clinical.trichology.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataTrichologyImages extends JpaRepository<TrichologyJpaEntities.AssessmentImageEntity, UUID> {
    Optional<TrichologyJpaEntities.AssessmentImageEntity> findByIdAndCompanyIdAndAssessmentId(UUID id, UUID companyId,
                                                                                            UUID assessmentId);

    Optional<TrichologyJpaEntities.AssessmentImageEntity> findByIdAndCompanyId(UUID id, UUID companyId);

    List<TrichologyJpaEntities.AssessmentImageEntity> findByCompanyIdAndAssessmentIdOrderByCreatedAt(UUID companyId,
                                                                                                    UUID assessmentId);

    boolean existsByCompanyIdAndImageId(UUID companyId, UUID imageId);

    @Query("""
            select count(i) > 0 from TrichologyAssessmentImageEntity i, TrichologyAssessmentEntity a
             where i.assessmentId = a.id and i.companyId = :companyId and i.imageId = :imageId
               and a.status = 'PUBLISHED'
            """)
    boolean linkedToPublished(UUID companyId, UUID imageId);
}
