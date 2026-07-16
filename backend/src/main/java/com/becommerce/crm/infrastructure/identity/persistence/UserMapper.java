package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.UserStatus;
import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.valueobject.Password;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserJpaEntity toJpaEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail().value());
        if (user.getPassword() != null) {
            entity.setPasswordHash(user.getPassword().value());
        }
        entity.setName(user.getFullName());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setPhone(user.getPhone());
        entity.setDepartment(user.getDepartment());
        entity.setJobTitle(user.getJobTitle());
        entity.setAvatarUrl(user.getAvatarUrl());
        entity.setCompanyId(user.getCompanyId());
        entity.setStatus(user.getStatus().name());
        entity.setActive(user.isActive());
        entity.setLanguage(user.getLanguage());
        entity.setTimezone(user.getTimezone());
        entity.setNotes(user.getNotes());
        entity.setLastLoginAt(user.getLastLoginAt());
        entity.setInviteToken(user.getInviteToken());
        entity.setInvitedAt(user.getInvitedAt());
        entity.setInvitedBy(user.getInvitedBy());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        entity.setDeletedAt(user.getDeletedAt());
        return entity;
    }

    public User toDomainEntity(UserJpaEntity entity) {
        Email email = new Email(entity.getEmail());
        Password password = entity.getPasswordHash() != null ? Password.fromHash(entity.getPasswordHash()) : null;

        User user = User.create(email, password, entity.getFirstName(),
                entity.getLastName() != null ? entity.getLastName() : "",
                entity.getCompanyId());
        user.setId(entity.getId());
        user.setPhone(entity.getPhone());
        user.setDepartment(entity.getDepartment());
        user.setJobTitle(entity.getJobTitle());
        user.setAvatarUrl(entity.getAvatarUrl());
        user.setStatus(UserStatus.valueOf(entity.getStatus()));
        user.setActive(entity.isActive());
        user.setLanguage(entity.getLanguage());
        user.setTimezone(entity.getTimezone());
        user.setNotes(entity.getNotes());
        user.setLastLoginAt(entity.getLastLoginAt());
        user.setInviteToken(entity.getInviteToken());
        user.setInvitedAt(entity.getInvitedAt());
        user.setInvitedBy(entity.getInvitedBy());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());
        user.setDeletedAt(entity.getDeletedAt());
        return user;
    }
}
