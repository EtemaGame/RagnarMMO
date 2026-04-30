package com.etema.ragnarmmo.combat.aggro;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.combat.status.RoCombatStatusService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Hooks into mob AI ticks to enforce aggro targeting and clean up debuffs.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class AggroEvents {

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        // --- Clean up expired Provoke debuffs ---
        long until = mob.getPersistentData().getLong(RoCombatStatusService.PROVOKE_UNTIL_TAG);
        if (until > 0 && mob.level().getGameTime() >= until) {
            removeProvokeState(mob);
        }

        // --- Enforce aggro retargeting ---
        UUID targetUUID = AggroManager.getAggroTarget(mob);
        if (targetUUID == null) return;

        ServerPlayer aggroTarget = serverLevel.getServer().getPlayerList().getPlayer(targetUUID);
        if (aggroTarget == null || !aggroTarget.isAlive()) {
            AggroManager.clearAggro(mob);
            removeProvokeState(mob);
            return;
        }

        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget == null || !currentTarget.getUUID().equals(targetUUID)) {
            mob.setTarget(aggroTarget);
        }
    }

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        // Clean up all Provoke state when the mob dies
        AggroManager.clearAggro(mob);
        removeProvokeState(mob);
    }

    private static void removeProvokeState(Mob mob) {
        RoCombatStatusService.clearProvoke(mob);
    }
}
