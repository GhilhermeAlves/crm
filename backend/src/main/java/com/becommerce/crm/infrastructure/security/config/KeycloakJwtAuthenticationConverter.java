package com.becommerce.crm.infrastructure.security.config;

import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    private final CurrentUserResolver currentUserResolver;

    public KeycloakJwtAuthenticationConverter(CurrentUserResolver currentUserResolver) {
        this.currentUserResolver = currentUserResolver;
    }

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        List<String> allRoles = new ArrayList<>(extractRealmRoles(jwt));
        for (String clientRole : extractClientRoles(jwt)) {
            if (!allRoles.contains(clientRole)) {
                allRoles.add(clientRole);
            }
        }

        CurrentUser currentUser = currentUserResolver.resolve(jwt);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String role : allRoles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        for (String role : currentUser.roles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        for (String permission : currentUser.permissions()) {
            authorities.add(new SimpleGrantedAuthority(permission));
        }

        return new UsernamePasswordAuthenticationToken(currentUser, jwt, authorities);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Jwt jwt) {
        try {
            var realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof java.util.Map) {
                var roles = ((java.util.Map<String, Object>) realmAccess).get("roles");
                if (roles instanceof List) {
                    var roleList = (List<Object>) roles;
                    return roleList.stream()
                            .filter(Object.class::isInstance)
                            .map(Object::toString)
                            .map(String::toUpperCase)
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractClientRoles(Jwt jwt) {
        List<String> result = new ArrayList<>();
        try {
            var resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess instanceof java.util.Map) {
                var resourceMap = (java.util.Map<String, Object>) resourceAccess;
                for (var entry : resourceMap.entrySet()) {
                    if (entry.getValue() instanceof java.util.Map) {
                        var roles = ((java.util.Map<String, Object>) entry.getValue()).get("roles");
                        if (roles instanceof List) {
                            for (Object role : (List<Object>) roles) {
                                result.add(role.toString().toUpperCase());
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return result;
    }
}
