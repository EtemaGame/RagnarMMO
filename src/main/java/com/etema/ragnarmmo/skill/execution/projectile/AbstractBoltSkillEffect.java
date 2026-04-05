package com.etema.ragnarmmo.skill.execution.projectile;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skill.runtime.SkillSequencer;
import com.etema.ragnarmmo.combat.element.ElementType;
import com.etema.ragnarmmo.skill.execution.projectile.ProjectileFactory;
import com.etema.ragnarmmo.skill.targeting.SkillTargeting;
import com.etema.ragnarmmo.skill.api.SkillVisuals;
import com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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

        // RO: Bolts deal 100% MATK each
        float damagePerHit = SkillDamageHelper.scaleByMATK(user, 100.0f);
        int hits = Math.min(level, 10);

        playCastVisual(serverLevel, user);

        for (int i = 0; i < hits; i++) {
            int delay = 10 + (i * 4); // Start after 10 ticks casting, 4 ticks between hits
            SkillSequencer.schedule(delay, () -> spawnHit(user, damagePerHit));
        }
    }

    protected void spawnHit(LivingEntity user, float damage) {
        if (!user.isAlive()) return;

        // Resolve strike position (target position or look position)
        Vec3 strikePos = SkillTargeting.resolveStrikePosition(user, 15.0);
        // Spawn 10 blocks above the target
        Vec3 startPos = strikePos.add(0, 10, 0);

        AbstractMagicProjectile projectile =
                ProjectileFactory.createBolt(elementType, user.level(), user, damage);

        projectile.setPos(startPos.x, startPos.y, startPos.z);
        // Shoot downwards
        projectile.shoot(0, -1, 0, projectile.getSpeed(), 0.0f);
        
        user.level().addFreshEntity(projectile);
    }

    protected void playCastVisual(ServerLevel level, LivingEntity user) {
        for (int t = 0; t < 10; t++) {
            SkillSequencer.schedule(t, () -> {
                if (!user.isAlive()) return;
                SkillVisuals.spawnCastParticles(level, user.position());
            });
        }
    }
}
