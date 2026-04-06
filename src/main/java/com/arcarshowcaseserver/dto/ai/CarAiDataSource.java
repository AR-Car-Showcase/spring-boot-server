package com.arcarshowcaseserver.dto.ai;

public enum CarAiDataSource {
    AUTO,
    DB,
    JSON;

    public static CarAiDataSource fromValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return AUTO;
        }

        for (CarAiDataSource value : values()) {
            if (value.name().equalsIgnoreCase(rawValue.trim())) {
                return value;
            }
        }

        return AUTO;
    }
}
