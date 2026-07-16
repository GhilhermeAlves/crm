package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import org.springframework.stereotype.Repository;
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
        RoleJpaEntity entity = new RoleJpaEntity();
        entity.setName(role.getName().name());
        entity.setCompanyId(role.getCompanyId());
        entity.setCreatedAt(role.getCreatedAt());

        RoleJpaEntity saved = repository.save(entity);

        Role roleDomain = Role.create(RoleName.valueOf(saved.getName()), saved.getCompanyId());
        roleDomain.setId(saved.getId());
        roleDomain.setCreatedAt(saved.getCreatedAt());
        return roleDomain;
    }

    @Override
    public Optional<Role> findById(UUID id) {
        return repository.findById(id).map(entity -> {
            Role role = Role.create(RoleName.valueOf(entity.getName()), entity.getCompanyId());
            role.setId(entity.getId());
            role.setCreatedAt(entity.getCreatedAt());
            return role;
        });
    }

    @Override
    public Optional<Role> findByNameAndCompanyId(RoleName name, UUID companyId) {
        return repository.findByNameAndCompanyId(name.name(), companyId).map(entity -> {
            Role role = Role.create(RoleName.valueOf(entity.getName()), entity.getCompanyId());
            role.setId(entity.getId());
            role.setCreatedAt(entity.getCreatedAt());
            return role;
        });
    }
}
