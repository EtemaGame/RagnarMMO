package com.etema.ragnarmmo.player.stats;

import com.mojang.serialization.Codec;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class StatContainer<K extends Enum<K>> {

    private final Class<K> keyClass;
    private final EnumMap<K, Integer> stats;

    public StatContainer(Class<K> keyClass) {
        this.keyClass = keyClass;
        this.stats = new EnumMap<>(keyClass);
        resetStats();
    }

    public static <K extends Enum<K>> Codec<StatContainer<K>> createCodec(
            Class<K> keyClass,
            Function<String, K> valueOf) {

        return Codec.unboundedMap(Codec.STRING, Codec.INT)
                .xmap(
                        map -> {
                            StatContainer<K> container = new StatContainer<>(keyClass);
                            map.forEach((key, value) -> {
                                try {
                                    K enumKey = valueOf.apply(key.toUpperCase(Locale.ROOT));
                                    container.set(enumKey, value);
                                } catch (IllegalArgumentException e) {
                                    // Ignore invalid keys.
                                }
                            });
                            return container;
                        },
                        container -> {
                            Map<String, Integer> map = new java.util.HashMap<>();
                            container.stats
                                    .forEach((key, value) -> map.put(key.name().toLowerCase(Locale.ROOT), value));
                            return map;
                        });
    }

    public void resetStats() {
        for (K key : keyClass.getEnumConstants()) {
            stats.put(key, 0);
        }
    }

    public int get(K key) {
        return stats.getOrDefault(key, 0);
    }

    public void set(K key, int value) {
        stats.put(key, Math.max(0, value));
    }

    public void add(K key, int amount) {
        set(key, get(key) + amount);
    }

    public void multiply(K key, double factor) {
        set(key, (int) (get(key) * factor));
    }

    public Map<K, Integer> view() {
        return Collections.unmodifiableMap(stats);
    }

    public void copyFrom(StatContainer<K> other) {
        this.stats.clear();
        this.stats.putAll(other.stats);
    }

    public String describe() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<K, Integer> entry : stats.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(entry.getKey().name().toLowerCase(Locale.ROOT))
                    .append('=')
                    .append(entry.getValue());
            first = false;
        }
        return builder.toString();
    }

    public int total() {
        return stats.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isEmpty() {
        return stats.values().stream().allMatch(v -> v == 0);
    }
}
