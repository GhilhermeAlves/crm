package com.becommerce.auth.infrastructure.config;

import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayConfigTest {

    @Mock private Environment environment;

    @Test
    void shouldRejectInsecureCookieInProdProfile() {
        OidcGatewayProperties properties = new OidcGatewayProperties();
        properties.setSecureCookie(false);
        when(environment.acceptsProfiles(Profiles.of("prod"))).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> new GatewayConfig(properties, environment));
    }

    @Test
    void shouldAllowInsecureCookieOutsideProdProfile() {
        OidcGatewayProperties properties = new OidcGatewayProperties();
        properties.setSecureCookie(false);
        when(environment.acceptsProfiles(Profiles.of("prod"))).thenReturn(false);

        new GatewayConfig(properties, environment);
    }

    @Test
    void shouldAllowSecureCookieInProdProfile() {
        OidcGatewayProperties properties = new OidcGatewayProperties();
        properties.setSecureCookie(true);
        when(environment.acceptsProfiles(Profiles.of("prod"))).thenReturn(true);

        new GatewayConfig(properties, environment);
    }
}
