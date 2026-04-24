package com.etema.ragnarmmo.common.api.jobs;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private final Map<ResourceLocation, Double> xpMultipliers;
    private final Set<ResourceLocation> classTreeSkills;

    JobType(String displayName) {
        this.displayName = displayName;
        this.xpMultipliers = new java.util.LinkedHashMap<>();
        this.classTreeSkills = new LinkedHashSet<>();
    }

    static {
        for (JobType job : values()) {
            job.registerBonuses();
        }
    }

    private void registerBonuses() {
        switch (this) {
            case NOVICE -> {
                classTreeSkills.add(skillId("first_aid"));
                classTreeSkills.add(skillId("basic_skill"));
                classTreeSkills.add(skillId("play_dead"));
            }
            case SWORDSMAN -> {
                classTreeSkills.add(skillId("sword_mastery"));
                classTreeSkills.add(skillId("bash"));
                classTreeSkills.add(skillId("provoke"));
                classTreeSkills.add(skillId("one_hand_mastery"));
                classTreeSkills.add(skillId("two_hand_mastery"));
                classTreeSkills.add(skillId("endurance"));

                // XP Bonuses (Examples, to be refined if needed)
                xpMultipliers.put(skillId("sword_mastery"), 1.3);
            }
            case MAGE -> {
                classTreeSkills.add(skillId("staff_mastery"));
                classTreeSkills.add(skillId("spell_knowledge"));
                classTreeSkills.add(skillId("mana_control"));
                classTreeSkills.add(skillId("magic_amplification"));
                classTreeSkills.add(skillId("elemental_affinity"));
                classTreeSkills.add(skillId("magic_guard"));
                classTreeSkills.add(skillId("arcane_regeneration"));
                classTreeSkills.add(skillId("overcast"));
            }
            case ARCHER -> {
                classTreeSkills.add(skillId("bow_mastery"));
                classTreeSkills.add(skillId("accuracy_training"));
                classTreeSkills.add(skillId("critical_shot"));
                classTreeSkills.add(skillId("evasion_boost"));
                classTreeSkills.add(skillId("wind_walker"));
                classTreeSkills.add(skillId("kiting_instinct"));
            }
            case THIEF -> {
                classTreeSkills.add(skillId("dagger_mastery"));
                classTreeSkills.add(skillId("backstab_training"));
                classTreeSkills.add(skillId("stealth_instinct"));
                classTreeSkills.add(skillId("flee_training"));
                classTreeSkills.add(skillId("poison_expertise"));
                classTreeSkills.add(skillId("fatal_instinct"));
            }
            case ACOLYTE -> {
                classTreeSkills.add(skillId("mace_mastery"));
                classTreeSkills.add(skillId("faith"));
                classTreeSkills.add(skillId("divine_protection"));
                classTreeSkills.add(skillId("heal_power"));
                classTreeSkills.add(skillId("holy_resistance"));
                classTreeSkills.add(skillId("blessing_aura"));
            }
            case MERCHANT -> {
                classTreeSkills.add(skillId("trading_knowledge"));
                classTreeSkills.add(skillId("weapon_maintenance"));
                classTreeSkills.add(skillId("armor_maintenance"));
                classTreeSkills.add(skillId("overcharge"));
                classTreeSkills.add(skillId("business_mind"));
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

    public double getXpMultiplier(ResourceLocation skillId) {
        if (skillId == null) {
            return 1.0;
        }
        return xpMultipliers.getOrDefault(skillId, 1.0);
    }

    /**
     * Gets all allowed skill IDs as ResourceLocations.
     *
     * @return Set of allowed skill ResourceLocations
     */
    public Set<ResourceLocation> getAllowedSkillIds() {
        if (!classTreeSkills.isEmpty()) {
            return Set.copyOf(classTreeSkills);
        }
        return switch (this) {
            case KNIGHT -> skillIds(
                    "sword_mastery", "increase_hp_recovery", "bash", "provoke",
                    "two_hand_mastery", "magnum_break", "endure");
            case WIZARD -> skillIds(
                    "increase_sp_recovery", "sight", "napalm_beat", "soul_strike", "safety_wall",
                    "cold_bolt", "frost_diver", "stone_curse", "fire_bolt", "fire_ball",
                    "fire_wall", "lightning_bolt", "thunder_storm");
            case HUNTER -> skillIds(
                    "owls_eye", "vultures_eye", "improve_concentration", "double_strafe", "arrow_shower");
            case ASSASSIN -> skillIds(
                    "double_attack", "improve_dodge", "steal", "hiding", "envenom", "detoxify");
            case BLACKSMITH -> skillIds(
                    "enlarge_weight_limit", "discount", "overcharge", "pushcart",
                    "vending", "buying_store", "identify", "mammonite");
            case PRIEST -> skillIds(
                    "divine_protection", "demon_bane", "angelus", "blessing", "heal",
                    "increase_agi", "decrease_agi", "cure", "ruwach", "teleportation",
                    "warp_portal", "pneuma", "aqua_benedicta", "holy_light", "signum_crucis");
            default -> Set.copyOf(classTreeSkills);
        };
    }

    private static Set<ResourceLocation> skillIds(String... paths) {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
        for (String path : paths) {
            ids.add(ResourceLocation.fromNamespaceAndPath("ragnarmmo", path));
        }
        return Set.copyOf(ids);
    }

    private static ResourceLocation skillId(String path) {
        return ResourceLocation.fromNamespaceAndPath("ragnarmmo", path);
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
     * Returns the display label for the resource bar (always "SP" after Phase 2 unification).
     */
    public String getResourceLabel() {
        return "SP";
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
