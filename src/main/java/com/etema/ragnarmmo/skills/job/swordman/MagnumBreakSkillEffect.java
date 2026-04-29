package com.etema.ragnarmmo.skills.job.swordman;

import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.runtime.SkillVisualFx;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;
import java.util.Set;

/**
 * Magnum Break — Active (Fire AoE)
 * RO: Deals Fire property physical damage in a 5x5 area with knockback.
 *     Grants +20% Fire property damage bonus for 10 seconds after cast.
 *
 * Minecraft:
 *  - AoE fire damage + knockback (retained from original).
 *  - The +20% fire damage buff is now tracked via PersistentData tag on the player
 *    ("ragnar_magnum_fire_until"), and applied in onOffensiveHurt if active.
 */
public class MagnumBreakSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "magnum_break");
    public static final String FIRE_BUFF_TAG = "ragnar_magnum_fire_until";

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public Set<TriggerType> getSupportedTriggers() {
        return Set.of(TriggerType.OFFENSIVE_HURT);
    }

    @Override
    public void execute(LivingEntity user, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }

    /**
     * Passive component: applies the +20% fire damage buff if active.
     */
    @Override
    public void onOffensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }

    private float defaultHpCost(int level) {
        return switch (level) {
            case 1, 2 -> 20.0f;
            case 3, 4 -> 19.0f;
            case 5, 6 -> 18.0f;
            case 7, 8 -> 17.0f;
            default -> 16.0f;
        };
    }
}
