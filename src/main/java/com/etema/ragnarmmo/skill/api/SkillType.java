package com.etema.ragnarmmo.skill.api;

import com.etema.ragnarmmo.skill.data.SkillDefinition;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Optional;

/**
 * Legacy skill type constants used for backward compatibility with
 * the data-driven {@link SkillRegistry} system.
 * <p>
 * Each constant provides a {@link #toResourceLocation()} bridge to the
 * new data-driven system. Metadata (displayName, grid position, etc.)
 * is now defined in JSON definitions, not in this enum.
 *
 * @deprecated Prefer using {@link ResourceLocation} IDs directly and
 *             looking up metadata via {@link SkillRegistry}.
 */
@Deprecated(forRemoval = true)
public enum SkillType {
        // === Novice Skills ===
        FIRST_AID, BASIC_SKILL, PLAY_DEAD,
        SWORDSMAN_SKILLS, SWORD_MASTERY, TWO_HAND_MASTERY, BASH, MAGNUM_BREAK, PROVOKE, ONE_HAND_MASTERY, ENDURANCE,
        // === Mage Skills ===
        STAFF_MASTERY, SPELL_KNOWLEDGE, MANA_CONTROL, MAGIC_AMPLIFICATION,
        ELEMENTAL_AFFINITY, MAGIC_GUARD, ARCANE_REGENERATION, OVERCAST,
        // === Archer Skills ===
        BOW_MASTERY, ACCURACY_TRAINING, CRITICAL_SHOT, EVASION_BOOST,
        WIND_WALKER, KITING_INSTINCT,
        // === Thief Skills ===
        DAGGER_MASTERY, BACKSTAB_TRAINING, STEALTH_INSTINCT, FLEE_TRAINING,
        POISON_EXPERTISE, FATAL_INSTINCT,
        // === Acolyte Skills ===
        MACE_MASTERY, FAITH, DIVINE_PROTECTION, HEAL_POWER, HOLY_RESISTANCE, BLESSING_AURA,
        // === Merchant Skills ===
        TRADING_KNOWLEDGE, CART_STRENGTH, WEAPON_MAINTENANCE, ARMOR_MAINTENANCE,
        OVERCHARGE, BUSINESS_MIND,
        // === Life Skills ===
        MINING, WOODCUTTING, EXCAVATION, FARMING, FISHING, EXPLORATION;

        public String getId() {
                return name().toLowerCase(Locale.ROOT);
        }

        public ResourceLocation toResourceLocation() {
                return ResourceLocation.fromNamespaceAndPath("ragnarmmo", getId());
        }

        public Optional<SkillDefinition> toDefinition() {
                return SkillRegistry.get(toResourceLocation());
        }

        public static SkillType fromId(String id) {
                if (id == null || id.isEmpty())
                        return null;
                try {
                        return valueOf(id.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                        return null;
                }
        }

        public static SkillType fromResourceLocation(ResourceLocation id) {
                if (id == null)
                        return null;
                if (!"ragnarmmo".equals(id.getNamespace()))
                        return null;
                return fromId(id.getPath());
        }
}
