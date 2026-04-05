package com.etema.ragnarmmo.common.api.jobs;

import com.etema.ragnarmmo.skill.api.SkillType;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Defines various jobs (classes) and their skill bonuses.
 */
public enum JobType {
    NOVICE("Novice"),

    // First Classes (Primary)
    SWORDSMAN("Swordsman"),
    MAGE("Mage"),
    ARCHER("Archer"),
    THIEF("Thief"),
    MERCHANT("Merchant"),
    ACOLYTE("Acolyte"),

    // Second Classes
    WIZARD("Wizard"), // Mage -> Wizard progression
    PRIEST("Priest"), // Acolyte -> Priest progression
    KNIGHT("Knight"), // Swordsman -> Knight progression
    HUNTER("Hunter"), // Archer -> Hunter progression
    ASSASSIN("Assassin"), // Thief -> Assassin progression
    BLACKSMITH("Blacksmith"); // Merchant -> Blacksmith progression

    private final String displayName;
    private final java.util.Map<SkillType, Double> xpMultipliers;
    private final java.util.Set<SkillType> classTreeSkills;

    JobType(String displayName) {
        this.displayName = displayName;
        this.xpMultipliers = new EnumMap<>(SkillType.class);
        this.classTreeSkills = java.util.EnumSet.noneOf(SkillType.class);
    }

    static {
        for (JobType job : values()) {
            job.registerBonuses();
        }
    }

    private void registerBonuses() {
        switch (this) {
            case NOVICE -> {
                classTreeSkills.add(SkillType.FIRST_AID);
                classTreeSkills.add(SkillType.BASIC_SKILL);
                classTreeSkills.add(SkillType.PLAY_DEAD);
            }
            case SWORDSMAN -> {
                classTreeSkills.add(SkillType.SWORD_MASTERY);
                classTreeSkills.add(SkillType.BASH);
                classTreeSkills.add(SkillType.PROVOKE);
                classTreeSkills.add(SkillType.ONE_HAND_MASTERY);
                classTreeSkills.add(SkillType.TWO_HAND_MASTERY);
                classTreeSkills.add(SkillType.ENDURANCE);

                // XP Bonuses (Examples, to be refined if needed)
                xpMultipliers.put(SkillType.SWORD_MASTERY, 1.3);
            }
            case MAGE -> {
                classTreeSkills.add(SkillType.STAFF_MASTERY);
                classTreeSkills.add(SkillType.SPELL_KNOWLEDGE);
                classTreeSkills.add(SkillType.MANA_CONTROL);
                classTreeSkills.add(SkillType.MAGIC_AMPLIFICATION);
                classTreeSkills.add(SkillType.ELEMENTAL_AFFINITY);
                classTreeSkills.add(SkillType.MAGIC_GUARD);
                classTreeSkills.add(SkillType.ARCANE_REGENERATION);
                classTreeSkills.add(SkillType.OVERCAST);
            }
            case ARCHER -> {
                classTreeSkills.add(SkillType.BOW_MASTERY);
                classTreeSkills.add(SkillType.ACCURACY_TRAINING);
                classTreeSkills.add(SkillType.CRITICAL_SHOT);
                classTreeSkills.add(SkillType.EVASION_BOOST);
                classTreeSkills.add(SkillType.WIND_WALKER);
                classTreeSkills.add(SkillType.KITING_INSTINCT);
            }
            case THIEF -> {
                classTreeSkills.add(SkillType.DAGGER_MASTERY);
                classTreeSkills.add(SkillType.BACKSTAB_TRAINING);
                classTreeSkills.add(SkillType.STEALTH_INSTINCT);
                classTreeSkills.add(SkillType.FLEE_TRAINING);
                classTreeSkills.add(SkillType.POISON_EXPERTISE);
                classTreeSkills.add(SkillType.FATAL_INSTINCT);
            }
            case ACOLYTE -> {
                classTreeSkills.add(SkillType.MACE_MASTERY);
                classTreeSkills.add(SkillType.FAITH);
                classTreeSkills.add(SkillType.DIVINE_PROTECTION);
                classTreeSkills.add(SkillType.HEAL_POWER);
                classTreeSkills.add(SkillType.HOLY_RESISTANCE);
                classTreeSkills.add(SkillType.BLESSING_AURA);
            }
            case MERCHANT -> {
                classTreeSkills.add(SkillType.TRADING_KNOWLEDGE);
                classTreeSkills.add(SkillType.CART_STRENGTH);
                classTreeSkills.add(SkillType.WEAPON_MAINTENANCE);
                classTreeSkills.add(SkillType.ARMOR_MAINTENANCE);
                classTreeSkills.add(SkillType.OVERCHARGE);
                classTreeSkills.add(SkillType.BUSINESS_MIND);
            }
            case WIZARD -> {
                // Second Class - uses dynamic skill tree system from wizard_2.json
                // Skills are defined in data/ragnarmmo/skills/ and loaded dynamically
            }
            case PRIEST -> {
                // Second Class - uses dynamic skill tree system from priest_2.json
            }
            case KNIGHT -> {
                // Second Class - uses dynamic skill tree system from knight_2.json
            }
            case HUNTER -> {
                // Second Class - uses dynamic skill tree system from hunter_2.json
            }
            case ASSASSIN -> {
                // Second Class - uses dynamic skill tree system from assassin_2.json
            }
            case BLACKSMITH -> {
                // Second Class - uses dynamic skill tree system from blacksmith_2.json
            }
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getId() {
        return name().toLowerCase(Locale.ROOT);
    }

    public double getXpMultiplier(SkillType skill) {
        return xpMultipliers.getOrDefault(skill, 1.0);
    }

    /**
     * @deprecated Use {@link #getAllowedSkillIds()} instead
     */
    @Deprecated
    public java.util.Set<SkillType> getAllowedSkills() {
        return classTreeSkills;
    }

    /**
     * Gets all allowed skill IDs as ResourceLocations.
     *
     * @return Set of allowed skill ResourceLocations
     */
    public Set<ResourceLocation> getAllowedSkillIds() {
        return classTreeSkills.stream()
                .map(SkillType::toResourceLocation)
                .collect(Collectors.toSet());
    }

    /**
     * Returns true if this job is a magical class (uses Mana).
     */
    public boolean isMagical() {
        return this == MAGE || this == ACOLYTE || this == WIZARD || this == PRIEST;
    }

    /**
     * Returns true if this job is a physical class (uses SP).
     */
    public boolean isPhysical() {
        return !isMagical();
    }

    /**
     * Returns the display label for the resource bar ("Mana" or "SP").
     */
    public String getResourceLabel() {
        return isMagical() ? "Mana" : "SP";
    }

    public static JobType fromId(String id) {
        String normalized = normalizeKey(id);
        if (normalized.isBlank())
            return NOVICE;

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return NOVICE;
        }
    }

    public static String normalizeKey(String id) {
        if (id == null) {
            return "";
        }

        String normalized = id.trim();
        if (normalized.isBlank()) {
            return "";
        }

        if (normalized.contains(":")) {
            normalized = normalized.substring(normalized.indexOf(':') + 1);
        }

        return normalized.toUpperCase(Locale.ROOT);
    }

    // ── Class Hierarchy ──

    /** Set of all First Classes (Primary). */
    public static final Set<JobType> FIRST_CLASSES = Set.of(
            SWORDSMAN, MAGE, ARCHER, THIEF, MERCHANT, ACOLYTE);

    /** Set of all Second Classes. */
    public static final Set<JobType> SECOND_CLASSES = Set.of(
            WIZARD, PRIEST, KNIGHT, HUNTER, ASSASSIN, BLACKSMITH);

    /**
     * Returns the tier of this job in the class hierarchy.
     * 0 = Novice, 1 = 1st class, 2 = 2nd class.
     */
    public int getTier() {
        return switch (this) {
            case NOVICE -> 0;
            case SWORDSMAN, MAGE, ARCHER, THIEF, MERCHANT, ACOLYTE -> 1;
            case WIZARD, PRIEST, KNIGHT, HUNTER, ASSASSIN, BLACKSMITH -> 2;
        };
    }

    /**
     * Returns the prerequisite (parent) job needed to reach this class.
     * Returns null for NOVICE.
     */
    public JobType getParent() {
        return switch (this) {
            case NOVICE -> null;
            case SWORDSMAN, MAGE, ARCHER, THIEF, MERCHANT, ACOLYTE -> NOVICE;
            case KNIGHT -> SWORDSMAN;
            case WIZARD -> MAGE;
            case HUNTER -> ARCHER;
            case PRIEST -> ACOLYTE;
            case ASSASSIN -> THIEF;
            case BLACKSMITH -> MERCHANT;
        };
    }

    /**
     * Returns the list of jobs this class can promote to.
     * Empty list if there are no promotions (leaf class or not yet implemented).
     */
    public List<JobType> getPromotions() {
        return switch (this) {
            case NOVICE -> List.of(SWORDSMAN, MAGE, ARCHER, THIEF, MERCHANT, ACOLYTE);
            case SWORDSMAN -> List.of(KNIGHT);
            case MAGE -> List.of(WIZARD);
            case ARCHER -> List.of(HUNTER);
            case THIEF -> List.of(ASSASSIN);
            case MERCHANT -> List.of(BLACKSMITH);
            case ACOLYTE -> List.of(PRIEST);
            // Second Classes have no further promotions yet
            case KNIGHT, WIZARD, HUNTER, PRIEST, ASSASSIN, BLACKSMITH -> List.of();
        };
    }

    /**
     * Resolves the First Class ancestor for any job.
     * - NOVICE → null
     * - First Class → itself
     * - Second Class → its parent (the First Class)
     */
    public JobType getFirstClassAncestor() {
        return switch (getTier()) {
            case 0 -> null;
            case 1 -> this;
            default -> getParent(); // Second Class parent is always First Class
        };
    }

    public boolean hasPromotions() {
        return !getPromotions().isEmpty();
    }

    public boolean canPromoteTo(JobType target) {
        return target != null && getPromotions().contains(target);
    }

    public boolean matchesExactOrAncestor(JobType allowedJob) {
        if (allowedJob == null) {
            return false;
        }
        if (this == allowedJob) {
            return true;
        }
        JobType firstClass = getFirstClassAncestor();
        return firstClass != null && firstClass == allowedJob;
    }

    public boolean matchesSkillRule(String allowedJobId) {
        String normalized = normalizeKey(allowedJobId);
        if (normalized.isBlank()) {
            return false;
        }
        if (NOVICE.name().equals(normalized)) {
            return true;
        }
        if (name().equals(normalized)) {
            return true;
        }
        JobType firstClass = getFirstClassAncestor();
        return firstClass != null && firstClass.name().equals(normalized);
    }
}
