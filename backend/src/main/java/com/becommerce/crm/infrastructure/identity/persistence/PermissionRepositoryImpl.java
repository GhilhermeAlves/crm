package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.application.identity.port.output.PermissionRepository;
import com.becommerce.crm.domain.identity.Permission;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PermissionRepositoryImpl implements PermissionRepository {

    private final SpringDataPermissionRepository repository;
    private final PermissionMapper mapper;

    public PermissionRepositoryImpl(SpringDataPermissionRepository repository, PermissionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Permission save(Permission permission) {
        PermissionJpaEntity entity = mapper.toJpaEntity(permission);
        PermissionJpaEntity saved = repository.save(entity);
        return mapper.toDomainEntity(saved);
    }

    @Override
    public Optional<Permission> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public Optional<Permission> findByName(String name) {
        return repository.findByName(name).map(mapper::toDomainEntity);
    }

    @Override
    public List<Permission> findAll() {
        return repository.findAll().stream().map(mapper::toDomainEntity).toList();
    }

    @Override
    public List<Permission> findByModule(String module) {
        return repository.findByModule(module).stream().map(mapper::toDomainEntity).toList();
    }

    @Override
    public List<Permission> findByRoleId(UUID roleId) {
        return repository.findByRoleId(roleId).stream().map(mapper::toDomainEntity).toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByName(name);
    }

    @Override
    public List<String> findEffectivePermissionNamesByUserIdAndCompanyId(UUID userId, UUID companyId) {
        return repository.findEffectivePermissionNamesByUserIdAndCompanyId(userId, companyId);
    }
}
