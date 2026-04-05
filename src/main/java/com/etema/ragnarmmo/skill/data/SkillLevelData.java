package com.etema.ragnarmmo.skill.data;

import java.util.Map;
import java.util.Optional;

/**
 * Arbitrary per-level tuning values loaded from skill JSON.
 * Designed so datapacks can extend skill tuning without adding Java fields.
 */
public final class SkillLevelData {

    private final Map<String, Double> numericValues;
    private final Map<String, String> stringValues;
    private final Map<String, Boolean> booleanValues;

    public SkillLevelData(Map<String, Double> numericValues, Map<String, String> stringValues,
            Map<String, Boolean> booleanValues) {
        this.numericValues = numericValues != null ? Map.copyOf(numericValues) : Map.of();
        this.stringValues = stringValues != null ? Map.copyOf(stringValues) : Map.of();
        this.booleanValues = booleanValues != null ? Map.copyOf(booleanValues) : Map.of();
    }

    public Map<String, Double> getNumericValues() {
        return numericValues;
    }

    public Map<String, String> getStringValues() {
        return stringValues;
    }

    public Map<String, Boolean> getBooleanValues() {
        return booleanValues;
    }

    public Optional<Double> getNumber(String key) {
        return Optional.ofNullable(numericValues.get(key));
    }

    public Optional<Integer> getInt(String key) {
        return getNumber(key).map(value -> (int) Math.round(value));
    }

    public Optional<String> getString(String key) {
        return Optional.ofNullable(stringValues.get(key));
    }

    public Optional<Boolean> getBoolean(String key) {
        return Optional.ofNullable(booleanValues.get(key));
    }

    public boolean has(String key) {
        return numericValues.containsKey(key) || stringValues.containsKey(key) || booleanValues.containsKey(key);
    }
}
