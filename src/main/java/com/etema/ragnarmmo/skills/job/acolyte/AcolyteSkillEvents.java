package com.etema.ragnarmmo.skills.job.acolyte;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.entity.aoe.PneumaAoe;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Acolyte skill event hooks for passive and persistent effects.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class AcolyteSkillEvents {

    /**
     * Angelus Hook: Reduces incoming damage if Angelus is active.
     */
    @SubscribeEvent
    public static void onAngelusDamage(LivingDamageEvent event) {
        var angelus = event.getEntity().getEffect(RagnarMobEffects.ANGELUS.get());
        if (angelus == null) {
            return;
        }

        float vit = 0.0f;
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            vit = (float) RagnarCoreAPI.get(player).map(stats -> stats.getVIT()).orElse(1);
        }

        float bonusRate = 0.05f * (angelus.getAmplifier() + 1);
        float reduction = Math.min(0.25f, vit * bonusRate * 0.0035f);
    }

    /**
     * Pneuma Hook: Blocks projectile damage if Pneuma is active.
     */
    @SubscribeEvent
    public static void onPneumaProtect(LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof Projectile projectile)) {
            return;
        }

        boolean protectedByPneuma = !event.getEntity().level().getEntitiesOfClass(PneumaAoe.class,
                event.getEntity().getBoundingBox().inflate(2.5),
                aoe -> aoe.protects(event.getEntity()))
                .isEmpty();
        if (protectedByPneuma) {
            event.setCanceled(true);
            projectile.discard();
        }
    }

}
