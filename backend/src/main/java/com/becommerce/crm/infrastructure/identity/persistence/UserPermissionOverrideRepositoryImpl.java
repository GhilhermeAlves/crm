package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.application.identity.port.output.UserPermissionOverrideRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class UserPermissionOverrideRepositoryImpl implements UserPermissionOverrideRepository {

    private final SpringDataUserPermissionRepository repository;

    public UserPermissionOverrideRepositoryImpl(SpringDataUserPermissionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entry> findByUser(UUID userId, UUID companyId) {
        return repository.findByUserIdAndCompanyId(userId, companyId).stream()
                .map(entity -> new Entry(
                        entity.getPermissionId(),
                        permissionName(entity.getPermissionId()),
                        entity.getEffect()))
                .toList();
    }

    @Override
    @Transactional
    public void upsert(UUID userId, UUID companyId, UUID permissionId, String effect) {
        var entity = repository.findByUserIdAndPermissionId(userId, permissionId)
                .orElseGet(() -> {
                    var created = new UserPermissionJpaEntity();
                    created.setUserId(userId);
                    created.setCompanyId(companyId);
                    created.setPermissionId(permissionId);
                    created.setCreatedAt(LocalDateTime.now());
                    return created;
                });
        entity.setEffect(effect);
        entity.setUpdatedAt(LocalDateTime.now());
        repository.save(entity);
    }

    @Override
    @Transactional
    public void deleteByUserIdAndPermissionId(UUID userId, UUID permissionId) {
        repository.findByUserIdAndPermissionId(userId, permissionId)
                .ifPresent(repository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserAndPermissionAndEffect(UUID userId, UUID permissionId, String effect) {
        return repository.findByUserIdAndPermissionId(userId, permissionId)
                .map(entity -> effect.equals(entity.getEffect()))
                .orElse(false);
    }

    private String permissionName(UUID permissionId) {
        // nome resolvido via join nativo para evitar N+1 de findById por linha
        return repository.findPermissionNameById(permissionId).orElse("?");
    }
}
