package com.becommerce.crm.infrastructure.followup.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Acesso JPA a {@code followup_sequences} (Sprint 22). Leituras e exclusão
 * sempre filtradas por {@code company_id} (defesa em profundidade + RLS FORCE).
 */
public interface FollowUpSequenceJpaRepository extends JpaRepository<FollowUpSequenceJpaEntity, UUID> {

    @Query("""
            SELECT e FROM FollowUpSequenceJpaEntity e
            WHERE e.companyId = :companyId
            ORDER BY e.createdAt DESC, e.id ASC
            """)
    Page<FollowUpSequenceJpaEntity> findByCompany(@Param("companyId") UUID companyId, Pageable pageable);

    @Modifying
    @Query(value = """
            DELETE FROM followup_sequences
            WHERE id = :id AND company_id = :companyId
            """, nativeQuery = true)
    int deleteByIdAndCompany(@Param("id") UUID id, @Param("companyId") UUID companyId);
}