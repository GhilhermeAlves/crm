package com.becommerce.crm.sales.recall.infrastructure.persistence;

import com.becommerce.crm.sales.recall.application.port.out.RecallSuggestionDismissalRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class RecallSuggestionDismissalRepositoryImpl implements RecallSuggestionDismissalRepository {

    private final RecallSuggestionDismissalJpaRepository jpaRepository;

    public RecallSuggestionDismissalRepositoryImpl(RecallSuggestionDismissalJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsByCompanyAndContact(UUID companyId, UUID contactId) {
        return jpaRepository.existsByCompanyIdAndContactId(companyId, contactId);
    }

    @Override
    public void save(UUID companyId, UUID contactId, UUID createdBy) {
        jpaRepository.save(RecallSuggestionDismissalJpaEntity.create(companyId, contactId, createdBy));
    }
}
