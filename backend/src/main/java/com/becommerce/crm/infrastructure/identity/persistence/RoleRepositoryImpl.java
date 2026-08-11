package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final SpringDataRoleRepository repository;

    public RoleRepositoryImpl(SpringDataRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public Role save(Role role) {
        RoleJpaEntity entity = toJpaEntity(role);
        RoleJpaEntity saved = repository.save(entity);
        return toDomainEntity(saved);
    }

    @Override
    public Optional<Role> findById(UUID id) {
        return repository.findById(id).map(this::toDomainEntity);
    }

    @Override
    public Optional<Role> findByNameAndCompanyId(String name, UUID companyId) {
        return repository.findByNameAndCompanyId(name, companyId).map(this::toDomainEntity);
    }

    @Override
    public List<Role> findAllByCompanyId(UUID companyId) {
        return repository.findByCompanyId(companyId).stream().map(this::toDomainEntity).toList();
    }

    @Override
    public boolean existsByNameAndCompanyId(String name, UUID companyId) {
        return repository.existsByNameAndCompanyId(name, companyId);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private RoleJpaEntity toJpaEntity(Role role) {
        RoleJpaEntity entity = new RoleJpaEntity();
        entity.setId(role.getId());
        entity.setName(role.getName());
        entity.setDescription(role.getDescription());
        entity.setCompanyId(role.getCompanyId());
        entity.setSystem(role.isSystem());
        entity.setActive(role.isActive());
        entity.setCreatedAt(role.getCreatedAt());
        entity.setUpdatedAt(role.getUpdatedAt() != null ? role.getUpdatedAt() : role.getCreatedAt());
        return entity;
    }

    private Role toDomainEntity(RoleJpaEntity entity) {
        Role role = Role.create(entity.getName(), entity.getCompanyId());
        role.setId(entity.getId());
        role.setDescription(entity.getDescription());
        role.setSystem(entity.isSystem());
        role.setActive(entity.isActive());
        role.setCreatedAt(entity.getCreatedAt());
        role.setUpdatedAt(entity.getUpdatedAt());
        return role;
    }
}
