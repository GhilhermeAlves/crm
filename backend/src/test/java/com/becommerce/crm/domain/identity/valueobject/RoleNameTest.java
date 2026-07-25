package com.becommerce.crm.domain.identity.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RoleNameTest {

    @Test
    void shouldExposeDisplayNamesForAllRoles() {
        assertEquals("SUPER ADMIN", RoleName.SUPER_ADMIN.getDisplayName());
        assertEquals("ADMIN", RoleName.ADMIN.getDisplayName());
        assertEquals("MANAGER", RoleName.MANAGER.getDisplayName());
        assertEquals("AGENT", RoleName.AGENT.getDisplayName());
        assertEquals("VIEWER", RoleName.VIEWER.getDisplayName());
    }
}
