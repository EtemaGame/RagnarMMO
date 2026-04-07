package com.etema.ragnarmmo.combat.event.client;

import java.util.List;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.combat.integration.bettercombat.BetterCombatAttackSnapshot;
import com.etema.ragnarmmo.combat.integration.bettercombat.BetterCombatBridge;
import com.etema.ragnarmmo.combat.net.ServerboundRagnarBasicAttackPacket;
import com.etema.ragnarmmo.common.net.Network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side sender for the first server-authoritative basic attack flow.
 * It uses Better Combat data when available and falls back to crosshair target
 * detection otherwise.
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

        BetterCombatAttackSnapshot snapshot = BetterCombatBridge.snapshotCurrentClientAttack(minecraft);
        List<Integer> ids = snapshot.candidateTargetIds();
        if (ids == null || ids.isEmpty()) {
            return;
        }

        int[] targetIds = ids.stream().mapToInt(Integer::intValue).toArray();
        Network.sendToServer(new ServerboundRagnarBasicAttackPacket(
                sequenceId++,
                snapshot.comboIndex(),
                snapshot.offHand(),
                player.getInventory().selected,
                targetIds));
    }
}
