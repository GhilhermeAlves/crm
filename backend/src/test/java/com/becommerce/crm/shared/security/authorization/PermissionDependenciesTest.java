package com.becommerce.crm.shared.security.authorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionDependenciesTest {

    @Test
    void dependenteRecebeSoLeituraDoDono() {
        assertThat(PermissionDependencies.screensDependingOn("catalog:read"))
                .contains("appointment:read", "contact:read");
    }

    @Test
    void naoETransitivo() {
        PermissionDependencies.all().forEach((screen, owners) ->
                owners.forEach(owner -> assertThat(owner).endsWith(":read")));
    }

    @Test
    void donoSemDependentesDevolveVazio() {
        assertThat(PermissionDependencies.screensDependingOn("nada:read")).isEmpty();
    }
}
