package com.etema.ragnarmmo.system.skills;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * Hooks into mob AI ticks to enforce aggro targeting and clean up debuffs.
 *
 * - Retargets taunted mobs toward the taunting player each tick.
 * - Removes the Provoke DEF debuff when aggro expires or the mob dies.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class AggroEvents {

    // Must match the UUID used in ProvokeSkillEffect
    private static final UUID PROVOKE_DEF_DEBUFF_UUID = UUID.fromString("a1b2c3d4-dead-beef-cafe-000000000001");
    private static final String PROVOKE_DEF_UNTIL_TAG = "ragnar_provoke_def_until";

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        // --- Clean up expired Provoke DEF debuff ---
        long defUntil = mob.getPersistentData().getLong(PROVOKE_DEF_UNTIL_TAG);
        if (defUntil > 0 && mob.level().getGameTime() >= defUntil) {
            removeDefDebuff(mob);
        }

        // --- Enforce aggro retargeting ---
        UUID targetUUID = AggroManager.getAggroTarget(mob);
        if (targetUUID == null) return;

        ServerPlayer aggroTarget = serverLevel.getServer().getPlayerList().getPlayer(targetUUID);
        if (aggroTarget == null || !aggroTarget.isAlive()) {
            AggroManager.clearAggro(mob);
            removeDefDebuff(mob);
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
        removeDefDebuff(mob);
    }

    private static void removeDefDebuff(Mob mob) {
        var armorAttr = mob.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(PROVOKE_DEF_DEBUFF_UUID);
        }
        mob.getPersistentData().remove(PROVOKE_DEF_UNTIL_TAG);
    }
}
