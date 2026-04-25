package com.etema.ragnarmmo.skills.job.mage;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles the hit-absorption mechanic for Safety Wall.
 * Cancels physical incoming attack events while the player has Safety Wall active.
 * Magic attacks pass through (DamageSource.isMagic() == true).
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class SafetyWallEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        CompoundTag data = player.getPersistentData();

        long until = data.getLong(SafetyWallSkillEffect.SW_UNTIL_TAG);
        if (until <= 0) return;

        long now = player.level().getGameTime();
        if (now >= until) {
            data.remove(SafetyWallSkillEffect.SW_HITS_TAG);
            data.remove(SafetyWallSkillEffect.SW_UNTIL_TAG);
            return;
        }

        int hitsLeft = data.getInt(SafetyWallSkillEffect.SW_HITS_TAG);
        if (hitsLeft <= 0) {
            data.remove(SafetyWallSkillEffect.SW_HITS_TAG);
            data.remove(SafetyWallSkillEffect.SW_UNTIL_TAG);
            return;
        }

        // Magic damage bypasses Safety Wall (matches RO behaviour).
        // In 1.20.1 Forge, DamageSource.isMagic() is not available as a method;
        // we inspect the msgId string to identify magic/environmental bypass sources.
        DamageSource src = event.getSource();
        String msgId = src.getMsgId();
        // Bypass: fire, explosions, freeze, and any environment source
        boolean isBypass = msgId.equals("inFire") || msgId.equals("onFire")
                || msgId.equals("fireworks") || msgId.equals("explosion")
                || msgId.equals("explosion.player") || msgId.equals("freeze")
                || msgId.equals("magic") || msgId.equals("indirectMagic")
                || msgId.equals("starve") || msgId.equals("fall") || msgId.equals("drown");
        if (isBypass) return;

        // Absorb the physical hit
        event.setCanceled(true);
        data.putInt(SafetyWallSkillEffect.SW_HITS_TAG, hitsLeft - 1);
    }
}
