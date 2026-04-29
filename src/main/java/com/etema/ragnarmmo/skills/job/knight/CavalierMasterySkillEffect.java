package com.etema.ragnarmmo.skills.job.knight;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Set;

/**
 * Cavalier Mastery — Passive
 * RO: Reduces the ASPD penalty for being mounted. At level 5 (max), removes
 *     the penalty entirely, allowing full combat efficiency on Peco Peco.
 *
 * Minecraft:
 *  - When the player is riding a vehicle (mounted) and attacks, reduces the
 *    damage penalty that vanilla applies to mounted attacks by adding a partial
 *    Haste effect during combat.
 *  - At level 5, provides Haste 1 always while mounted to compensate for any
 *    attack speed loss while riding.
 *
 *  NOTE: Vanilla Minecraft doesn't penalize damage while riding, so this skill
 *  acts as a quality-of-life passive: it amplifies mounted combat by adding
 *  a stacking Haste when isPassenger() is true.
 */
public class CavalierMasterySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "cavalier_mastery");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public Set<TriggerType> getSupportedTriggers() {
        return Set.of(TriggerType.OFFENSIVE_HURT);
    }

    @Override
    public void onOffensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }
}
