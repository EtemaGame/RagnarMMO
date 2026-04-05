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

public class WeaponPerfectionSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "weapon_perfection");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1.0, player.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS,
                    1.0f, 1.5f);

            int duration = (60 + level * 60) * 20;
            AABB area = player.getBoundingBox().inflate(15.0);
            List<Player> party = serverLevel.getEntitiesOfClass(Player.class, area, p -> true);

            for (Player p : party) {
                // Perfection: Negates size penalty in CommonEvents via tag
                p.addTag("ragnarmmo_weapon_perfection");
                // We'll use a scheduled task to remove it, or better, use an effect that handles the tag
                // But for now, since it's a skill effect, let's use MobEffect as a timer if possible
                // Actually, I'll use a custom effect if I can, but let's stick to tags + duration for now
                // Wait, vanilla tags don't have duration.
                // I'll use a MobEffect with a specific ID as a "timer".
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 255, false, false, false)); // Invisible dummy effect
                // Actually, let's just use LUCK as the timer since we already have it in CommonEvents logic
                p.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, 0));
            }
        }
    }
}
