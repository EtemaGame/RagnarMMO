package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
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

public class OverThrustSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "over_thrust");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5,
                    0.5, 0.5, 0.1);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS,
                    1.0f, 0.8f);

            int duration = (10 + level * 10) * 20;

            AABB area = player.getBoundingBox().inflate(15.0);
            List<Player> party = serverLevel.getEntitiesOfClass(Player.class, area, p -> true);

            for (Player p : party) {
                // Over Thrust: Store level in data for BlacksmithSkillEvents multiplier
                p.getPersistentData().putInt("ragnarmmo_over_thrust_level", level);
                // We use STRENGTH as a visual indicator and timer
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, level - 1));
            }
        }
    }
}
