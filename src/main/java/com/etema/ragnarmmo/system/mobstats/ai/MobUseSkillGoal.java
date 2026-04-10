package com.etema.ragnarmmo.system.mobstats.ai;

import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadViewResolver;
import com.etema.ragnarmmo.system.mobstats.core.MobStats;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public class MobUseSkillGoal extends Goal {
    private final Mob mob;
    private final ResourceLocation skillId;
    private final double rangeSqr;
    private final int cooldown;
    private final int manaCost;
    private int cooldownTicks;

    public MobUseSkillGoal(Mob mob, ResourceLocation skillId, double range, int cooldown, int manaCost) {
        this.mob = mob;
        this.skillId = skillId;
        this.rangeSqr = range * range;
        this.cooldown = cooldown;
        this.manaCost = manaCost;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;

        double dist = mob.distanceToSqr(target);
        if (dist > rangeSqr) return false;

        // Check mana if mob has stats
        MobStats legacyStats = MobStatsProvider.get(mob).orElse(null);
        if (legacyStats != null) {
            return legacyStats.getMana() >= manaCost;
        }
        return true;
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        if (target != null) {
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        
        MobStats legacyStats = MobStatsProvider.get(mob).orElse(null);
        int skillLevel = resolveSkillLevel(legacyStats);

        if (legacyStats != null) {
            legacyStats.consumeMana(manaCost);
            // This is a simplified execution. In a real scenario, we'd want the full effect.
            // We can now execute the skill since we updated ISkillEffect
            com.etema.ragnarmmo.skill.data.SkillRegistry.getEffect(skillId)
                    .ifPresent(effect -> effect.execute(mob, skillLevel));
        }

        cooldownTicks = cooldown;
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    private int resolveSkillLevel(MobStats legacyStats) {
        var readView = MobConsumerReadViewResolver.resolve(mob, legacyStats).orElse(null);
        if (readView != null && readView.level() > 0) {
            return Math.max(1, readView.level() / 10);
        }
        if (legacyStats != null && legacyStats.getLevel() > 0) {
            return Math.max(1, legacyStats.getLevel() / 10);
        }
        return 1;
    }
}
