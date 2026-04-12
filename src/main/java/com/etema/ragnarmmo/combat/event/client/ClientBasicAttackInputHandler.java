package com.etema.ragnarmmo.combat.event.client;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.combat.net.ServerboundRagnarBasicAttackPacket;
import com.etema.ragnarmmo.common.net.Network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side sender for the first server-authoritative basic attack flow.
 * It only sends a request when the vanilla crosshair is on an entity.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientBasicAttackInputHandler {
    private static boolean wasAttackDown = false;
    private static int sequenceId = 1;

    private ClientBasicAttackInputHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            wasAttackDown = false;
            return;
        }

        boolean attackDown = minecraft.options.keyAttack.isDown();
        if (!attackDown) {
            wasAttackDown = false;
            return;
        }

        if (wasAttackDown) {
            return;
        }
        wasAttackDown = true;

        if (minecraft.screen != null) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (player.isSpectator() || !player.isAlive()) {
            return;
        }

        if (!(minecraft.hitResult instanceof EntityHitResult hitResult)) {
            return;
        }

        int[] targetIds = new int[] { hitResult.getEntity().getId() };
        Network.sendToServer(new ServerboundRagnarBasicAttackPacket(
                sequenceId++,
                0,
                false,
                player.getInventory().selected,
                targetIds));
    }
}
