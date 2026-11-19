package com.becommerce.crm.shared.security.authorization;

import org.springframework.stereotype.Component;

@Component("perm")
public class PermissionEvaluatorBean {

    private final CurrentUserAuthorities authorities;

    public PermissionEvaluatorBean(CurrentUserAuthorities authorities) {
        this.authorities = authorities;
    }

    public boolean has(String permission) {
        return isAdmin() || authorities.has(permission);
    }

    public boolean hasOrDependent(String owner) {
        if (owner == null || owner.isBlank()) {
            return false;
        }
        if (has(owner)) {
            return true;
        }
        return PermissionDependencies.screensDependingOn(owner).stream().anyMatch(authorities::has);
    }

    private boolean isAdmin() {
        return authorities.hasRole("CRM_ADMIN") || authorities.hasRole("SUPER_ADMIN");
    }
}
