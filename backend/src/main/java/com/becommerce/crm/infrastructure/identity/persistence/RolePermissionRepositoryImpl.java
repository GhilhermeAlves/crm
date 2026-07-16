package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.application.identity.port.output.RolePermissionRepository;
import com.becommerce.crm.domain.identity.RolePermission;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RolePermissionRepositoryImpl implements RolePermissionRepository {

    private final SpringDataRolePermissionRepository repository;

    public RolePermissionRepositoryImpl(SpringDataRolePermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    public RolePermission save(RolePermission rolePermission) {
        RolePermissionJpaEntity entity = new RolePermissionJpaEntity();
        entity.setId(rolePermission.getId());
        entity.setRoleId(rolePermission.getRoleId());
        entity.setPermissionId(rolePermission.getPermissionId());
        entity.setCreatedAt(rolePermission.getCreatedAt());

        RolePermissionJpaEntity saved = repository.save(entity);

        RolePermission rp = RolePermission.create(saved.getRoleId(), saved.getPermissionId());
        rp.setId(saved.getId());
        rp.setCreatedAt(saved.getCreatedAt());
        return rp;
    }

    @Override
    public Optional<RolePermission> findById(UUID id) {
        return repository.findById(id).map(entity -> {
            RolePermission rp = RolePermission.create(entity.getRoleId(), entity.getPermissionId());
            rp.setId(entity.getId());
            rp.setCreatedAt(entity.getCreatedAt());
            return rp;
        });
    }

    @Override
    public List<RolePermission> findByRoleId(UUID roleId) {
        return repository.findByRoleId(roleId).stream().map(entity -> {
            RolePermission rp = RolePermission.create(entity.getRoleId(), entity.getPermissionId());
            rp.setId(entity.getId());
            rp.setCreatedAt(entity.getCreatedAt());
            return rp;
        }).toList();
    }

    @Override
    public List<RolePermission> findByPermissionId(UUID permissionId) {
        return repository.findByPermissionId(permissionId).stream().map(entity -> {
            RolePermission rp = RolePermission.create(entity.getRoleId(), entity.getPermissionId());
            rp.setId(entity.getId());
            rp.setCreatedAt(entity.getCreatedAt());
            return rp;
        }).toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId) {
        repository.deleteByRoleIdAndPermissionId(roleId, permissionId);
    }

    @Override
    public boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId) {
        return repository.existsByRoleIdAndPermissionId(roleId, permissionId);
    }
}
