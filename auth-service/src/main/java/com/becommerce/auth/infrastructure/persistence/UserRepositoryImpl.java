package com.becommerce.auth.infrastructure.persistence;

import com.becommerce.auth.application.identity.port.output.UserRepository;
import com.becommerce.auth.domain.identity.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final SpringDataUserRepository springData;

    public UserRepositoryImpl(SpringDataUserRepository springData) {
        this.springData = springData;
    }

    @Override
    public Optional<User> findByKeycloakSub(String keycloakSub) {
        return springData.findByKeycloakSub(keycloakSub).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springData.findByEmail(email).map(this::toDomain);
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getName(),
                entity.getKeycloakSub(),
                entity.getCompanyId(),
                entity.isActive(),
                entity.isCrmEnabled());
    }
}
