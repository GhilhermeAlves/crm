package com.becommerce.crm.automation.ai.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentMemoryJpaRepository extends JpaRepository<AgentMemoryJpaEntity, UUID> {

    Optional<AgentMemoryJpaEntity> findByIdAndCompanyId(UUID id, UUID companyId);

    List<AgentMemoryJpaEntity> findByCompanyIdAndContactIdOrderByImportanceDescUpdatedAtDesc(
            UUID companyId, UUID contactId, Pageable pageable);

    void deleteByIdAndCompanyId(UUID id, UUID companyId);
}
