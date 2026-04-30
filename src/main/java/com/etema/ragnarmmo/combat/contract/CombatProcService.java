package com.etema.ragnarmmo.combat.contract;

import java.util.Random;

import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.runtime.PlayerSkillsProvider;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Passive combat procs that must be resolved by the RO combat contract, not by
 * LivingHurtEvent side effects.
 */
public final class CombatProcService {
    private static final ResourceLocation DOUBLE_ATTACK =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "double_attack");
    private static final TagKey<Item> DAGGER_TAG =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("ragnarmmo", "daggers"));

    private CombatProcService() {
    }

    public static double applyBasicAttackDamageMultiplier(CombatantProfile attacker, double damage, Random rng) {
        if (!(attacker.entity() instanceof ServerPlayer player) || rng == null || !player.getMainHandItem().is(DAGGER_TAG)) {
            return damage;
        }

        int level = PlayerSkillsProvider.get(player)
                .map(skills -> skills.getSkillLevel(DOUBLE_ATTACK))
                .orElse(0);
        if (level <= 0) {
            return damage;
        }

        double chance = SkillRegistry.get(DOUBLE_ATTACK)
                .map(def -> def.getLevelDouble("proc_chance", level, level * 0.05D))
                .orElse(level * 0.05D);
        if (rng.nextDouble() >= chance) {
            return damage;
        }

        double multiplier = SkillRegistry.get(DOUBLE_ATTACK)
                .map(def -> def.getLevelDouble("damage_multiplier", level, 2.0D))
                .orElse(2.0D);
        return damage * Math.max(1.0D, multiplier);
    }
}
