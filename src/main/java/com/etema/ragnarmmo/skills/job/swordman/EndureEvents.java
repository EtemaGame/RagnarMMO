package com.etema.ragnarmmo.skills.job.swordman;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles the actual knockback cancellation for the Endure skill.
 * When a player has an active Endure buff (tracked via PersistentData),
 * incoming knockback events are cancelled and the hit counter is decremented.
 * After 7 hits, Endure expires even if duration hasn't ended.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class EndureEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        CompoundTag data = player.getPersistentData();

        // Check if Endure is active
        long endureUntil = data.getLong(EndureSkillEffect.ENDURE_TAG);
        if (endureUntil <= 0) return;

        long now = player.level().getGameTime();
        if (now >= endureUntil) {
            // Duration expired — clean up
            data.remove(EndureSkillEffect.ENDURE_TAG);
            data.remove(EndureSkillEffect.ENDURE_HITS_TAG);
            return;
        }

        int hitsLeft = data.getInt(EndureSkillEffect.ENDURE_HITS_TAG);
        if (hitsLeft <= 0) {
            // All 7 hits exhausted — remove Endure
            data.remove(EndureSkillEffect.ENDURE_TAG);
            data.remove(EndureSkillEffect.ENDURE_HITS_TAG);
            return;
        }

        // Cancel this knockback event and decrement hit counter
        event.setCanceled(true);
        data.putInt(EndureSkillEffect.ENDURE_HITS_TAG, hitsLeft - 1);

        // Feedback: Sound and particles
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
        
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    5, 0.2, 0.2, 0.2, 0);
        }
    }
}
