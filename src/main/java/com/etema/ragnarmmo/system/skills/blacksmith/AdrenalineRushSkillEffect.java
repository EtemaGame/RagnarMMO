package com.etema.ragnarmmo.system.skills.blacksmith;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
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

public class AdrenalineRushSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "adrenaline_rush");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.2,
                    0.5, 0.2, 0.05);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS,
                    1.0f, 1.2f);

            int duration = (30 + level * 30) * 20; // 30s + 30s/lvl
            int amplifier = level >= 5 ? 1 : 0;

            // Apply to player and party members in range
            AABB area = player.getBoundingBox().inflate(15.0);
            List<Player> party = serverLevel.getEntitiesOfClass(Player.class, area, p -> true);

            for (Player p : party) {
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, amplifier));
            }
        }
    }
}
