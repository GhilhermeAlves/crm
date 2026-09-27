package com.becommerce.crm.infrastructure.ai.persistence;

import com.becommerce.crm.application.ai.port.output.ToolPermissionRepository;
import com.becommerce.crm.domain.ai.ToolPermission;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ToolPermissionRepositoryImpl implements ToolPermissionRepository {

    private final ToolPermissionJpaRepository jpaRepository;

    public ToolPermissionRepositoryImpl(ToolPermissionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<ToolPermission> findByToolId(UUID toolId) {
        return jpaRepository.findByToolIdOrderByRoleAsc(toolId).stream()
                .map(ToolPermissionRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public Optional<ToolPermission> findByToolAndRole(UUID toolId, String role) {
        return jpaRepository.findByToolIdAndRole(toolId, role).map(ToolPermissionRepositoryImpl::toDomain);
    }

    @Override
    public ToolPermission save(ToolPermission permission) {
        return toDomain(jpaRepository.save(toEntity(permission)));
    }

    @Override
    public void delete(UUID id) {
        jpaRepository.deleteById(id);
    }

    static ToolPermission toDomain(ToolPermissionJpaEntity e) {
        return ToolPermission.reconstitute(e.getId(), e.getToolId(), e.getRole(), e.isAllowed(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    static ToolPermissionJpaEntity toEntity(ToolPermission permission) {
        ToolPermissionJpaEntity e = new ToolPermissionJpaEntity();
        e.setId(permission.getId());
        e.setToolId(permission.getToolId());
        e.setRole(permission.getRole());
        e.setAllowed(permission.isAllowed());
        e.setCreatedAt(permission.getCreatedAt());
        e.setUpdatedAt(permission.getUpdatedAt());
        return e;
    }
}
