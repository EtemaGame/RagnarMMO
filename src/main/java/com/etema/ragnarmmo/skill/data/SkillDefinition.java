package com.etema.ragnarmmo.skill.data;

import com.etema.ragnarmmo.skill.api.SkillCategory;
import com.etema.ragnarmmo.skill.api.SkillTier;
import com.etema.ragnarmmo.skill.api.SkillUsageType;
import com.etema.ragnarmmo.skill.api.ISkillDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable implementation of ISkillDefinition.
 * Instances are created by SkillDataLoader from JSON files.
 */
public final class SkillDefinition implements ISkillDefinition {

    private final ResourceLocation id;
    private final String displayName;
    private final SkillCategory category;
    private final SkillTier tier;
    private final SkillUsageType usageType;
    private final String scalingStat;
    private final double xpMultiplier;

    // Costs
    private final int baseCost;
    private final int costPerLevel;

    // Timing
    private final int cooldownTicks;
    private final int castDelayTicks;
    private final int castTimeTicks;
    private final boolean interruptible;

    // Progression
    private final int maxLevel;
    private final int upgradeCost;
    private final boolean canGainXp;
    private final boolean canUpgradeWithPoints;

    // Requirements
    private final Map<ResourceLocation, Integer> requirements;
    private final Set<String> allowedJobs;

    // UI
    private final ResourceLocation icon;
    private final String textureName;
    private final int gridX;
    private final int gridY;

    // Effect
    private final String effectClass;

    private SkillDefinition(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "Skill ID cannot be null");
        this.displayName = Objects.requireNonNull(builder.displayName, "Display name cannot be null");
        this.category = Objects.requireNonNull(builder.category, "Category cannot be null");
        this.tier = Objects.requireNonNull(builder.tier, "Tier cannot be null");
        this.usageType = Objects.requireNonNull(builder.usageType, "Usage type cannot be null");
        this.scalingStat = builder.scalingStat != null ? builder.scalingStat : "STR";
        this.xpMultiplier = builder.xpMultiplier;

        this.baseCost = builder.baseCost;
        this.costPerLevel = builder.costPerLevel;

        this.cooldownTicks = builder.cooldownTicks;
        this.castDelayTicks = builder.castDelayTicks;
        this.castTimeTicks = builder.castTimeTicks;
        this.interruptible = builder.interruptible;

        this.maxLevel = builder.maxLevel;
        this.upgradeCost = builder.upgradeCost;
        this.canGainXp = builder.canGainXp;
        this.canUpgradeWithPoints = builder.canUpgradeWithPoints;

        this.requirements = builder.requirements != null ? Map.copyOf(builder.requirements) : Map.of();
        this.allowedJobs = builder.allowedJobs != null ? Set.copyOf(builder.allowedJobs) : Set.of();

        this.icon = builder.icon;
        this.textureName = builder.textureName != null ? builder.textureName : id.getPath();
        this.gridX = builder.gridX;
        this.gridY = builder.gridY;

        this.effectClass = builder.effectClass;
    }

    // === Identity ===

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Component getTranslatedName() {
        return Component.translatable(getTranslationKey());
    }

    @Override
    public String getTranslationKey() {
        return "skill." + id.getNamespace() + "." + id.getPath();
    }

    @Override
    public String getDescriptionKey() {
        return getTranslationKey() + ".desc";
    }

    // === Classification ===

    @Override
    public SkillCategory getCategory() {
        return category;
    }

    @Override
    public SkillTier getTier() {
        return tier;
    }

    @Override
    public SkillUsageType getUsageType() {
        return usageType;
    }

    @Override
    public String getScalingStat() {
        return scalingStat;
    }

    @Override
    public double getXpMultiplier() {
        return xpMultiplier;
    }

    // === Costs ===

    @Override
    public int getBaseCost() {
        return baseCost;
    }

    @Override
    public int getCostPerLevel() {
        return costPerLevel;
    }

    // === Timing ===

    @Override
    public int getCooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public int getCastDelayTicks() {
        return castDelayTicks;
    }

    @Override
    public int getCastTimeTicks() {
        return castTimeTicks;
    }

    @Override
    public boolean isInterruptible() {
        return interruptible;
    }

    // === Progression ===

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public int getUpgradeCost() {
        return upgradeCost;
    }

    @Override
    public boolean canGainXp() {
        return canGainXp;
    }

    @Override
    public boolean canUpgradeWithPoints() {
        return canUpgradeWithPoints;
    }

    // === Requirements ===

    @Override
    public Map<ResourceLocation, Integer> getRequirements() {
        return requirements;
    }

    @Override
    public Set<String> getAllowedJobs() {
        return allowedJobs;
    }

    // === UI ===

    @Override
    public ResourceLocation getIcon() {
        return icon != null ? icon
                : new ResourceLocation(id.getNamespace(), "textures/gui/skills/" + textureName + ".png");
    }

    @Override
    public String getTextureName() {
        return textureName;
    }

    @Override
    public int getGridX() {
        return gridX;
    }

    @Override
    public int getGridY() {
        return gridY;
    }

    // === Effect ===

    @Override
    public String getEffectClass() {
        return effectClass;
    }

    // === Object methods ===

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SkillDefinition that))
            return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "SkillDefinition{" + id + "}";
    }

    // === Builder ===

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static Builder builder(String namespace, String path) {
        return new Builder(new ResourceLocation(namespace, path));
    }

    public static final class Builder {
        private final ResourceLocation id;
        private String displayName;
        private SkillCategory category = SkillCategory.CLASS_PASSIVE;
        private SkillTier tier = SkillTier.FIRST;
        private SkillUsageType usageType = SkillUsageType.PASSIVE;
        private String scalingStat = "STR";
        private double xpMultiplier = 1.0;

        private int baseCost = 0;
        private int costPerLevel = 2;

        private int cooldownTicks = 20;
        private int castDelayTicks = 0;
        private int castTimeTicks = 0;
        private boolean interruptible = true;

        private int maxLevel = 10;
        private int upgradeCost = 1;
        private boolean canGainXp = false;
        private boolean canUpgradeWithPoints = true;

        private Map<ResourceLocation, Integer> requirements;
        private Set<String> allowedJobs;

        private ResourceLocation icon;
        private String textureName;
        private int gridX = 0;
        private int gridY = 0;

        private String effectClass;

        private Builder(ResourceLocation id) {
            this.id = id;
            this.displayName = id.getPath();
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder category(SkillCategory category) {
            this.category = category;
            return this;
        }

        public Builder tier(SkillTier tier) {
            this.tier = tier;
            return this;
        }

        public Builder usageType(SkillUsageType usageType) {
            this.usageType = usageType;
            return this;
        }

        public Builder scalingStat(String scalingStat) {
            this.scalingStat = scalingStat;
            return this;
        }

        public Builder xpMultiplier(double xpMultiplier) {
            this.xpMultiplier = xpMultiplier;
            return this;
        }

        public Builder baseCost(int baseCost) {
            this.baseCost = baseCost;
            return this;
        }

        public Builder costPerLevel(int costPerLevel) {
            this.costPerLevel = costPerLevel;
            return this;
        }

        public Builder cooldownTicks(int cooldownTicks) {
            this.cooldownTicks = cooldownTicks;
            return this;
        }

        public Builder castDelayTicks(int castDelayTicks) {
            this.castDelayTicks = castDelayTicks;
            return this;
        }

        public Builder castTimeTicks(int castTimeTicks) {
            this.castTimeTicks = castTimeTicks;
            return this;
        }

        public Builder interruptible(boolean interruptible) {
            this.interruptible = interruptible;
            return this;
        }

        public Builder maxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
            return this;
        }

        public Builder upgradeCost(int upgradeCost) {
            this.upgradeCost = upgradeCost;
            return this;
        }

        public Builder canGainXp(boolean canGainXp) {
            this.canGainXp = canGainXp;
            return this;
        }

        public Builder canUpgradeWithPoints(boolean canUpgradeWithPoints) {
            this.canUpgradeWithPoints = canUpgradeWithPoints;
            return this;
        }

        public Builder requirements(Map<ResourceLocation, Integer> requirements) {
            this.requirements = requirements;
            return this;
        }

        public Builder allowedJobs(Set<String> allowedJobs) {
            this.allowedJobs = allowedJobs;
            return this;
        }

        public Builder icon(ResourceLocation icon) {
            this.icon = icon;
            return this;
        }

        public Builder textureName(String textureName) {
            this.textureName = textureName;
            return this;
        }

        public Builder gridX(int gridX) {
            this.gridX = gridX;
            return this;
        }

        public Builder gridY(int gridY) {
            this.gridY = gridY;
            return this;
        }

        public Builder effectClass(String effectClass) {
            this.effectClass = effectClass;
            return this;
        }

        public SkillDefinition build() {
            return new SkillDefinition(this);
        }
    }
}
