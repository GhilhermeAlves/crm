package com.becommerce.crm.infrastructure.ai.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiActionJpaRepository extends JpaRepository<AiActionJpaEntity, UUID> {

    List<AiActionJpaEntity> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    /**
     * Adquire lock pessimista de escrita para tornar a transicao de estado
     * PROPOSED -> terminal atomica sob confirmacoes concorrentes (AI-05).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AiActionJpaEntity a where a.id = :id")
    Optional<AiActionJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}