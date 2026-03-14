package com.etema.ragnarmmo.system.mobstats.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Unified configuration parsing utilities for mob stats system.
 * Consolidates ConfigMapParser and ConfigParsing functionality.
 */
public final class ConfigUtils {
    private ConfigUtils() {}

    // ===== From ConfigMapParser =====

    /** "minecraft:zombie=WARRIOR" -> map.put("minecraft:zombie","WARRIOR") */
    public static Map<String,String> parseKeyEqualsValue(List<? extends String> rows) {
        Map<String,String> m = new HashMap<>();
        if (rows == null) return m;
        for (String s : rows) {
            if (s == null) continue;
            int eq = s.indexOf('=');
            if (eq <= 0 || eq >= s.length()-1) continue;
            m.put(s.substring(0,eq).trim(), s.substring(eq+1).trim());
        }
        return m;
    }

    /** "WARRIOR: str=3,agi=1" -> map[class][stat]=value */
    public static Map<String, Map<String, Double>> parseClassVector(List<? extends String> rows) {
        Map<String, Map<String, Double>> out = new HashMap<>();
        if (rows == null) return out;
        for (String line : rows) {
            if (line == null) continue;
            String[] parts = line.split(":",2);
            if (parts.length != 2) continue;
            String cls = parts[0].trim().toUpperCase(Locale.ROOT);
            Map<String, Double> vec = Arrays.stream(parts[1].split(","))
                    .map(String::trim)
                    .map(s -> s.split("=",2))
                    .filter(a -> a.length==2)
                    .collect(Collectors.toMap(a->a[0].trim().toLowerCase(Locale.ROOT),
                            a->safeDouble(a[1].trim()),
                            (a,b)->b, LinkedHashMap::new));
            out.put(cls, vec);
        }
        return out;
    }

    private static double safeDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    // ===== From ConfigParsing =====

    /** Entrada de la forma "namespace:path=number" */
    public static Optional<Integer> getMinLevelFor(List<? extends String> entries, String id) {
        if (entries == null) return Optional.empty();
        for (String e : entries) {
            if (e == null) continue;
            String s = e.trim();
            int eq = s.lastIndexOf('=');
            if (eq <= 0 || eq >= s.length() - 1) continue;
            String key = s.substring(0, eq).trim();
            String val = s.substring(eq + 1).trim();
            if (id.equals(key)) {
                try {
                    int n = Integer.parseInt(val);
                    return Optional.of(Math.max(1, n));
                } catch (NumberFormatException ignore) {}
            }
        }
        return Optional.empty();
    }
}
