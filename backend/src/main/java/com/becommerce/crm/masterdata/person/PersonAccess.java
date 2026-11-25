package com.becommerce.crm.masterdata.person;

import com.becommerce.crm.shared.security.filter.CurrentUser;

/** Permissões finas do cadastro (doc 22.9). CRM_ADMIN e SUPER_ADMIN sempre passam. */
public final class PersonAccess {

    private PersonAccess() {}

    public static boolean has(CurrentUser user, String permission) {
        if (user == null) return false;
        if (user.roles().contains("CRM_ADMIN") || user.roles().contains(CurrentUser.PLATFORM_ADMIN_ROLE)) return true;
        return user.permissions().contains(permission) || user.permissions().contains("*");
    }

    public static void require(CurrentUser user, String permission, String message) {
        if (!has(user, permission)) throw PersonException.forbidden(message);
    }
}
