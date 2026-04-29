package com.etema.ragnarmmo.skills.execution.projectile;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.runtime.SkillSequencer;
import com.etema.ragnarmmo.combat.element.ElementType;
import com.etema.ragnarmmo.skills.execution.projectile.ProjectileFactory;
import com.etema.ragnarmmo.skills.targeting.SkillTargeting;
import com.etema.ragnarmmo.skills.api.SkillVisuals;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile;
import com.etema.ragnarmmo.skills.runtime.SkillVisualFx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Base class for "Bolt" style skills (Fire Bolt, Cold Bolt, Lightning Bolt).
 * Handles multi-hit sequencing, targeting, and projectile spawning.
 */
public abstract class AbstractBoltSkillEffect implements ISkillEffect {

    protected final ResourceLocation id;
    protected final ElementType elementType;

    protected AbstractBoltSkillEffect(ResourceLocation id, ElementType elementType) {
        this.id = id;
        this.elementType = elementType;
    }

    @Override
    public ResourceLocation getSkillId() {
        return id;
    }

    @Override
    public void execute(LivingEntity user, int level) {
        if (level <= 0 || !(user.level() instanceof ServerLevel serverLevel)) return;

        int defaultHits = Math.min(level, 10);
        float damagePercent = SkillRegistry.get(id)
                .map(def -> (float) def.getLevelDouble("damage_percent", level, 100.0))
                .orElse(100.0f);
        int hits = SkillRegistry.get(id)
                .map(def -> def.getLevelInt("hit_count", level, defaultHits))
                .orElse(defaultHits);
        int hitSpacingTicks = SkillRegistry.get(id)
                .map(def -> def.getLevelInt("hit_spacing_ticks", level, 4))
                .orElse(4);
        float visualOnlyDamage = 0.0f;

        playCastVisual(serverLevel, user);

        for (int i = 0; i < hits; i++) {
            // SkillEffectHandler already handled the cast bar, so release bolts immediately.
            int delay = i * hitSpacingTicks;
            SkillSequencer.schedule(delay, () -> spawnHit(user, visualOnlyDamage, level));
        }
    }

    protected void spawnHit(LivingEntity user, float damage, int level) {
        if (!user.isAlive()) return;

        // Resolve strike position (target position or look position)
        Vec3 strikePos = SkillTargeting.resolveStrikePosition(user, 15.0);
        double startHeight = SkillRegistry.get(id)
                .map(def -> def.getLevelDouble("projectile_start_height", level, 10.0))
                .orElse(10.0);
        Vec3 startPos = strikePos.add(0, startHeight, 0);

        AbstractMagicProjectile projectile =
                ProjectileFactory.createBolt(elementType, user.level(), user, damage);

        projectile.setPos(startPos.x, startPos.y, startPos.z);
        // Shoot downwards
        projectile.shoot(0, -1, 0, projectile.getSpeed(), 0.0f);
        
        user.level().addFreshEntity(projectile);
    }

    protected void playCastVisual(ServerLevel level, LivingEntity user) {
        for (int t = 0; t < 10; t++) {
            final int tick = t;
            SkillSequencer.schedule(t, () -> {
                if (!user.isAlive()) return;
                SkillVisuals.spawnCastParticles(level, user.position());
                SkillVisualFx.spawnRotatingRing(level, user.position(), 0.9, 0.1,
                        getPrimaryCastParticle(), 8, tick * 0.35);
                if (tick % 2 == 0) {
                    SkillVisualFx.spawnRotatingRing(level, user.position(), 0.55, 1.0,
                            getAccentCastParticle(), 4, -tick * 0.45);
                }
            });
        }

        level.playSound(null, user.getX(), user.getY(), user.getZ(), getCastSound(), SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    protected ParticleOptions getPrimaryCastParticle() {
        return switch (elementType) {
            case FIRE -> ParticleTypes.FLAME;
            case WATER -> ParticleTypes.SNOWFLAKE;
            case WIND -> ParticleTypes.ELECTRIC_SPARK;
            default -> ParticleTypes.ENCHANT;
        };
    }

    protected ParticleOptions getAccentCastParticle() {
        return switch (elementType) {
            case FIRE -> ParticleTypes.SMALL_FLAME;
            case WATER -> ParticleTypes.ITEM_SNOWBALL;
            case WIND -> ParticleTypes.GLOW;
            default -> ParticleTypes.END_ROD;
        };
    }

    protected net.minecraft.sounds.SoundEvent getCastSound() {
        return switch (elementType) {
            case FIRE -> SoundEvents.FIRECHARGE_USE;
            case WATER -> SoundEvents.GLASS_BREAK;
            case WIND -> SoundEvents.LIGHTNING_BOLT_IMPACT;
            default -> SoundEvents.ENCHANTMENT_TABLE_USE;
        };
    }
}
