package com.etema.ragnarmmo.combat.contract;

import com.etema.ragnarmmo.skills.runtime.PlayerSkillsProvider;

import com.etema.ragnarmmo.combat.element.ElementType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobType;

/**
 * Passive skill modifiers that belong inside the RO combat contract.
 */
public final class PassiveCombatModifierService {
    private static final ResourceLocation DEMON_BANE =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "demon_bane");
    private static final ResourceLocation DIVINE_PROTECTION =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "divine_protection");
    private static final ResourceLocation BEAST_BANE =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "beast_bane");
    private static final ResourceLocation IRON_TEMPERING =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "iron_tempering");
    private static final ResourceLocation STEEL_TEMPERING =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "steel_tempering");
    private static final ResourceLocation UNFAIR_TRICK =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "unfair_trick");

    private PassiveCombatModifierService() {
    }

    public static double applyOutgoingPhysicalDamage(CombatantProfile attacker, CombatantProfile defender, double damage) {
        if (!(attacker.entity() instanceof ServerPlayer player)) {
            return damage;
        }

        if (isDemonOrUndead(defender)) {
            int demonBaneLevel = skillLevel(player, DEMON_BANE);
            if (demonBaneLevel > 0) {
                damage += demonBaneLevel * 3.0D;
            }
        }

        int beastBaneLevel = skillLevel(player, BEAST_BANE);
        if (beastBaneLevel > 0 && isBeastOrInsect(defender)) {
            damage += beastBaneLevel * 4.0D;
        }
        damage = applyBlacksmithPhysicalDamage(player, defender, damage);
        return damage;
    }

    public static double applyIncomingPhysicalDamage(CombatantProfile attacker, CombatantProfile defender, double damage) {
        if (!(defender.entity() instanceof ServerPlayer player) || !isDemonOrUndead(attacker)) {
            return damage;
        }
        int level = skillLevel(player, DIVINE_PROTECTION);
        if (level <= 0) {
            return damage;
        }
        return Math.max(0.0D, damage - (level * 3.0D));
    }

    private static boolean isDemonOrUndead(CombatantProfile profile) {
        if (profile == null || profile.entity() == null) {
            return false;
        }
        String race = profile.modifiers().race();
        ElementType element = profile.modifiers().element();
        return "demon".equalsIgnoreCase(race)
                || "undead".equalsIgnoreCase(race)
                || element == ElementType.UNDEAD
                || profile.entity().getMobType() == MobType.UNDEAD;
    }

    private static boolean isBeastOrInsect(CombatantProfile profile) {
        if (profile == null || profile.entity() == null) {
            return false;
        }
        String race = profile.modifiers().race();
        var category = profile.entity().getType().getCategory();
        return "brute".equalsIgnoreCase(race)
                || "beast".equalsIgnoreCase(race)
                || "animal".equalsIgnoreCase(race)
                || "insect".equalsIgnoreCase(race)
                || category == net.minecraft.world.entity.MobCategory.CREATURE
                || category == net.minecraft.world.entity.MobCategory.WATER_CREATURE
                || profile.entity() instanceof net.minecraft.world.entity.monster.Spider
                || profile.entity() instanceof net.minecraft.world.entity.monster.CaveSpider
                || profile.entity() instanceof net.minecraft.world.entity.monster.Silverfish;
    }

    private static int skillLevel(ServerPlayer player, ResourceLocation skillId) {
        return PlayerSkillsProvider.get(player)
                .map(skills -> skills.getSkillLevel(skillId))
                .orElse(0);
    }

    private static double applyBlacksmithPhysicalDamage(ServerPlayer player, CombatantProfile defender, double damage) {
        var held = player.getMainHandItem();
        if (held.getItem() instanceof net.minecraft.world.item.TieredItem tiered) {
            var tier = tiered.getTier();
            if (tier == net.minecraft.world.item.Tiers.IRON || tier == net.minecraft.world.item.Tiers.STONE) {
                damage *= 1.0D + (skillLevel(player, IRON_TEMPERING) * 0.01D);
            } else if (tier == net.minecraft.world.item.Tiers.DIAMOND
                    || tier == net.minecraft.world.item.Tiers.NETHERITE) {
                damage *= 1.0D + (skillLevel(player, STEEL_TEMPERING) * 0.015D);
            }
        }

        int unfairTrickLevel = skillLevel(player, UNFAIR_TRICK);
        net.minecraft.world.entity.LivingEntity target = defender != null ? defender.entity() : null;
        if (unfairTrickLevel > 0
                && target != null
                && target.getMaxHealth() > 0.0F
                && target.getHealth() < target.getMaxHealth() * 0.5F) {
            damage *= 1.0D + (unfairTrickLevel * 0.03D);
        }

        int overThrustLevel = player.getPersistentData().getInt("ragnarmmo_over_thrust_level");
        if (overThrustLevel > 0) {
            damage *= 1.0D + (overThrustLevel * 0.05D);
        }
        return damage;
    }
}
