package com.becommerce.crm.sales.recall.infrastructure.persistence;

import com.becommerce.crm.masterdata.contact.infrastructure.persistence.entity.ContactJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RecallJpaRepository extends JpaRepository<RecallJpaEntity, UUID> {

    @Query("SELECT r FROM RecallJpaEntity r LEFT JOIN ContactJpaEntity c ON c.id = r.contactId " +
            "WHERE r.companyId = :companyId " +
            "AND r.status <> 'DISMISSED' " +
            "AND (:hostId IS NULL OR r.hostId = :hostId) " +
            "AND (:tag IS NULL OR LOWER(r.tag) LIKE :tag) " +
            "AND (:tagId IS NULL OR r.tagId = :tagId) " +
            "AND (:like IS NULL OR LOWER(CONCAT(COALESCE(c.firstName, ''), ' ', COALESCE(c.lastName, ''))) LIKE :like) " +
            "AND (:dueFrom IS NULL OR r.dueDate >= :dueFrom) " +
            "AND (:dueTo IS NULL OR r.dueDate < :dueTo)")
    Page<RecallJpaEntity> findByCompany(@Param("companyId") UUID companyId,
                                        @Param("hostId") UUID hostId,
                                        @Param("tag") String tag,
                                        @Param("tagId") UUID tagId,
                                        @Param("like") String like,
                                        @Param("dueFrom") LocalDate dueFrom,
                                        @Param("dueTo") LocalDate dueTo,
                                        Pageable pageable);

    @Query(nativeQuery = true, value = """
            WITH latest_completed AS (
                SELECT DISTINCT ON (a.company_id, a.contact_id)
                       a.company_id,
                       a.contact_id,
                       a.host_id,
                       a.start_at AS "lastAppointmentAt"
                FROM appointments a
                JOIN contacts c ON c.id = a.contact_id
                WHERE a.company_id = :companyId
                  AND a.status = 'COMPLETED'
                  AND a.contact_id IS NOT NULL
                  AND c.deleted_at IS NULL
                ORDER BY a.company_id, a.contact_id, a.start_at DESC
            )
            SELECT
                c.id AS "contactId",
                c.first_name AS "firstName",
                c.last_name AS "lastName",
                c.mobile AS "mobile",
                c.phone AS "phone",
                lc.host_id AS "hostId",
                lc."lastAppointmentAt",
                COALESCE(types.recentProcedures, '') AS "recentProcedures",
                EXISTS (
                    SELECT 1 FROM appointments f
                    WHERE f.company_id = lc.company_id
                      AND f.contact_id = lc.contact_id
                      AND f.status <> 'CANCELED'
                      AND f.start_at >= :now
                ) AS "hasFutureAppointment",
                EXISTS (
                    SELECT 1 FROM recalls rl
                    WHERE rl.company_id = lc.company_id
                      AND rl.contact_id = lc.contact_id
                      AND rl.status = 'PENDING'
                ) AS "hasPendingRecall",
                EXISTS (
                    SELECT 1 FROM recall_suggestion_dismissals d
                    WHERE d.company_id = lc.company_id
                      AND d.contact_id = lc.contact_id
                ) AS "dismissed"
            FROM latest_completed lc
            JOIN contacts c ON c.id = lc.contact_id
            LEFT JOIN LATERAL (
                SELECT string_agg(t.name, E'\\n' ORDER BY ra."startAt" DESC) AS "recentProcedures"
                FROM (
                    SELECT ra2.appointment_type_id, ra2.start_at AS "startAt"
                    FROM appointments ra2
                    WHERE ra2.company_id = lc.company_id
                      AND ra2.contact_id = lc.contact_id
                      AND ra2.status = 'COMPLETED'
                      AND ra2.appointment_type_id IS NOT NULL
                    ORDER BY ra2.start_at DESC
                    LIMIT 3
                ) ra
                JOIN appointment_types t ON t.id = ra.appointment_type_id
            ) types ON TRUE
            WHERE lc."lastAppointmentAt" < :cutoff
            ORDER BY lc."lastAppointmentAt" ASC
            """)
    List<RecallSuggestionRow> findSuggestionCandidates(@Param("companyId") UUID companyId,
                                                       @Param("cutoff") Instant cutoff,
                                                       @Param("now") Instant now);
}
