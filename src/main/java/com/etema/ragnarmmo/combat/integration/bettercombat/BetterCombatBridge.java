package com.etema.ragnarmmo.combat.integration.bettercombat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.fml.ModList;

/**
 * Optional Better Combat bridge. This class must remain defensive because the
 * dependency may change between versions or be absent at runtime.
 */
public final class BetterCombatBridge {
    private BetterCombatBridge() {
    }

    public static BetterCombatAttackSnapshot snapshotCurrentClientAttack(Minecraft minecraft) {
        int comboIndex = 0;
        boolean offHand = false;
        String animationId = "";
        double upswingRatio = 0.0D;
        double attackRange = 3.0D;
        List<Integer> candidateTargetIds = new ArrayList<>();

        if (minecraft == null || minecraft.player == null) {
            return new BetterCombatAttackSnapshot(comboIndex, offHand, animationId, upswingRatio, attackRange,
                    candidateTargetIds);
        }

        if (minecraft.hitResult instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();
            if (entity != null) {
                candidateTargetIds.add(entity.getId());
            }
        }

        if (!ModList.get().isLoaded("bettercombat")) {
            return new BetterCombatAttackSnapshot(comboIndex, offHand, animationId, upswingRatio, attackRange,
                    candidateTargetIds);
        }

        try {
            comboIndex = readComboIndex(minecraft);
            AttackHandSnapshot handSnapshot = readCurrentAttackHand(minecraft.player, comboIndex);
            if (handSnapshot != null) {
                offHand = handSnapshot.offHand();
                if (handSnapshot.animationId() != null) {
                    animationId = handSnapshot.animationId();
                }
                if (handSnapshot.upswingRatio() >= 0.0D) {
                    upswingRatio = handSnapshot.upswingRatio();
                }
                if (handSnapshot.attackRange() > 0.0D) {
                    attackRange = handSnapshot.attackRange();
                }
                List<Integer> reflectedTargets = readCandidateTargets(minecraft, minecraft.player, handSnapshot);
                if (!reflectedTargets.isEmpty()) {
                    candidateTargetIds = reflectedTargets;
                }
            }
        } catch (Throwable ignored) {
            // Fallback to conservative crosshair-target snapshot.
        }

        return new BetterCombatAttackSnapshot(comboIndex, offHand, animationId, upswingRatio, attackRange,
                candidateTargetIds);
    }

    private static int readComboIndex(Minecraft minecraft) throws Exception {
        Method method = minecraft.getClass().getMethod("getComboCount");
        Object value = method.invoke(minecraft);
        return value instanceof Integer i ? i : 0;
    }

    private static AttackHandSnapshot readCurrentAttackHand(LocalPlayer player, int comboIndex) throws Exception {
        Class<?> helperClass = Class.forName("net.bettercombat.logic.PlayerAttackHelper");
        Method method = null;
        for (Method candidate : helperClass.getMethods()) {
            if (candidate.getName().equals("getCurrentAttack") && candidate.getParameterCount() == 2) {
                method = candidate;
                break;
            }
        }
        if (method == null) {
            return null;
        }

        Object attackHand = method.invoke(null, player, comboIndex);
        if (attackHand == null) {
            return null;
        }

        boolean offHand = invokeBooleanNoArg(attackHand, "isOffHand", false);
        double upswingRatio = invokeDoubleNoArg(attackHand, "upswingRate", 0.0D);
        Object attack = invokeNoArg(attackHand, "attack");
        Object attributes = invokeNoArg(attackHand, "attributes");
        String animationId = attack != null ? invokeStringNoArg(attack, "animation", "") : "";
        double attackRange = attributes != null ? invokeDoubleNoArg(attributes, "attackRange", 3.0D) : 3.0D;

        return new AttackHandSnapshot(attackHand, attack, attributes, offHand, animationId, upswingRatio, attackRange);
    }

    private static List<Integer> readCandidateTargets(Minecraft minecraft, LocalPlayer player, AttackHandSnapshot hand)
            throws Exception {
        if (hand.attack() == null || hand.attributes() == null) {
            return List.of();
        }

        Class<?> targetFinderClass = Class.forName("net.bettercombat.client.collision.TargetFinder");
        Method method = null;
        for (Method candidate : targetFinderClass.getMethods()) {
            if (candidate.getName().equals("findAttackTargets") && candidate.getParameterCount() == 4) {
                method = candidate;
                break;
            }
        }
        if (method == null) {
            return List.of();
        }

        Object result = method.invoke(null, player, currentHitResult(minecraft), hand.attack(), hand.attackRange());
        if (!(result instanceof List<?> list)) {
            return List.of();
        }

        List<Integer> ids = new ArrayList<>();
        for (Object value : list) {
            if (value instanceof Entity entity) {
                ids.add(entity.getId());
            }
        }
        return ids;
    }

    private static HitResult currentHitResult(Minecraft minecraft) {
        return minecraft.hitResult;
    }

    private static Object invokeNoArg(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private static boolean invokeBooleanNoArg(Object target, String methodName, boolean fallback) {
        try {
            Object value = invokeNoArg(target, methodName);
            return value instanceof Boolean b ? b : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double invokeDoubleNoArg(Object target, String methodName, double fallback) {
        try {
            Object value = invokeNoArg(target, methodName);
            return value instanceof Number number ? number.doubleValue() : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String invokeStringNoArg(Object target, String methodName, String fallback) {
        try {
            Object value = invokeNoArg(target, methodName);
            return value == null ? fallback : value.toString();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private record AttackHandSnapshot(
            Object rawHand,
            Object attack,
            Object attributes,
            boolean offHand,
            String animationId,
            double upswingRatio,
            double attackRange) {
    }
}
