package com.becommerce.crm.infrastructure.identity.persistence;

import com.becommerce.crm.domain.identity.Permission;
import org.springframework.stereotype.Component;

@Component
public class PermissionMapper {

    public PermissionJpaEntity toJpaEntity(Permission permission) {
        PermissionJpaEntity entity = new PermissionJpaEntity();
        entity.setId(permission.getId());
        entity.setName(permission.getName());
        entity.setDescription(permission.getDescription());
        entity.setModule(permission.getModule());
        entity.setResource(permission.getResource());
        entity.setAction(permission.getAction());
        entity.setCreatedAt(permission.getCreatedAt());
        return entity;
    }

    public Permission toDomainEntity(PermissionJpaEntity entity) {
        Permission permission = Permission.create(
            entity.getName(),
            entity.getDescription(),
            entity.getModule(),
            entity.getResource(),
            entity.getAction()
        );
        permission.setId(entity.getId());
        permission.setCreatedAt(entity.getCreatedAt());
        return permission;
    }
}
