package com.etema.ragnarmmo.skill.job.novice;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.stats.ChangeReason;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;

import java.util.Set;

/**
 * Basic Skill - Passive (Novice)
 * Minecraft adaptation of RO's social/utility Basic Skill ladder.
 *
 * In this adaptation:
 * - Levels improve "resting" recovery while crouching and standing still.
 * - Lv5 automatically unlocks First Aid.
 * - Lv7 automatically unlocks Play Dead while still a Novice.
 * - Lv9 is checked by the class-change flow.
 */
public class BasicSkillEffect implements ISkillEffect {

    public static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "basic_skill");
    public static final ResourceLocation FIRST_AID = new ResourceLocation("ragnarmmo", "first_aid");
    public static final ResourceLocation PLAY_DEAD = new ResourceLocation("ragnarmmo", "play_dead");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public Set<TriggerType> getSupportedTriggers() {
        return Set.of(TriggerType.PERIODIC_TICK);
    }

    @Override
    public void onPeriodicTick(TickEvent.PlayerTickEvent event, ServerPlayer player, int level) {
        syncSpecialUnlocks(player, level);

        if (level <= 0) return;
        if (player.tickCount % 20 != 0) return;
        if (!player.isCrouching() || !player.onGround()) return;
        if (player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) return;
        if (player.hurtTime > 0) return;

        float healAmount = 0.10f * level;
        double resourceAmount = 0.15 * level;
        if (level >= 3) {
            healAmount *= 2.0f;
            resourceAmount *= 2.0;
        }
        final double finalResourceAmount = resourceAmount;

        if (player.getHealth() < player.getMaxHealth()) {
            player.heal(healAmount);
        }

        RagnarCoreAPI.get(player).ifPresent(stats -> {
            stats.addResource(finalResourceAmount);
        });

        if (player.tickCount % 40 == 0 && player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    3, 0.25, 0.2, 0.25, 0.01);
        }
    }

    private void syncSpecialUnlocks(ServerPlayer player, int basicLevel) {
        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            if (basicLevel >= 5 && skills.getSkillLevel(FIRST_AID) <= 0) {
                skills.setSkillLevel(FIRST_AID, 1, ChangeReason.SYSTEM);
            }

            boolean isNovice = RagnarCoreAPI.get(player)
                    .map(stats -> JobType.fromId(stats.getJobId()) == JobType.NOVICE)
                    .orElse(true);

            int playDeadLevel = skills.getSkillLevel(PLAY_DEAD);
            if (basicLevel >= 7 && isNovice) {
                if (playDeadLevel <= 0) {
                    skills.setSkillLevel(PLAY_DEAD, 1, ChangeReason.SYSTEM);
                }
            } else if (playDeadLevel > 0) {
                skills.setSkillLevel(PLAY_DEAD, 0, ChangeReason.SYSTEM);
            }
        });
    }
}
