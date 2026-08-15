package com.becommerce.crm.infrastructure.workflow.persistence;

import com.becommerce.crm.domain.workflow.WorkflowRun;
import com.becommerce.crm.domain.workflow.WorkflowRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface WorkflowRunJpaRepository extends JpaRepository<WorkflowRunJpaEntity, UUID> {

    @Query("""
            SELECT r FROM WorkflowRunJpaEntity r
            WHERE r.companyId = :companyId AND r.workflowId = :workflowId
              AND (:status IS NULL OR r.status = :status)
              AND (:eventType IS NULL OR r.eventType = :eventType)
              AND (:from IS NULL OR r.createdAt >= :from)
              AND (:to IS NULL OR r.createdAt <= :to)
            ORDER BY r.createdAt DESC
            """)
    Page<WorkflowRunJpaEntity> findRuns(@Param("companyId") UUID companyId,
                                        @Param("workflowId") UUID workflowId,
                                        @Param("status") String status,
                                        @Param("eventType") String eventType,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to,
                                        Pageable pageable);

    @Query("""
            SELECT r FROM WorkflowRunJpaEntity r
            WHERE r.companyId = :companyId
              AND (:status IS NULL OR r.status = :status)
              AND (:eventType IS NULL OR r.eventType = :eventType)
              AND (:from IS NULL OR r.createdAt >= :from)
              AND (:to IS NULL OR r.createdAt <= :to)
            ORDER BY r.createdAt DESC
            """)
    Page<WorkflowRunJpaEntity> findCompanyRuns(@Param("companyId") UUID companyId,
                                               @Param("status") String status,
                                               @Param("eventType") String eventType,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to,
                                               Pageable pageable);

    @Query("SELECT r FROM WorkflowRunJpaEntity r WHERE r.companyId = :companyId AND r.workflowId = :workflowId")
    List<WorkflowRunJpaEntity> findAllByCompanyAndWorkflow(@Param("companyId") UUID companyId,
                                                           @Param("workflowId") UUID workflowId);
}
