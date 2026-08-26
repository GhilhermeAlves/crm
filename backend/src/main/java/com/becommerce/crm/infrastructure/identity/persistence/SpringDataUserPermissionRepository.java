package com.becommerce.crm.infrastructure.identity.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUserPermissionRepository extends JpaRepository<UserPermissionJpaEntity, UUID> {

    Optional<UserPermissionJpaEntity> findByUserIdAndPermissionId(UUID userId, UUID permissionId);

    List<UserPermissionJpaEntity> findByUserIdAndCompanyId(UUID userId, UUID companyId);

    @Query(value = "SELECT name FROM permissions WHERE id = :id", nativeQuery = true)
    Optional<String> findPermissionNameById(@Param("id") UUID id);
}
