package com.becommerce.crm.infrastructure.ai.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ToolPermissionJpaRepository extends JpaRepository<ToolPermissionJpaEntity, UUID> {

    List<ToolPermissionJpaEntity> findByToolIdOrderByRoleAsc(UUID toolId);

    Optional<ToolPermissionJpaEntity> findByToolIdAndRole(UUID toolId, String role);
}
