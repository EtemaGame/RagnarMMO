package com.etema.ragnarmmo.system.skills.acolyte;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.TickEvent;
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
        if (event.getEntity() instanceof ServerPlayer player) {
            int angelusLevel = player.getPersistentData().getInt("angelus_level");
            if (angelusLevel > 0) {
                // Reduction: level * 4%
                float reduction = angelusLevel * 0.04f;
                event.setAmount(event.getAmount() * (1.0f - reduction));
            }
        }
    }

    /**
     * Pneuma Hook: Blocks projectile damage if Pneuma is active.
     */
    @SubscribeEvent
    public static void onPneumaProtect(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            long pneumaExpiry = player.getPersistentData().getLong("pneuma_expiry");
            if (pneumaExpiry > player.level().getGameTime()) {
                // Check if source is a projectile
                if (event.getSource().getDirectEntity() instanceof Projectile) {
                    event.setAmount(0);
                    event.setCanceled(true);
                    // Pneuma in RO blocks specific cell. In MC we make it personal for simplicity.
                }
            }
        }
    }

    /**
     * Warp Portal Hook: Checks for portal walk-through.
     * In RO, warp portals are cells. In MC, we check if player is near their own
     * active warp portal coordinates for teleportation.
     */
    @SubscribeEvent
    public static void onPlayerWarpTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            var nbt = player.getPersistentData();
            if (nbt.contains("warp_expiry")) {
                long expiry = nbt.getLong("warp_expiry");
                if (expiry > player.level().getGameTime()) {
                    BlockPos portalPos = new BlockPos(nbt.getInt("warp_x"), nbt.getInt("warp_y"), nbt.getInt("warp_z"));
                    if (player.blockPosition().closerThan(portalPos, 1.5)) {
                        // Teleport trigger!
                        // RO warp portals usually go to a saved location.
                        // For now we implement as a "self-recall" or simpler: teleport to where it was SET.
                        // Actually, RO Warp Portal allows multiple destinations. 
                        // Our implementation plan says: "teleport to the position of the caster" or 
                        // "others get teleported THERE".
                        // Let's implement: any player touching the portal gets teleported to the cached destination.
                        // We need to check other players too.
                    }
                }
            }
        }
    }
}
