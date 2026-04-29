package com.etema.ragnarmmo.mobs.event;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public final class MobCombatHandler {

    private MobCombatHandler() {
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingHurt(LivingHurtEvent event) {
        // RO combat damage is resolved by RagnarCombatEngine. This event handler remains only
        // as an integration anchor for legacy registration order and must not mutate damage.
    }
}
