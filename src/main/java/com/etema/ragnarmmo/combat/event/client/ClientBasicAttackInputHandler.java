package com.etema.ragnarmmo.combat.event.client;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.combat.hand.AttackHandResolver;
import com.etema.ragnarmmo.combat.net.ServerboundRagnarBasicAttackPacket;
import com.etema.ragnarmmo.combat.timing.AttackCadenceCalculator;
import com.etema.ragnarmmo.common.net.Network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
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
    private static boolean nextIsOffHand = false;
    private static int localAttackCooldownTicks = 0;
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
            resetHoldState(true);
            return;
        }

        LocalPlayer player = minecraft.player;
        if (minecraft.screen != null || player.isSpectator() || !player.isAlive()) {
            resetHoldState(true);
            return;
        }

        boolean attackDown = minecraft.options.keyAttack.isDown();
        if (!attackDown) {
            resetHoldState(true);
            return;
        }

        wasAttackDown = true;

        if (localAttackCooldownTicks > 0) {
            localAttackCooldownTicks--;
        }

        if (AttackHandResolver.shouldResetCycle(player)) {
            nextIsOffHand = false;
        }

        if (!(minecraft.hitResult instanceof EntityHitResult hitResult)) {
            return;
        }

        if (localAttackCooldownTicks > 0) {
            return;
        }

        boolean dualWielding = AttackHandResolver.isDualWielding(player);
        boolean offHand = dualWielding && nextIsOffHand && AttackHandResolver.isValidAttackHand(player, true);
        int[] targetIds = new int[] { hitResult.getEntity().getId() };
        Network.sendToServer(new ServerboundRagnarBasicAttackPacket(
                sequenceId++,
                0,
                offHand,
                player.getInventory().selected,
                targetIds));

        player.swing(offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        localAttackCooldownTicks = AttackCadenceCalculator.computeIntervalTicks(player, offHand);
        nextIsOffHand = dualWielding ? AttackHandResolver.resolveNextHand(player, offHand) : false;
    }

    private static void resetHoldState(boolean resetCooldown) {
        wasAttackDown = false;
        nextIsOffHand = false;
        if (resetCooldown) {
            localAttackCooldownTicks = 0;
        }
    }
}
