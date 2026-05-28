package com.arcarshowcaseserver.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum PermissionType {
    PROFILE_READ("profile:read"),
    PROFILE_WRITE("profile:write"),
    LIKE_READ("like:read"),
    LIKE_WRITE("like:write"),
    CUSTOMIZATION_READ("customization:read"),
    CUSTOMIZATION_WRITE("customization:write"),
    RECOMMENDATION_FEEDBACK("recommendation:feedback"),
    CARS_AI_COMPARE("cars:ai:compare"),
    CARS_AI_RECOMMEND("cars:ai:recommend"),
    ADMIN_ALL("admin:all");

    private final String value;

    PermissionType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Set<String> userDefaults() {
        return Arrays.stream(values())
                .filter(permission -> permission != ADMIN_ALL)
                .map(PermissionType::value)
                .collect(Collectors.toSet());
    }

    public static Set<String> adminDefaults() {
        Set<String> permissions = userDefaults();
        permissions.add(ADMIN_ALL.value());
        return permissions;
    }
}
