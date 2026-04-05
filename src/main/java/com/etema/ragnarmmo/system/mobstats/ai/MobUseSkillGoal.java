package com.etema.ragnarmmo.system.mobstats.ai;

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
        var statsOpt = MobStatsProvider.get(mob);
        if (statsOpt.isPresent()) {
            return statsOpt.orElseThrow(() -> new IllegalStateException("Missing stats"))
                .getMana() >= manaCost;
        }
        return true;
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        if (target != null) {
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        
        // Use skill via SkillRegistry
        int skillLevel = MobStatsProvider.get(mob).map(s -> Math.max(1, s.getLevel() / 10)).orElse(1);
        
        MobStatsProvider.get(mob).ifPresent(stats -> {
            stats.consumeMana(manaCost);
            // This is a simplified execution. In a real scenario, we'd want the full effect.
            // We can now execute the skill since we updated ISkillEffect
            com.etema.ragnarmmo.skill.data.SkillRegistry.getEffect(skillId).ifPresent(effect -> effect.execute(mob, skillLevel));
        });

        cooldownTicks = cooldown;
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }
}
