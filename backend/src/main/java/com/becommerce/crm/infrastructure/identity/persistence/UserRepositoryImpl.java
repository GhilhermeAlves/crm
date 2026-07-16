package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final SpringDataUserRepository repository;
    private final UserMapper mapper;

    public UserRepositoryImpl(SpringDataUserRepository repository, UserMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = mapper.toJpaEntity(user);
        UserJpaEntity saved = repository.save(entity);
        return mapper.toDomainEntity(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomainEntity);
    }

    @Override
    public Optional<User> findByInviteToken(String token) {
        return repository.findByInviteToken(token).map(mapper::toDomainEntity);
    }

    @Override
    public List<User> findAllByCompanyId(UUID companyId) {
        return repository.findByCompanyId(companyId).stream()
                .map(mapper::toDomainEntity)
                .toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, UUID id) {
        return repository.existsByEmailAndIdNot(email, id);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public PageResult findByCompanyIdWithFilters(UUID companyId, String search, UserStatus status,
                                                  int page, int pageSize, String sortBy, String sortDirection) {
        Sort sort = Sort.by("desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy != null ? sortBy : "createdAt");
        PageRequest pageRequest = PageRequest.of(page, pageSize, sort);

        String statusStr = status != null ? status.name() : null;
        Page<UserJpaEntity> result = repository.findByCompanyIdWithFilters(companyId, search, statusStr, pageRequest);

        List<User> users = result.getContent().stream()
                .map(mapper::toDomainEntity)
                .toList();

        return new PageResult(users, result.getTotalElements());
    }
}
