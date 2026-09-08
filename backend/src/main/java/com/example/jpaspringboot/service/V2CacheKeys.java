package com.example.jpaspringboot.service;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class V2CacheKeys {
    private V2CacheKeys() {
    }

    public static String all() {
        return "all";
    }

    public static String region(String regionId) {
        return normalize(regionId);
    }

    public static String filters(Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return "none";
        }
        return filters.entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(
                        entry -> normalize(entry.getKey()),
                        entry -> normalize(entry.getValue()),
                        (left, right) -> right,
                        TreeMap::new
                ))
                .entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
