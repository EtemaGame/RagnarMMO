package com.etema.ragnarmmo.common.api.mobs.runtime.integration;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import com.etema.ragnarmmo.system.mobstats.service.MobRuntimeInitializationService;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Runtime seam for the strict manual path.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public final class ManualMobProfileRuntimeIntegrator {

    private ManualMobProfileRuntimeIntegrator() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity entity) || entity instanceof Player) {
            return;
        }

        ManualMobProfileRuntimeStore.clear(entity);
        MobRuntimeInitializationService.tryInitialize(entity);
    }
}
