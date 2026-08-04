package com.becommerce.auth.presentation.rest;

import com.becommerce.auth.infrastructure.health.DependencyProbe;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    private final DependencyProbe probe = mock(DependencyProbe.class);
    private final HealthController controller = new HealthController(probe);

    @Test
    void livenessShouldReturnUpWithoutDependencies() {
        Map<String, String> body = controller.liveness();

        assertEquals("UP", body.get("status"));
        verify(probe, never()).redisReachable();
        verify(probe, never()).keycloakReachable();
    }

    @Test
    void readinessShouldBeUpWhenDependenciesReachable() {
        when(probe.redisReachable()).thenReturn(true);
        when(probe.keycloakReachable()).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.readiness();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("UP", ((Map<?, ?>) response.getBody().get("checks")).get("redis"));
        assertEquals("UP", ((Map<?, ?>) response.getBody().get("checks")).get("keycloak"));
    }

    @Test
    void readinessShouldFailWhenRedisDown() {
        when(probe.redisReachable()).thenReturn(false);
        when(probe.keycloakReachable()).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = controller.readiness();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("DOWN", response.getBody().get("status"));
        assertEquals("DOWN", ((Map<?, ?>) response.getBody().get("checks")).get("redis"));
        assertEquals("UP", ((Map<?, ?>) response.getBody().get("checks")).get("keycloak"));
    }

    @Test
    void readinessShouldFailWhenKeycloakDown() {
        when(probe.redisReachable()).thenReturn(true);
        when(probe.keycloakReachable()).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.readiness();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("DOWN", response.getBody().get("status"));
        assertEquals("DOWN", ((Map<?, ?>) response.getBody().get("checks")).get("keycloak"));
    }

    @Test
    void readinessShouldRecoverAfterDependencyReturns() {
        when(probe.redisReachable()).thenReturn(false, true);
        when(probe.keycloakReachable()).thenReturn(false, true);

        ResponseEntity<Map<String, Object>> down = controller.readiness();
        ResponseEntity<Map<String, Object>> up = controller.readiness();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, down.getStatusCode());
        assertEquals("DOWN", down.getBody().get("status"));
        assertEquals(HttpStatus.OK, up.getStatusCode());
        assertEquals("UP", up.getBody().get("status"));
    }

    @Test
    void readinessShouldNotExposeAdminDetailsOrSecrets() {
        when(probe.redisReachable()).thenReturn(false);
        when(probe.keycloakReachable()).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.readiness();
        String body = response.getBody().toString();

        assertTrue(body.contains("DOWN"));
        assertFalse(body.contains("redis://"), "não expor URL de conexão");
        assertFalse(body.contains("6379"), "não expor porta");
        assertFalse(body.contains("password"), "não expor segredo");
        assertFalse(body.contains("secret"), "não expor client_secret");
        assertFalse(body.contains("Exception"), "não expor stack trace");
        assertFalse(body.contains("Connection refused"), "não expor detalhe da exceção");
    }
}
