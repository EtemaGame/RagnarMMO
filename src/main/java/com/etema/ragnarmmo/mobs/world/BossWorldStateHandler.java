package com.etema.ragnarmmo.mobs.world;

import com.etema.ragnarmmo.RagnarMMO;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RagnarMMO.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BossWorldStateHandler {

    private static final int CLEANUP_INTERVAL_TICKS = 200;

    private BossWorldStateHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)
                || !(event.getLevel() instanceof ServerLevel serverLevel)
                || serverLevel.getServer() == null) {
            return;
        }

        BossTierResolver.resolveTier(living)
                .filter(tier -> tier.shouldPersistWorldState())
                .ifPresent(tier -> ActiveBossesSavedData.get(serverLevel.getServer())
                        .registerBoss(serverLevel, living, tier));
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity().level() instanceof ServerLevel serverLevel) || serverLevel.getServer() == null) {
            return;
        }

        BossTierResolver.resolveTier(event.getEntity())
                .filter(tier -> tier.shouldPersistWorldState())
                .ifPresent(tier -> ActiveBossesSavedData.get(serverLevel.getServer())
                        .handleBossDeath(serverLevel, event.getEntity(), tier));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % CLEANUP_INTERVAL_TICKS != 0) {
            return;
        }

        ActiveBossesSavedData.get(event.getServer()).pruneAndRefresh(event.getServer());
    }
}
