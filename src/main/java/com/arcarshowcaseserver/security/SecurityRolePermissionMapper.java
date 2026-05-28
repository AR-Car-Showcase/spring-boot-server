package com.arcarshowcaseserver.security;

import com.arcarshowcaseserver.enums.PermissionType;

import java.util.HashSet;
import java.util.Set;

public final class SecurityRolePermissionMapper {

    private SecurityRolePermissionMapper() {
    }

    public static Set<String> permissionsForRoles(Set<String> roles) {
        Set<String> permissions = new HashSet<>();
        if (roles == null || roles.isEmpty()) {
            return permissions;
        }

        if (roles.contains("ADMIN")) {
            permissions.addAll(PermissionType.adminDefaults());
            return permissions;
        }

        if (roles.contains("DEFAULT")) {
            permissions.addAll(PermissionType.userDefaults());
        }

        return permissions;
    }

    public static Set<String> mergeExplicitAndRolePermissions(Set<String> roles, Set<String> explicitPermissions) {
        Set<String> merged = permissionsForRoles(roles);
        if (explicitPermissions != null) {
            merged.addAll(explicitPermissions);
        }
        return merged;
    }
}
