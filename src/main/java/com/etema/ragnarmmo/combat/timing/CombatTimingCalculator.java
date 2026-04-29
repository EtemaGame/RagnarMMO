package com.etema.ragnarmmo.combat.timing;

import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.player.stats.compute.CombatMath;
import com.etema.ragnarmmo.skills.data.SkillDefinition;

import net.minecraft.server.level.ServerPlayer;

/**
 * Resolves RO timing terms for the server-authoritative combat actor state.
 */
public final class CombatTimingCalculator {
    private CombatTimingCalculator() {
    }

    public static TimingProfile resolveSkillTiming(ServerPlayer player, SkillDefinition definition, int level) {
        int variableCast = definition.getVariableCastTicks(level);
        int fixedCast = definition.getFixedCastTicks(level);
        int adjustedVariable = adjustVariableCastTicks(player, variableCast);
        return new TimingProfile(
                adjustedVariable,
                Math.max(0, fixedCast),
                Math.max(0, definition.getAfterCastDelayTicks(level)),
                Math.max(0, definition.getGlobalDelayTicks(level)),
                Math.max(0, definition.getCooldownTicks(level)));
    }

    public static int adjustVariableCastTicks(ServerPlayer player, int variableCastTicks) {
        if (player == null || variableCastTicks <= 0) {
            return 0;
        }
        int dex = (int) Math.round(StatAttributes.getTotal(player, StatKeys.DEX));
        int intel = (int) Math.round(StatAttributes.getTotal(player, StatKeys.INT));
        double seconds = variableCastTicks / 20.0D;
        return Math.max(0, (int) Math.round(CombatMath.computeCastTime(seconds, dex, intel, false) * 20.0D));
    }

    public record TimingProfile(
            int variableCastTicks,
            int fixedCastTicks,
            int afterCastDelayTicks,
            int globalDelayTicks,
            int cooldownTicks) {
        public int totalCastTicks() {
            return Math.max(0, variableCastTicks) + Math.max(0, fixedCastTicks);
        }
    }
}
