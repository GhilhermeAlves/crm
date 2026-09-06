package com.becommerce.crm.infrastructure.followup.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso JPA a follow-ups (Sprint 22). Todas as modificações são atômicas e
 * filtradas por {@code company_id} (defesa em profundidade + RLS FORCE da
 * conexão):
 * <ul>
 *   <li>{@link #claim}: guard de status — exatamente UMA instância do worker
 *       vence por follow-up (idempotência/concorrência); PROCESSING órfão
 *       (crash) é recuperado via cutoff.</li>
 *   <li>{@link #markSent}/{@link #scheduleRetry}/{@link #markFailedTerminal}:
 *       transições apenas a partir de PROCESSING (quem ganhou o claim).</li>
 *   <li>{@link #cancelPending}/{@link #cancelProcessingByRule}: cancelamentos
 *       do usuário (PENDING) e por regra do processador (PENDING/PROCESSING).</li>
 * </ul>
 */
public interface FollowUpJpaRepository extends JpaRepository<FollowUpJpaEntity, UUID> {

    @Query("""
            SELECT e FROM FollowUpJpaEntity e
            WHERE e.companyId = :companyId
              AND (:conversationId IS NULL OR e.conversationId = :conversationId)
            ORDER BY e.executeAt ASC, e.createdAt DESC
            """)
    Page<FollowUpJpaEntity> findByCompany(@Param("companyId") UUID companyId,
                                          @Param("conversationId") UUID conversationId,
                                          Pageable pageable);

    Optional<FollowUpJpaEntity> findByCompanyIdAndIdempotencyKey(UUID companyId, UUID idempotencyKey);

    /** Claim atômico: vence apenas quem transaciona PENDING (ou recupera PROCESSING órfão). */
    @Modifying
    @Query(value = """
            UPDATE followups
            SET status = 'PROCESSING', processing_started_at = :now, updated_at = :now
            WHERE id = :id AND company_id = :companyId
              AND execute_at <= :now
              AND (status = 'PENDING'
                   OR (status = 'PROCESSING' AND processing_started_at < :staleProcessingCutoff))
            """, nativeQuery = true)
    int claim(@Param("companyId") UUID companyId, @Param("id") UUID id,
              @Param("now") LocalDateTime now, @Param("staleProcessingCutoff") LocalDateTime staleProcessingCutoff);

    @Modifying
    @Query(value = """
            UPDATE followups
            SET status = 'SENT', result_text = :resultText, processed_at = :now, updated_at = :now
            WHERE id = :id AND company_id = :companyId AND status = 'PROCESSING'
            """, nativeQuery = true)
    int markSent(@Param("companyId") UUID companyId, @Param("id") UUID id,
                 @Param("resultText") String resultText, @Param("now") LocalDateTime now);

    @Modifying
    @Query(value = """
            UPDATE followups
            SET status = 'PENDING', attempts = attempts + 1, execute_at = :nextExecuteAt,
                last_error = :error, processing_started_at = NULL, updated_at = :now
            WHERE id = :id AND company_id = :companyId AND status = 'PROCESSING'
            """, nativeQuery = true)
    int scheduleRetry(@Param("companyId") UUID companyId, @Param("id") UUID id,
                      @Param("nextExecuteAt") LocalDateTime nextExecuteAt,
                      @Param("error") String error, @Param("now") LocalDateTime now);

    @Modifying
    @Query(value = """
            UPDATE followups
            SET status = 'FAILED', last_error = :error, processed_at = :now, updated_at = :now
            WHERE id = :id AND company_id = :companyId AND status = 'PROCESSING'
            """, nativeQuery = true)
    int markFailedTerminal(@Param("companyId") UUID companyId, @Param("id") UUID id,
                           @Param("error") String error, @Param("now") LocalDateTime now);

    @Modifying
    @Query(value = """
            UPDATE followups
            SET status = 'CANCELLED', cancelled_reason = :reason, cancelled_at = :now, updated_at = :now
            WHERE id = :id AND company_id = :companyId AND status = 'PENDING'
            """, nativeQuery = true)
    int cancelPending(@Param("companyId") UUID companyId, @Param("id") UUID id,
                      @Param("reason") String reason, @Param("now") LocalDateTime now);

    /** Cancelamento por regra do processador (modo HUMAN / obsoleto por nova mensagem). */
    @Modifying
    @Query(value = """
            UPDATE followups
            SET status = 'CANCELLED', cancelled_reason = :reason, cancelled_at = :now, updated_at = :now
            WHERE id = :id AND company_id = :companyId
              AND status IN ('PENDING', 'PROCESSING')
            """, nativeQuery = true)
    int cancelProcessingByRule(@Param("companyId") UUID companyId, @Param("id") UUID id,
                               @Param("reason") String reason, @Param("now") LocalDateTime now);
}