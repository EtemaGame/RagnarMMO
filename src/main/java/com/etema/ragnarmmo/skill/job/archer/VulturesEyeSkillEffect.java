package com.etema.ragnarmmo.skill.job.archer;

import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraft.world.entity.projectile.AbstractArrow;

import java.util.Set;

public class VulturesEyeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "vultures_eye");

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
        if (level <= 0)
            return;

        // In RO, Vulture's Eye increases HIT (Accuracy).
        // For Minecraft, we'll simulate this by increasing arrow damage slightly
        // to represent "more accurate hits" and reward the skill investment.
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow) {
            if (arrow.getOwner() == player) {
                // RO: +1 HIT per level. We'll give a +2% damage bonus per level for ranged
                // attacks.
                float multiplier = 1.0f + (level * 0.02f);
                event.setAmount(event.getAmount() * multiplier);
            }
        }
    }
}
