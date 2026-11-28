package com.becommerce.crm.clinical.trichology.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataTrichologyAssessments extends JpaRepository<TrichologyJpaEntities.AssessmentEntity, UUID> {
    Optional<TrichologyJpaEntities.AssessmentEntity> findByIdAndCompanyIdAndContactId(UUID id, UUID companyId,
                                                                                    UUID contactId);

    List<TrichologyJpaEntities.AssessmentEntity> findByCompanyIdAndContactIdOrderByCreatedAtDesc(UUID companyId,
                                                                                                UUID contactId);

    Optional<TrichologyJpaEntities.AssessmentEntity> findByQuizFormTokenHash(String tokenHash);

    @Query("""
            select a from TrichologyAssessmentEntity a
             where a.companyId = :companyId and a.contactId = :contactId and a.status = 'PUBLISHED'
               and a.publishedAt < :before and a.id <> :excludeId
             order by a.publishedAt desc
            """)
    List<TrichologyJpaEntities.AssessmentEntity> findPublishedBefore(UUID companyId, UUID contactId, Instant before,
                                                                    UUID excludeId);

    boolean existsByVerificationCode(String code);

    Optional<TrichologyJpaEntities.AssessmentEntity> findByCompanyIdAndPatientDocumentId(UUID companyId,
                                                                                       UUID documentId);
}
