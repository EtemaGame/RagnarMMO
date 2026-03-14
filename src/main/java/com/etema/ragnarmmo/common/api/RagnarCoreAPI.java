package com.etema.ragnarmmo.common.api;

import com.etema.ragnarmmo.common.api.compute.DerivedStats;
import com.etema.ragnarmmo.common.api.events.StatComputeEvent;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class RagnarCoreAPI {
    @FunctionalInterface
    public interface StatsAccessor extends Function<Player, Optional<IPlayerStats>> {
    }

    @FunctionalInterface
    public interface ComputeFunction {
        DerivedStats compute(Player player, IPlayerStats stats,
                             double weaponATK, double weaponAps,
                             double spellBase, double armorEfficiency,
                             double baseCastTime);
    }

    private static final StatsAccessor DEFAULT_ACCESSOR = player -> Optional.empty();
    private static final AtomicReference<StatsAccessor> ACCESSOR = new AtomicReference<>(DEFAULT_ACCESSOR);
    private static final List<ComputeFunction> COMPUTE_FUNCTIONS = new CopyOnWriteArrayList<>();

    private RagnarCoreAPI() {
    }

    public static void registerAccessor(StatsAccessor statsAccessor) {
        ACCESSOR.set(statsAccessor != null ? statsAccessor : DEFAULT_ACCESSOR);
    }

    public static Optional<IPlayerStats> get(Player player) {
        return ACCESSOR.get().apply(player);
    }

    public static boolean hasStatsAccessor() {
        return ACCESSOR.get() != DEFAULT_ACCESSOR;
    }

    public static void registerComputeFunction(ComputeFunction function) {
        if (function != null) {
            COMPUTE_FUNCTIONS.add(function);
        }
    }

    public static boolean hasComputeFunction() {
        return !COMPUTE_FUNCTIONS.isEmpty();
    }

    public static Optional<DerivedStats> computeDerivedStats(Player player, IPlayerStats stats,
                                                             double weaponATK, double weaponAps,
                                                             double spellBase, double armorEfficiency,
                                                             double baseCastTime) {
        if (player == null || stats == null || COMPUTE_FUNCTIONS.isEmpty()) {
            return Optional.empty();
        }

        DerivedStats derived = null;
        for (ComputeFunction function : COMPUTE_FUNCTIONS) {
            derived = function.compute(player, stats, weaponATK, weaponAps,
                    spellBase, armorEfficiency, baseCastTime);
            if (derived != null) {
                break;
            }
        }

        if (derived == null) {
            return Optional.empty();
        }

        MinecraftForge.EVENT_BUS.post(new StatComputeEvent(player, stats, derived));
        return Optional.of(derived);
    }
}






