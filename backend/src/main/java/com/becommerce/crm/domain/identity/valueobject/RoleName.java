package com.becommerce.crm.domain.identity.valueobject;

public enum RoleName {
    SUPER_ADMIN,
    ADMIN,
    MANAGER,
    AGENT,
    VIEWER;

    public String getDisplayName() {
        return name().replace("_", " ");
    }
}
