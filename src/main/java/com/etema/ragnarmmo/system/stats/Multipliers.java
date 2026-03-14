package com.etema.ragnarmmo.system.stats;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumMap;
import java.util.Map;

/**
 * Manages multipliers for various entity attributes.
 * All multipliers default to 1.0 and are clamped to non-negative values.
 */
public class Multipliers {

    public enum Type {
        HEALTH,
        DAMAGE,
        DEFENSE,
        SPEED
    }

    private final EnumMap<Type, Double> values;

    public Multipliers() {
        this.values = new EnumMap<>(Type.class);
        reset();
    }

    /**
     * Resets all multipliers to 1.0.
     */
    public void reset() {
        for (Type type : Type.values()) {
            values.put(type, 1.0);
        }
    }

    /**
     * Gets a multiplier value.
     */
    public double get(Type type) {
        return values.getOrDefault(type, 1.0);
    }

    /**
     * Sets a multiplier value (clamped to >= 0.0).
     */
    public void set(Type type, double value) {
        values.put(type, Math.max(0.0, value));
    }

    /**
     * Multiplies an existing multiplier by a factor.
     */
    public void multiply(Type type, double factor) {
        set(type, get(type) * factor);
    }

    /**
     * Gets all multipliers as an unmodifiable map.
     */
    public Map<Type, Double> getAll() {
        return java.util.Collections.unmodifiableMap(values);
    }

    /**
     * Copies all values from another Multipliers instance.
     */
    public void copyFrom(Multipliers other) {
        this.values.clear();
        this.values.putAll(other.values);
    }

    // Convenience getters for common use
    public double health() {
        return get(Type.HEALTH);
    }

    public double damage() {
        return get(Type.DAMAGE);
    }

    public double defense() {
        return get(Type.DEFENSE);
    }

    public double speed() {
        return get(Type.SPEED);
    }

    // Convenience setters
    public void setHealth(double value) {
        set(Type.HEALTH, value);
    }

    public void setDamage(double value) {
        set(Type.DAMAGE, value);
    }

    public void setDefense(double value) {
        set(Type.DEFENSE, value);
    }

    public void setSpeed(double value) {
        set(Type.SPEED, value);
    }

    // Codec for serialization
    public static final Codec<Multipliers> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("health", 1.0).forGetter(m -> m.get(Type.HEALTH)),
            Codec.DOUBLE.optionalFieldOf("damage", 1.0).forGetter(m -> m.get(Type.DAMAGE)),
            Codec.DOUBLE.optionalFieldOf("defense", 1.0).forGetter(m -> m.get(Type.DEFENSE)),
            Codec.DOUBLE.optionalFieldOf("speed", 1.0).forGetter(m -> m.get(Type.SPEED)))
            .apply(instance, (health, damage, defense, speed) -> {
                Multipliers m = new Multipliers();
                m.setHealth(health);
                m.setDamage(damage);
                m.setDefense(defense);
                m.setSpeed(speed);
                return m;
            }));

    @Override
    public String toString() {
        return String.format("Multipliers{health=%.2f, damage=%.2f, defense=%.2f, speed=%.2f}",
                health(), damage(), defense(), speed());
    }
}






