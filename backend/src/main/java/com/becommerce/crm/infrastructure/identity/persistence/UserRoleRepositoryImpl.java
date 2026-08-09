package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.domain.identity.UserRole;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRoleRepositoryImpl implements UserRoleRepository {

    private final SpringDataUserRoleRepository repository;

    public UserRoleRepositoryImpl(SpringDataUserRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserRole save(UserRole userRole) {
        UserRoleJpaEntity entity = new UserRoleJpaEntity();
        entity.setUserId(userRole.getUserId());
        entity.setRoleId(userRole.getRoleId());
        entity.setCompanyId(userRole.getCompanyId());
        entity.setCreatedAt(userRole.getCreatedAt());

        UserRoleJpaEntity saved = repository.save(entity);

        UserRole ur = UserRole.assign(saved.getUserId(), saved.getRoleId(), saved.getCompanyId());
        ur.setCreatedAt(saved.getCreatedAt());
        return ur;
    }

    @Override
    public List<UserRole> findByUserId(UUID userId) {
        return repository.findByUserId(userId).stream().map(entity -> {
            UserRole ur = UserRole.assign(entity.getUserId(), entity.getRoleId(), entity.getCompanyId());
            ur.setCreatedAt(entity.getCreatedAt());
            return ur;
        }).toList();
    }

    @Override
    public List<UserRole> findByCompanyId(UUID companyId) {
        return repository.findByCompanyId(companyId).stream().map(entity -> {
            UserRole ur = UserRole.assign(entity.getUserId(), entity.getRoleId(), entity.getCompanyId());
            ur.setCreatedAt(entity.getCreatedAt());
            return ur;
        }).toList();
    }

    @Override
    public List<UserRole> findByUserIdAndCompanyId(UUID userId, UUID companyId) {
        return repository.findByUserIdAndCompanyId(userId, companyId).stream().map(entity -> {
            UserRole ur = UserRole.assign(entity.getUserId(), entity.getRoleId(), entity.getCompanyId());
            ur.setCreatedAt(entity.getCreatedAt());
            return ur;
        }).toList();
    }

    @Override
    public Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId) {
        return repository.findByUserIdAndRoleId(userId, roleId).map(entity -> {
            UserRole ur = UserRole.assign(entity.getUserId(), entity.getRoleId(), entity.getCompanyId());
            ur.setCreatedAt(entity.getCreatedAt());
            return ur;
        });
    }

    @Override
    public void deleteByUserIdAndRoleId(UUID userId, UUID roleId) {
        repository.deleteByUserIdAndRoleId(userId, roleId);
    }

    @Override
    public void deleteByUserIdAndCompanyId(UUID userId, UUID companyId) {
        repository.deleteByUserIdAndCompanyId(userId, companyId);
    }

    @Override
    public boolean existsByUserIdAndRoleId(UUID userId, UUID roleId) {
        return repository.existsByUserIdAndRoleId(userId, roleId);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        repository.deleteByUserId(userId);
    }
}
