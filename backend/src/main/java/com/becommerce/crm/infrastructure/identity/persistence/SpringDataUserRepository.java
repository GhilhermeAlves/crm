package com.becommerce.crm.infrastructure.identity.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<UserJpaEntity> findByEmail(String email);
    List<UserJpaEntity> findByCompanyId(UUID companyId);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
    Optional<UserJpaEntity> findByInviteToken(String token);
    Optional<UserJpaEntity> findByKeycloakSub(String keycloakSub);

    long countByCompanyIdAndDeletedAtIsNull(UUID companyId);

    long countByCompanyIdAndStatusAndDeletedAtIsNull(UUID companyId, String status);

    long countByCompanyIdAndCreatedAtAfterAndDeletedAtIsNull(UUID companyId, LocalDateTime since);

    @Query("SELECT u.department, COUNT(u) FROM UserJpaEntity u WHERE u.companyId = :companyId AND u.deletedAt IS NULL AND u.department IS NOT NULL GROUP BY u.department ORDER BY COUNT(u) DESC")
    List<Object[]> countByCompanyIdGroupByDepartment(@Param("companyId") UUID companyId);

    @Query("SELECT u FROM UserJpaEntity u WHERE u.companyId = :companyId AND u.deletedAt IS NULL " +
            "AND (:search IS NULL OR :search = '' OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR :status = '' OR u.status = :status)")
    Page<UserJpaEntity> findByCompanyIdWithFilters(
            @Param("companyId") UUID companyId,
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable);
}
