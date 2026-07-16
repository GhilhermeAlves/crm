package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.valueobject.Password;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserJpaEntity toJpaEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail().value());
        entity.setPasswordHash(user.getPassword().value());
        entity.setName(user.getName());
        entity.setCompanyId(user.getCompanyId());
        entity.setActive(user.isActive());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        entity.setDeletedAt(user.getDeletedAt());
        return entity;
    }

    public User toDomainEntity(UserJpaEntity entity) {
        Email email = new Email(entity.getEmail());
        Password password = Password.fromHash(entity.getPasswordHash());

        User user = User.create(email, password, entity.getName(), entity.getCompanyId());
        user.setId(entity.getId());
        user.setActive(entity.isActive());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());
        user.setDeletedAt(entity.getDeletedAt());
        return user;
    }
}
