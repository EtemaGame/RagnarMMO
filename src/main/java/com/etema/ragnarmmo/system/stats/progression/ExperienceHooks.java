package com.etema.ragnarmmo.system.stats.progression;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntUnaryOperator;

/**
 * Simple registry that allows other mods or datapacks to plug custom experience
 * curves into the Ragnar progression system. A string identifier is looked up
 * from the configuration file and, when present, replaces the built-in
 * exponential curves.
 */
public final class ExperienceHooks {
    private static final Map<String, IntUnaryOperator> EXP_CURVES = new ConcurrentHashMap<>();
    private static final Map<String, IntUnaryOperator> JOB_EXP_CURVES = new ConcurrentHashMap<>();

    private ExperienceHooks() {}

    public static void registerExpCurve(String id, IntUnaryOperator function) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(function, "function");
        EXP_CURVES.put(id, function);
    }

    public static void unregisterExpCurve(String id) {
        if (id != null) {
            EXP_CURVES.remove(id);
        }
    }

    public static IntUnaryOperator getExpCurve(String id) {
        return id == null ? null : EXP_CURVES.get(id);
    }

    public static void registerJobExpCurve(String id, IntUnaryOperator function) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(function, "function");
        JOB_EXP_CURVES.put(id, function);
    }

    public static void unregisterJobExpCurve(String id) {
        if (id != null) {
            JOB_EXP_CURVES.remove(id);
        }
    }

    public static IntUnaryOperator getJobExpCurve(String id) {
        return id == null ? null : JOB_EXP_CURVES.get(id);
    }
}






