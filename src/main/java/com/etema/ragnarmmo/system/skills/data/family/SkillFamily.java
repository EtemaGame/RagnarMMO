package com.etema.ragnarmmo.system.skills.data.family;

import com.etema.ragnarmmo.common.api.skills.SkillCategory;
import com.etema.ragnarmmo.common.api.skills.SkillTier;
import com.etema.ragnarmmo.common.api.skills.SkillUsageType;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Represents a skill family - a template that defines default values for a group of related skills.
 * Skills can inherit from a family and override specific properties.
 *
 * Example: "bolt" family defines defaults for Fire Bolt, Cold Bolt, Lightning Bolt
 */
public final class SkillFamily {
    private final ResourceLocation id;
    private final Map<String, Object> defaults;

    private SkillFamily(ResourceLocation id, Map<String, Object> defaults) {
        this.id = id;
        this.defaults = Collections.unmodifiableMap(new HashMap<>(defaults));
    }

    public ResourceLocation getId() {
        return id;
    }

    /**
     * Get default value for a field, or null if not defined.
     */
    public Object getDefault(String fieldName) {
        return defaults.get(fieldName);
    }

    /**
     * Get all default field names defined by this family.
     */
    public Set<String> getDefaultFields() {
        return defaults.keySet();
    }

    /**
     * Check if this family defines a default for the given field.
     */
    public boolean hasDefault(String fieldName) {
        return defaults.containsKey(fieldName);
    }

    /**
     * Get a string default value, or null if not defined or wrong type.
     */
    public String getStringDefault(String fieldName) {
        Object val = defaults.get(fieldName);
        return val instanceof String ? (String) val : null;
    }

    /**
     * Get an integer default value, or defaultValue if not defined or wrong type.
     */
    public int getIntDefault(String fieldName, int defaultValue) {
        Object val = defaults.get(fieldName);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return defaultValue;
    }

    /**
     * Get a double default value, or defaultValue if not defined or wrong type.
     */
    public double getDoubleDefault(String fieldName, double defaultValue) {
        Object val = defaults.get(fieldName);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Get a boolean default value, or defaultValue if not defined or wrong type.
     */
    public boolean getBooleanDefault(String fieldName, boolean defaultValue) {
        Object val = defaults.get(fieldName);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        return "SkillFamily{id=" + id + ", defaults=" + defaults.size() + " fields}";
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static class Builder {
        private final ResourceLocation id;
        private final Map<String, Object> defaults = new HashMap<>();

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder setDefault(String fieldName, Object value) {
            if (value != null) {
                defaults.put(fieldName, value);
            }
            return this;
        }

        public Builder category(SkillCategory category) {
            return setDefault("category", category.name());
        }

        public Builder tier(SkillTier tier) {
            return setDefault("tier", tier.name());
        }

        public Builder usageType(SkillUsageType usage) {
            return setDefault("usage", usage.name());
        }

        public Builder scalingStat(String stat) {
            return setDefault("scaling_stat", stat);
        }

        public Builder cooldownTicks(int ticks) {
            return setDefault("cooldown_ticks", ticks);
        }

        public Builder castTimeTicks(int ticks) {
            return setDefault("cast_time_ticks", ticks);
        }

        public Builder interruptible(boolean value) {
            return setDefault("interruptible", value);
        }

        public Builder baseCost(int cost) {
            return setDefault("base_cost", cost);
        }

        public Builder costPerLevel(int cost) {
            return setDefault("cost_per_level", cost);
        }

        public Builder maxLevel(int level) {
            return setDefault("max_level", level);
        }

        public Builder upgradeCost(int cost) {
            return setDefault("upgrade_cost", cost);
        }

        public SkillFamily build() {
            return new SkillFamily(id, defaults);
        }
    }
}
