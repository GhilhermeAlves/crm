package com.becommerce.crm.identity.domain.valueobject;

public enum RoleName {
    CRM_ADMIN,
    ADMIN,
    MANAGER,
    AGENT,
    VIEWER;

    public String getDisplayName() {
        return name().replace("_", " ");
    }
}
