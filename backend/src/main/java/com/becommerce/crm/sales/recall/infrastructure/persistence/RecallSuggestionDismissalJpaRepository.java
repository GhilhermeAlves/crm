package com.becommerce.crm.sales.recall.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecallSuggestionDismissalJpaRepository
        extends JpaRepository<RecallSuggestionDismissalJpaEntity, UUID> {

    boolean existsByCompanyIdAndContactId(UUID companyId, UUID contactId);
}
