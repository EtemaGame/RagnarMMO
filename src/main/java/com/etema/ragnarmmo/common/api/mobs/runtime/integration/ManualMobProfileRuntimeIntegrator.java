package com.etema.ragnarmmo.common.api.mobs.runtime.integration;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobBackendResolver;
import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Narrow runtime seam that associates the new manual mob profile with covered entities.
 *
 * <p>This hook only attaches an already-resolved {@code ComputedMobProfile} for the strict manual path.
 * It does not replace the legacy runtime pipeline, apply attributes, or perform sync.</p>
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
        if (MobStatsConfigAccess.getLevelScalingMode() != RagnarConfigs.LevelScalingMode.MANUAL) {
            return;
        }

        var resolution = ManualMobBackendResolver.resolve(entity);
        if (resolution.profile() != null) {
            ManualMobProfileRuntimeStore.attach(entity, resolution.profile());
            ComputedMobProfileAttributeApplier.apply(entity, resolution.profile());
        }
    }
}
