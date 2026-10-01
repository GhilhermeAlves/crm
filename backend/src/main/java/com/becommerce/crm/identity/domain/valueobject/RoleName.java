package com.becommerce.crm.identity.domain.valueobject;

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
