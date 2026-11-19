package com.becommerce.crm.shared.security.authorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionEvaluatorBeanTest {

    private final CurrentUserAuthorities auth = mock(CurrentUserAuthorities.class);
    private final PermissionEvaluatorBean perm = new PermissionEvaluatorBean(auth);

    @Test
    void donoPassa() {
        when(auth.has("catalog:read")).thenReturn(true);
        assertThat(perm.hasOrDependent("catalog:read")).isTrue();
    }

    @Test
    void dependentePassa() {
        when(auth.has("contact:read")).thenReturn(true);
        assertThat(perm.hasOrDependent("catalog:read")).isTrue();
    }

    @Test
    void adminPassa() {
        when(auth.hasRole("CRM_ADMIN")).thenReturn(true);
        assertThat(perm.hasOrDependent("catalog:read")).isTrue();
    }

    @Test
    void superAdminPassa() {
        when(auth.hasRole("SUPER_ADMIN")).thenReturn(true);
        assertThat(perm.hasOrDependent("catalog:read")).isTrue();
    }

    @Test
    void semNadaNega() {
        assertThat(perm.hasOrDependent("catalog:read")).isFalse();
    }

    @Test
    void donoVazioNega() {
        assertThat(perm.hasOrDependent(" ")).isFalse();
    }

    @Test
    void hasNaoUsaDependencia() {
        when(auth.has("contact:read")).thenReturn(true);
        assertThat(perm.has("catalog:read")).isFalse();
    }

    @Test
    void hasAdminPassa() {
        when(auth.hasRole("CRM_ADMIN")).thenReturn(true);
        assertThat(perm.has("catalog:read")).isTrue();
    }
}
