package com.etema.ragnarmmo.system.mobstats.core;

import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.system.mobstats.config.SpeciesConfig;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MobStatDistributor {
    private final Random rng;

    public MobStatDistributor(Random rng) {
        this.rng = rng;
    }

    public void distribute(MobStats stats, int totalPoints, SpeciesConfig.SpeciesSettings settings) {
        stats.resetStats();
        if (totalPoints <= 0)
            return;

        if (settings == null || settings.randomDistribution() || settings.statWeights().isEmpty()) {
            distributeRandom(stats, totalPoints);
            return;
        }

        Map<StatKeys, Integer> weights = new EnumMap<>(StatKeys.class);
        int weightSum = 0;
        for (Map.Entry<StatKeys, Integer> e : settings.statWeights().entrySet()) {
            int v = Math.max(0, e.getValue());
            if (v > 0) {
                weights.put(e.getKey(), v);
                weightSum += v;
            }
        }

        if (weights.isEmpty() || weightSum <= 0) {
            distributeRandom(stats, totalPoints);
            return;
        }

        int allocated = 0;
        for (Map.Entry<StatKeys, Integer> e : weights.entrySet()) {
            int base = (int) Math.floor((double) totalPoints * e.getValue() / weightSum);
            if (base > 0) {
                stats.set(e.getKey(), base);
                allocated += base;
            }
        }
        int remaining = totalPoints - allocated;
        if (remaining > 0) {
            distributeByWeights(stats, remaining, weights);
        }
    }

    // NUEVO: distribución por pesos base + overrides string->stat
    public void distributeWithWeights(MobStats stats, int totalPoints,
            Map<StatKeys, Integer> baseWeights,
            Map<String, Double> overrides) {
        stats.resetStats();
        if (totalPoints <= 0)
            return;

        Map<StatKeys, Integer> w = new EnumMap<>(StatKeys.class);
        int sum = 0;
        for (StatKeys k : StatKeys.values()) {
            int base = baseWeights.getOrDefault(k, 1);
            int ov = (int) Math.round(overrides.getOrDefault(k.name().toLowerCase(), (double) base));
            int val = Math.max(0, ov);
            if (val > 0) {
                w.put(k, val);
                sum += val;
            }
        }
        if (w.isEmpty() || sum <= 0) {
            distributeRandom(stats, totalPoints);
            return;
        }

        int allocated = 0;
        for (Map.Entry<StatKeys, Integer> e : w.entrySet()) {
            int base = (int) Math.floor((double) totalPoints * e.getValue() / sum);
            if (base > 0) {
                stats.set(e.getKey(), base);
                allocated += base;
            }
        }
        int remaining = totalPoints - allocated;
        if (remaining > 0) {
            distributeByWeights(stats, remaining, w);
        }
    }

    private void distributeByWeights(MobStats stats, int remaining, Map<StatKeys, Integer> weights) {
        if (weights.isEmpty()) {
            distributeRandom(stats, remaining);
            return;
        }
        int weightSum = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (weightSum <= 0) {
            distributeRandom(stats, remaining);
            return;
        }

        List<Map.Entry<StatKeys, Integer>> entries = new ArrayList<>(weights.entrySet());
        for (int i = 0; i < remaining; i++) {
            int roll = rng.nextInt(weightSum) + 1;
            int acc = 0;
            for (Map.Entry<StatKeys, Integer> e : entries) {
                acc += e.getValue();
                if (roll <= acc) {
                    stats.add(e.getKey(), 1);
                    break;
                }
            }
        }
    }

    private void distributeRandom(MobStats stats, int totalPoints) {
        StatKeys[] keys = StatKeys.values();
        for (int i = 0; i < totalPoints; i++) {
            StatKeys k = keys[rng.nextInt(keys.length)];
            stats.add(k, 1);
        }
    }
}






