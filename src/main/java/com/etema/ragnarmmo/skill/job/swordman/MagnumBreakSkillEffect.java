package com.etema.ragnarmmo.skill.job.swordman;

import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.skill.runtime.SkillVisualFx;
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

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "magnum_break");
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
        if (level <= 0) return;

        var defOpt = SkillRegistry.get(ID);
        if (user instanceof Player player) {
            float hpCost = defOpt
                    .map(def -> (float) def.getLevelDouble("hp_cost", level, defaultHpCost(level)))
                    .orElse(defaultHpCost(level));
            player.setHealth(Math.max(1.0f, player.getHealth() - hpCost));
        }

        float pct = defOpt
                .map(def -> (float) def.getLevelDouble("damage_percent", level, 100.0 + (20.0 * level)))
                .orElse(100f + (20f * level));
        float baseDamage = Math.max(com.etema.ragnarmmo.combat.damage.SkillDamageHelper.MIN_ATK,
                com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByATK(user, pct));

        double radius = defOpt
                .map(def -> def.getLevelDouble("aoe_radius", level, 2.5D))
                .orElse(2.5D);
        double hitMultiplier = defOpt
                .map(def -> def.getLevelDouble("accuracy_multiplier", level, 1.0 + (0.10 * level)))
                .orElse(1.0 + (0.10 * level));
        int burnSeconds = defOpt
                .map(def -> def.getLevelInt("burn_seconds", level, 3))
                .orElse(3);
        int buffDurationTicks = defOpt
                .map(def -> def.getLevelInt("buff_duration_ticks", level, 200))
                .orElse(200);
        double knockbackStrength = defOpt
                .map(def -> def.getLevelDouble("knockback_strength", level, 1.0D))
                .orElse(1.0D);
        AABB area = user.getBoundingBox().inflate(radius);
        List<Entity> nearby = user.level().getEntities(user, area,
                e -> e instanceof LivingEntity && e != user && e.isAlive());

        for (Entity e : nearby) {
            LivingEntity target = (LivingEntity) e;
            if (!SwordmanCombatUtil.rollPhysicalSkillHit(user, target, 0.0, hitMultiplier)) {
                if (user.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SMOKE,
                            target.getX(), target.getY() + 1.0, target.getZ(),
                            4, 0.15, 0.2, 0.15, 0.01);
                }
                continue;
            }

            DamageSource damageSource = user instanceof Player p
                    ? user.damageSources().playerAttack(p)
                    : user.damageSources().mobAttack(user);
            
            SwordmanCombatUtil.withSkillDamageContext(user,
                    () -> com.etema.ragnarmmo.combat.damage.SkillDamageHelper.dealSkillDamage(target, damageSource, baseDamage));
            target.setSecondsOnFire(burnSeconds);

            // Knockback away from caster
            net.minecraft.world.phys.Vec3 knockDir = target.position().subtract(user.position()).normalize();
            target.knockback(knockbackStrength, -knockDir.x, -knockDir.z);
        }

        // Grant +20% fire damage buff for 10 seconds via PersistentData
        long buffUntil = user.level().getGameTime() + buffDurationTicks;
        user.getPersistentData().putLong(FIRE_BUFF_TAG, buffUntil);
        user.addEffect(new MobEffectInstance(RagnarMobEffects.MAGNUM_BREAK_FIRE.get(), buffDurationTicks, 0, false,
                false, true));

        // --- Sounds & Visuals ---
        user.level().playSound(null, user.getX(), user.getY(), user.getZ(),
                com.etema.ragnarmmo.common.init.RagnarSounds.MAGNUM_BREAK.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
        
        user.level().playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.2f);

        if (user.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, user.getX(), user.getY() + 1.0, user.getZ(), 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.EXPLOSION, user.getX(), user.getY() + 1.0, user.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MAGMA_BLOCK.defaultBlockState()),
                    user.getX(), user.getY() + 0.7, user.getZ(), 26, 0.8, 0.35, 0.8, 0.06);
            SkillVisualFx.spawnRotatingRing(sl, user.position(), 1.1, 0.1, ParticleTypes.LAVA, 10, 0.0);
        }

        // Visual expansion: 4 rings of fire for smoother RO "blast" feel
        for (int r = 1; r <= 4; r++) {
            final double currentR = radius * (r / 4.0);
            com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(r * 2, () -> {
                if (user.level() instanceof ServerLevel serverLevel) {
                    // Circle particles
                    for (int i = 0; i < 360; i += 12) {
                        double rad = Math.toRadians(i);
                        double x = user.getX() + Math.cos(rad) * currentR;
                        double z = user.getZ() + Math.sin(rad) * currentR;
                        serverLevel.sendParticles(ParticleTypes.FLAME, x, user.getY() + 0.1, z, 1, 0, 0.1, 0, 0.05);
                        if (currentR > 1.5) {
                            serverLevel.sendParticles(ParticleTypes.SMALL_FLAME, x, user.getY() + 0.5, z, 1, 0.1, 0.2, 0.1, 0);
                        }
                    }
                    if (currentR >= radius) {
                        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, user.getX(), user.getY() + 0.5, user.getZ(), 10, 1.0, 0.5, 1.0, 0.05);
                    }
                }
            });
        }
    }

    /**
     * Passive component: applies the +20% fire damage buff if active.
     */
    @Override
    public void onOffensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        if (level <= 0) return;
        if (event.getSource().getDirectEntity() != player) return;
        if (SwordmanCombatUtil.isSkillDamageContext(player)) return;

        MobEffectInstance fireBuff = player.getEffect(RagnarMobEffects.MAGNUM_BREAK_FIRE.get());
        long fireUntil = player.getPersistentData().getLong(FIRE_BUFF_TAG);
        if (fireUntil <= 0 && fireBuff == null) return;

        if (fireUntil > 0 && player.level().getGameTime() >= fireUntil) {
            player.getPersistentData().remove(FIRE_BUFF_TAG);
            return;
        }

        float fireBonus = SkillRegistry.get(ID)
                .map(def -> (float) def.getLevelDouble("fire_bonus_percent", level, 20.0D))
                .orElse(20.0f);
        event.setAmount(event.getAmount() * (1.0f + (fireBonus / 100.0f)));
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
