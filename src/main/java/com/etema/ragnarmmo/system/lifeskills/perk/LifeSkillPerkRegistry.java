package com.etema.ragnarmmo.system.lifeskills.perk;

import com.etema.ragnarmmo.skill.api.SkillType;
import com.etema.ragnarmmo.system.lifeskills.perk.LifeSkillPerk.PerkEffect;

import java.util.*;

/**
 * Registry of all available Life Skill perks.
 * Defines the two choices available at each tier (every 10 levels).
 */
@SuppressWarnings("removal")
public class LifeSkillPerkRegistry {

        private static final Map<String, LifeSkillPerk> PERKS = new LinkedHashMap<>();
        private static final Map<SkillType, Map<Integer, List<LifeSkillPerk>>> PERKS_BY_SKILL_TIER = new EnumMap<>(
                        SkillType.class);

        static {
                // === MINING PERKS ===
                registerPerk(new LifeSkillPerk("mining_t1_a", SkillType.MINING, 1, "A",
                                "Efficient Miner", "+5% Mining Speed",
                                PerkEffect.MINING_SPEED, 0.05));
                registerPerk(new LifeSkillPerk("mining_t1_b", SkillType.MINING, 1, "B",
                                "Prospector", "+10% Ore Drop Chance",
                                PerkEffect.MINING_FORTUNE, 0.10));

                registerPerk(new LifeSkillPerk("mining_t2_a", SkillType.MINING, 2, "A",
                                "Speed Miner", "+10% Mining Speed",
                                PerkEffect.MINING_SPEED, 0.10));
                registerPerk(new LifeSkillPerk("mining_t2_b", SkillType.MINING, 2, "B",
                                "Lucky Strike", "+15% Ore Drop Chance",
                                PerkEffect.MINING_FORTUNE, 0.15));

                registerPerk(new LifeSkillPerk("mining_t3_a", SkillType.MINING, 3, "A",
                                "Master Miner", "+15% Mining Speed",
                                PerkEffect.MINING_SPEED, 0.15));
                registerPerk(new LifeSkillPerk("mining_t3_b", SkillType.MINING, 3, "B",
                                "Double Ore", "5% Double Ore Drop",
                                PerkEffect.MINING_DOUBLE_DROP, 0.05));

                // Tiers 4-10 for mining
                for (int tier = 4; tier <= 10; tier++) {
                        double speedBonus = 0.05 + (tier - 1) * 0.02;
                        double fortuneBonus = 0.05 + (tier - 1) * 0.02;
                        registerPerk(new LifeSkillPerk("mining_t" + tier + "_a", SkillType.MINING, tier, "A",
                                        "Mining Speed " + tier, String.format("+%.0f%% Mining Speed", speedBonus * 100),
                                        PerkEffect.MINING_SPEED, speedBonus));
                        registerPerk(new LifeSkillPerk("mining_t" + tier + "_b", SkillType.MINING, tier, "B",
                                        "Mining Fortune " + tier,
                                        String.format("+%.0f%% Drop Chance", fortuneBonus * 100),
                                        PerkEffect.MINING_FORTUNE, fortuneBonus));
                }

                // === WOODCUTTING PERKS ===
                registerPerk(new LifeSkillPerk("woodcutting_t1_a", SkillType.WOODCUTTING, 1, "A",
                                "Swift Axe", "+5% Chop Speed",
                                PerkEffect.WOODCUTTING_SPEED, 0.05));
                registerPerk(new LifeSkillPerk("woodcutting_t1_b", SkillType.WOODCUTTING, 1, "B",
                                "Extra Logs", "+10% Extra Log Chance",
                                PerkEffect.WOODCUTTING_EXTRA_LOGS, 0.10));

                for (int tier = 2; tier <= 10; tier++) {
                        double speedBonus = 0.05 + (tier - 1) * 0.02;
                        double logBonus = 0.05 + (tier - 1) * 0.02;
                        registerPerk(new LifeSkillPerk("woodcutting_t" + tier + "_a", SkillType.WOODCUTTING, tier, "A",
                                        "Chop Speed " + tier, String.format("+%.0f%% Chop Speed", speedBonus * 100),
                                        PerkEffect.WOODCUTTING_SPEED, speedBonus));
                        registerPerk(new LifeSkillPerk("woodcutting_t" + tier + "_b", SkillType.WOODCUTTING, tier, "B",
                                        "Log Fortune " + tier, String.format("+%.0f%% Extra Logs", logBonus * 100),
                                        PerkEffect.WOODCUTTING_EXTRA_LOGS, logBonus));
                }

                // === EXCAVATION PERKS ===
                registerPerk(new LifeSkillPerk("excavation_t1_a", SkillType.EXCAVATION, 1, "A",
                                "Efficient Digger", "+5% Dig Speed",
                                PerkEffect.EXCAVATION_SPEED, 0.05));
                registerPerk(new LifeSkillPerk("excavation_t1_b", SkillType.EXCAVATION, 1, "B",
                                "Treasure Hunter", "+10% Treasure Chance",
                                PerkEffect.EXCAVATION_TREASURE, 0.10));

                for (int tier = 2; tier <= 10; tier++) {
                        double speedBonus = 0.05 + (tier - 1) * 0.02;
                        double treasureBonus = 0.05 + (tier - 1) * 0.02;
                        registerPerk(new LifeSkillPerk("excavation_t" + tier + "_a", SkillType.EXCAVATION, tier, "A",
                                        "Dig Speed " + tier, String.format("+%.0f%% Dig Speed", speedBonus * 100),
                                        PerkEffect.EXCAVATION_SPEED, speedBonus));
                        registerPerk(new LifeSkillPerk("excavation_t" + tier + "_b", SkillType.EXCAVATION, tier, "B",
                                        "Treasure Find " + tier, String.format("+%.0f%% Treasure", treasureBonus * 100),
                                        PerkEffect.EXCAVATION_TREASURE, treasureBonus));
                }

                // === FARMING PERKS ===
                registerPerk(new LifeSkillPerk("farming_t1_a", SkillType.FARMING, 1, "A",
                                "Green Thumb", "+10% Extra Crops",
                                PerkEffect.FARMING_EXTRA_CROPS, 0.10));
                registerPerk(new LifeSkillPerk("farming_t1_b", SkillType.FARMING, 1, "B",
                                "Fertile Soil", "+5% Growth Speed",
                                PerkEffect.FARMING_GROWTH_SPEED, 0.05));

                for (int tier = 2; tier <= 10; tier++) {
                        double cropBonus = 0.05 + (tier - 1) * 0.02;
                        double growthBonus = 0.03 + (tier - 1) * 0.01;
                        registerPerk(new LifeSkillPerk("farming_t" + tier + "_a", SkillType.FARMING, tier, "A",
                                        "Crop Yield " + tier, String.format("+%.0f%% Extra Crops", cropBonus * 100),
                                        PerkEffect.FARMING_EXTRA_CROPS, cropBonus));
                        registerPerk(new LifeSkillPerk("farming_t" + tier + "_b", SkillType.FARMING, tier, "B",
                                        "Growth Boost " + tier,
                                        String.format("+%.0f%% Growth Speed", growthBonus * 100),
                                        PerkEffect.FARMING_GROWTH_SPEED, growthBonus));
                }

                // === FISHING PERKS ===
                registerPerk(new LifeSkillPerk("fishing_t1_a", SkillType.FISHING, 1, "A",
                                "Quick Catch", "+10% Fishing Speed",
                                PerkEffect.FISHING_SPEED, 0.10));
                registerPerk(new LifeSkillPerk("fishing_t1_b", SkillType.FISHING, 1, "B",
                                "Lucky Angler", "+10% Treasure Chance",
                                PerkEffect.FISHING_TREASURE, 0.10));

                for (int tier = 2; tier <= 10; tier++) {
                        double speedBonus = 0.05 + (tier - 1) * 0.02;
                        double treasureBonus = 0.05 + (tier - 1) * 0.02;
                        registerPerk(new LifeSkillPerk("fishing_t" + tier + "_a", SkillType.FISHING, tier, "A",
                                        "Reel Speed " + tier, String.format("+%.0f%% Fishing Speed", speedBonus * 100),
                                        PerkEffect.FISHING_SPEED, speedBonus));
                        registerPerk(new LifeSkillPerk("fishing_t" + tier + "_b", SkillType.FISHING, tier, "B",
                                        "Treasure Fish " + tier, String.format("+%.0f%% Treasure", treasureBonus * 100),
                                        PerkEffect.FISHING_TREASURE, treasureBonus));
                }

                // === EXPLORATION PERKS ===
                registerPerk(new LifeSkillPerk("exploration_t1_a", SkillType.EXPLORATION, 1, "A",
                                "Lucky Looter", "+10% Chest Loot Quality",
                                PerkEffect.EXPLORATION_CHEST_LOOT, 0.10));
                registerPerk(new LifeSkillPerk("exploration_t1_b", SkillType.EXPLORATION, 1, "B",
                                "Monster Hunter", "+10% Mob Drop Quality",
                                PerkEffect.EXPLORATION_MOB_DROP, 0.10));

                for (int tier = 2; tier <= 10; tier++) {
                        double lootBonus = 0.05 + (tier - 1) * 0.02;
                        double dropBonus = 0.05 + (tier - 1) * 0.02;
                        registerPerk(new LifeSkillPerk("exploration_t" + tier + "_a", SkillType.EXPLORATION, tier, "A",
                                        "Chest Loot " + tier, String.format("+%.0f%% Chest Quality", lootBonus * 100),
                                        PerkEffect.EXPLORATION_CHEST_LOOT, lootBonus));
                        registerPerk(new LifeSkillPerk("exploration_t" + tier + "_b", SkillType.EXPLORATION, tier, "B",
                                        "Mob Drops " + tier, String.format("+%.0f%% Drop Quality", dropBonus * 100),
                                        PerkEffect.EXPLORATION_MOB_DROP, dropBonus));
                }
        }

        private static void registerPerk(LifeSkillPerk perk) {
                PERKS.put(perk.getId(), perk);

                PERKS_BY_SKILL_TIER
                                .computeIfAbsent(perk.getSkill(), k -> new HashMap<>())
                                .computeIfAbsent(perk.getTier(), k -> new ArrayList<>())
                                .add(perk);
        }

        /**
         * Get perk by ID.
         */
        public static LifeSkillPerk getPerk(String id) {
                return PERKS.get(id);
        }

        /**
         * Get the two perk choices for a skill at a specific tier.
         */
        public static List<LifeSkillPerk> getPerksForTier(SkillType skill, int tier) {
                Map<Integer, List<LifeSkillPerk>> tierMap = PERKS_BY_SKILL_TIER.get(skill);
                if (tierMap == null)
                        return Collections.emptyList();
                return tierMap.getOrDefault(tier, Collections.emptyList());
        }

        /**
         * Get all perks for a skill.
         */
        public static List<LifeSkillPerk> getPerksForSkill(SkillType skill) {
                List<LifeSkillPerk> result = new ArrayList<>();
                Map<Integer, List<LifeSkillPerk>> tierMap = PERKS_BY_SKILL_TIER.get(skill);
                if (tierMap != null) {
                        for (List<LifeSkillPerk> perks : tierMap.values()) {
                                result.addAll(perks);
                        }
                }
                return result;
        }
}
