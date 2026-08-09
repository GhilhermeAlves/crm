package com.becommerce.auth.infrastructure.config;

import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;
import com.becommerce.auth.infrastructure.gateway.GatewayCookieFactory;
import com.becommerce.auth.infrastructure.security.GatewayCsrfFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void shouldRegisterCsrfFilterForBothRefreshAndLink() {
        OidcGatewayProperties properties = new OidcGatewayProperties();
        GatewayConfig config = new GatewayConfig(properties, environment);
        GatewayCookieFactory cookieFactory = new GatewayCookieFactory(properties);

        FilterRegistrationBean<GatewayCsrfFilter> registration =
                config.gatewayCsrfFilter(cookieFactory, properties, new ObjectMapper());

        List<String> patterns = List.copyOf(registration.getUrlPatterns());
        assertEquals(2, patterns.size(), "apenas refresh e link devem ser registrados");
        assertTrue(patterns.contains("/auth/refresh"),
                "POST /auth/refresh deve ser protegido por CSRF");
        assertTrue(patterns.contains("/auth/link"),
                "POST /auth/link (Sprint 7.2) deve ser protegido por CSRF");
    }
}
