package com.etema.ragnarmmo.common.debug;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.config.RagnarConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class RagnarDebugLog {

    private static final Map<RagnarDebugChannel, Boolean> OVERRIDES = new EnumMap<>(RagnarDebugChannel.class);

    private RagnarDebugLog() {
    }

    public static synchronized void enable(RagnarDebugChannel channel) {
        if (channel == null) {
            return;
        }
        if (channel == RagnarDebugChannel.MASTER) {
            OVERRIDES.put(RagnarDebugChannel.MASTER, Boolean.TRUE);
            return;
        }
        OVERRIDES.put(RagnarDebugChannel.MASTER, Boolean.TRUE);
        OVERRIDES.put(channel, Boolean.TRUE);
    }

    public static synchronized void disable(RagnarDebugChannel channel) {
        if (channel == null) {
            return;
        }
        OVERRIDES.put(channel, Boolean.FALSE);
    }

    public static synchronized void reset(RagnarDebugChannel channel) {
        if (channel == null) {
            return;
        }
        OVERRIDES.remove(channel);
    }

    public static synchronized void enableAll() {
        OVERRIDES.put(RagnarDebugChannel.MASTER, Boolean.TRUE);
        OVERRIDES.put(RagnarDebugChannel.COMBAT, Boolean.TRUE);
        OVERRIDES.put(RagnarDebugChannel.PLAYER_DATA, Boolean.TRUE);
        OVERRIDES.put(RagnarDebugChannel.MOB_SPAWNS, Boolean.TRUE);
        OVERRIDES.put(RagnarDebugChannel.BOSS_WORLD, Boolean.TRUE);
        OVERRIDES.put(RagnarDebugChannel.MIGRATION, Boolean.TRUE);
    }

    public static synchronized void disableAll() {
        OVERRIDES.put(RagnarDebugChannel.MASTER, Boolean.FALSE);
        OVERRIDES.put(RagnarDebugChannel.COMBAT, Boolean.FALSE);
        OVERRIDES.put(RagnarDebugChannel.PLAYER_DATA, Boolean.FALSE);
        OVERRIDES.put(RagnarDebugChannel.MOB_SPAWNS, Boolean.FALSE);
        OVERRIDES.put(RagnarDebugChannel.BOSS_WORLD, Boolean.FALSE);
        OVERRIDES.put(RagnarDebugChannel.MIGRATION, Boolean.FALSE);
    }

    public static synchronized void resetAll() {
        OVERRIDES.clear();
    }

    public static boolean combatEnabled() {
        return masterEnabled() && resolve(RagnarDebugChannel.COMBAT, RagnarConfigs.SERVER.logging.debugCombat.get());
    }

    public static boolean playerDataEnabled() {
        return masterEnabled() && resolve(RagnarDebugChannel.PLAYER_DATA, RagnarConfigs.SERVER.logging.debugPlayerData.get());
    }

    public static boolean mobSpawnsEnabled() {
        return masterEnabled() && resolve(RagnarDebugChannel.MOB_SPAWNS, RagnarConfigs.SERVER.logging.debugMobSpawns.get());
    }

    public static boolean bossWorldEnabled() {
        return masterEnabled() && resolve(RagnarDebugChannel.BOSS_WORLD, RagnarConfigs.SERVER.logging.debugBossWorld.get());
    }

    public static boolean migrationEnabled() {
        return masterEnabled() && resolve(RagnarDebugChannel.MIGRATION, RagnarConfigs.SERVER.logging.debugMigration.get());
    }

    public static void combat(String message, Object... args) {
        log(combatEnabled(), "COMBAT", message, args);
    }

    public static void playerData(String message, Object... args) {
        log(playerDataEnabled(), "PLAYER", message, args);
    }

    public static void mobSpawns(String message, Object... args) {
        log(mobSpawnsEnabled(), "MOB", message, args);
    }

    public static void bossWorld(String message, Object... args) {
        log(bossWorldEnabled(), "BOSS", message, args);
    }

    public static void migration(String message, Object... args) {
        log(migrationEnabled(), "MIGRATION", message, args);
    }

    public static String entityLabel(Entity entity) {
        if (entity == null) {
            return "null";
        }

        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String typeId = key == null ? "unknown" : key.toString();
        String name = entity.getName().getString();
        return name + "{" + typeId + "#" + entity.getId() + "}";
    }

    public static String livingState(LivingEntity entity) {
        if (entity == null) {
            return "null";
        }
        return entityLabel(entity) + " hp=" + formatDouble(entity.getHealth()) + "/" + formatDouble(entity.getMaxHealth());
    }

    public static String blockPos(BlockPos pos) {
        if (pos == null) {
            return "(null)";
        }
        return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }

    public static String percent(double value) {
        return String.format(Locale.ROOT, "%.2f%%", value * 100.0D);
    }

    public static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static String describeStatus() {
        return describeChannel(RagnarDebugChannel.MASTER, masterEnabled(), getOverride(RagnarDebugChannel.MASTER))
                + ", "
                + describeChannel(RagnarDebugChannel.COMBAT, combatEnabled(), getOverride(RagnarDebugChannel.COMBAT))
                + ", "
                + describeChannel(RagnarDebugChannel.PLAYER_DATA, playerDataEnabled(),
                        getOverride(RagnarDebugChannel.PLAYER_DATA))
                + ", "
                + describeChannel(RagnarDebugChannel.MOB_SPAWNS, mobSpawnsEnabled(),
                        getOverride(RagnarDebugChannel.MOB_SPAWNS))
                + ", "
                + describeChannel(RagnarDebugChannel.BOSS_WORLD, bossWorldEnabled(),
                        getOverride(RagnarDebugChannel.BOSS_WORLD))
                + ", "
                + describeChannel(RagnarDebugChannel.MIGRATION, migrationEnabled(),
                        getOverride(RagnarDebugChannel.MIGRATION));
    }

    private static void log(boolean enabled, String category, String message, Object... args) {
        if (!enabled) {
            return;
        }

        Object[] fullArgs = new Object[args.length + 1];
        fullArgs[0] = category;
        System.arraycopy(args, 0, fullArgs, 1, args.length);
        RagnarMMO.LOGGER.info("[RO-DEBUG][{}] " + message, fullArgs);
    }

    private static boolean masterEnabled() {
        return resolve(RagnarDebugChannel.MASTER, RagnarConfigs.SERVER.logging.debug.get());
    }

    private static synchronized Boolean getOverride(RagnarDebugChannel channel) {
        return OVERRIDES.get(channel);
    }

    private static synchronized boolean resolve(RagnarDebugChannel channel, boolean configValue) {
        Boolean override = OVERRIDES.get(channel);
        return override != null ? override.booleanValue() : configValue;
    }

    private static String describeChannel(RagnarDebugChannel channel, boolean effective, Boolean override) {
        String source = override == null ? "config" : "runtime";
        return channel.commandName() + "=" + effective + "(" + source + ")";
    }
}
