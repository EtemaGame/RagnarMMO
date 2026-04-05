package com.etema.ragnarmmo.skill.job.blacksmith;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Adrenaline Rush — Active (Buff)
 * Adds ASPD bonus with Axes and Maces to party members.
 */
public class AdrenalineRushSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "adrenaline_rush");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.2, 0.5, 0.2, 0.05);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.2f);

            int durationTicks = (30 + level * 30) * 20; 
            int amplifier = level >= 5 ? 1 : 0;
            double radius = 15.0;

            AABB searchArea = player.getBoundingBox().inflate(radius);
            List<Player> nearbyPlayers = serverLevel.getEntitiesOfClass(Player.class, searchArea, p -> p.isAlive());

            for (Player p : nearbyPlayers) {
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, durationTicks, amplifier, false, true, true));
                p.getPersistentData().putInt("ragnarmmo_adrenaline_rush_level", level);
            }
        }
    }
}
