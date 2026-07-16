package com.becommerce.crm.infrastructure.audit.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

    @Query("SELECT a FROM AuditLogJpaEntity a WHERE a.companyId = :companyId " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:module IS NULL OR a.module = :module) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:userId IS NULL OR a.userId = :userId) " +
           "AND (:entityName IS NULL OR a.entityName = :entityName) " +
           "AND (:entityId IS NULL OR a.entityId = :entityId) " +
           "AND (:search IS NULL OR LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.userName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.userEmail) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.requestUri) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:startDate IS NULL OR a.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    List<AuditLogJpaEntity> search(
        @Param("companyId") UUID companyId,
        @Param("action") String action,
        @Param("module") String module,
        @Param("status") String status,
        @Param("userId") UUID userId,
        @Param("entityName") String entityName,
        @Param("entityId") String entityId,
        @Param("search") String search,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        PageRequest pageable
    );

    @Query("SELECT COUNT(a) FROM AuditLogJpaEntity a WHERE a.companyId = :companyId " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:module IS NULL OR a.module = :module) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:userId IS NULL OR a.userId = :userId) " +
           "AND (:entityName IS NULL OR a.entityName = :entityName) " +
           "AND (:entityId IS NULL OR a.entityId = :entityId) " +
           "AND (:search IS NULL OR LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.userName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.userEmail) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(a.requestUri) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:startDate IS NULL OR a.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR a.createdAt <= :endDate)")
    long countSearch(
        @Param("companyId") UUID companyId,
        @Param("action") String action,
        @Param("module") String module,
        @Param("status") String status,
        @Param("userId") UUID userId,
        @Param("entityName") String entityName,
        @Param("entityId") String entityId,
        @Param("search") String search,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
