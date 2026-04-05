package com.etema.ragnarmmo.combat.aggro;

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
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class AggroEvents {

    // Must match the UUID used in ProvokeSkillEffect
    private static final UUID PROVOKE_DEF_DEBUFF_UUID = UUID.fromString("a1b2c3d4-dead-beef-cafe-000000000001");
    private static final UUID PROVOKE_ATK_BUFF_UUID = UUID.fromString("a1b2c3d4-dead-beef-cafe-000000000002");
    private static final String PROVOKE_UNTIL_TAG = "ragnar_provoke_until";

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        // --- Clean up expired Provoke debuffs ---
        long until = mob.getPersistentData().getLong(PROVOKE_UNTIL_TAG);
        if (until > 0 && mob.level().getGameTime() >= until) {
            removeProvokeModifiers(mob);
        }

        // --- Enforce aggro retargeting ---
        UUID targetUUID = AggroManager.getAggroTarget(mob);
        if (targetUUID == null) return;

        ServerPlayer aggroTarget = serverLevel.getServer().getPlayerList().getPlayer(targetUUID);
        if (aggroTarget == null || !aggroTarget.isAlive()) {
            AggroManager.clearAggro(mob);
            removeProvokeModifiers(mob);
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
        removeProvokeModifiers(mob);
    }

    private static void removeProvokeModifiers(Mob mob) {
        var armorAttr = mob.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(PROVOKE_DEF_DEBUFF_UUID);
        }
        var atkAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null) {
            atkAttr.removeModifier(PROVOKE_ATK_BUFF_UUID);
        }
        mob.getPersistentData().remove(PROVOKE_UNTIL_TAG);
    }
}
