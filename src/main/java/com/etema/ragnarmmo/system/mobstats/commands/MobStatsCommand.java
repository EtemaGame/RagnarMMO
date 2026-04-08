package com.etema.ragnarmmo.system.mobstats.commands;

import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.mobstats.config.MobConfig;
import com.etema.ragnarmmo.system.mobstats.core.MobStatDistributor;
import com.etema.ragnarmmo.system.mobstats.core.MobStats;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;
import com.etema.ragnarmmo.system.mobstats.level.MobLevelManager;
import com.etema.ragnarmmo.system.mobstats.level.MobLevelManager.DifficultyReport;
import com.etema.ragnarmmo.system.mobstats.network.SyncMobStatsPacket;
import com.etema.ragnarmmo.system.mobstats.util.MobAttributeHelper;
import com.etema.ragnarmmo.system.mobstats.world.ActiveBossesSavedData;
import com.etema.ragnarmmo.system.mobstats.world.BossSpawnMetadata;
import com.etema.ragnarmmo.system.mobstats.world.BossSpawnService;
import com.etema.ragnarmmo.system.mobstats.world.BossSpawnSource;
import com.etema.ragnarmmo.system.mobstats.world.BossSigilItem;
import com.etema.ragnarmmo.system.mobstats.world.MobSpawnOverrides;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.Locale;
import java.util.Random;

/**
 * Debug commands for mob stat inspection and boss lifecycle tuning.
 */
public final class MobStatsCommand {

    private static final int MAX_LIST_LINES = 8;

    private MobStatsCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createNode() {
        return Commands.literal("mobstats")
                .requires(stack -> stack.hasPermission(2))
                .executes(ctx -> showStats(ctx.getSource(), ctx.getSource().getEntityOrException()))
                .then(Commands.literal("difficulty")
                        .executes(ctx -> showWorldDifficulty(ctx.getSource())))
                .then(Commands.literal("boss")
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(ctx -> inspectManualBoss(
                                                ctx.getSource(),
                                                EntityArgument.getEntity(ctx, "target")))))
                        .then(Commands.literal("mark")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .then(Commands.argument("min_level", IntegerArgumentType.integer(1))
                                                .executes(ctx -> markManualBoss(
                                                        ctx.getSource(),
                                                        EntityArgument.getEntity(ctx, "target"),
                                                        IntegerArgumentType.getInteger(ctx, "min_level"))))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(ctx -> clearManualBoss(
                                                ctx.getSource(),
                                                EntityArgument.getEntity(ctx, "target"))))))
                .then(Commands.argument("target", EntityArgument.entity())
                        .executes(ctx -> showStats(ctx.getSource(), EntityArgument.getEntity(ctx, "target"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createBossSigilNode() {
        return Commands.literal("sigil")
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("entity_type", StringArgumentType.word())
                                        .then(Commands.argument("tier", StringArgumentType.word())
                                                .then(Commands.argument("spawn_key", StringArgumentType.word())
                                                        .then(Commands.argument(
                                                                "respawn_seconds",
                                                                IntegerArgumentType.integer(0))
                                                                .executes(ctx -> giveBossSigil(
                                                                        ctx.getSource(),
                                                                        EntityArgument.getPlayer(ctx, "player"),
                                                                        StringArgumentType.getString(
                                                                                ctx,
                                                                                "entity_type"),
                                                                        StringArgumentType.getString(ctx, "tier"),
                                                                        StringArgumentType.getString(
                                                                                ctx,
                                                                                "spawn_key"),
                                                                        IntegerArgumentType.getInteger(
                                                                                ctx,
                                                                                "respawn_seconds")))))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createBossSpawnNode() {
        return Commands.literal("spawn")
                .then(Commands.argument("entity_type", StringArgumentType.word())
                        .then(Commands.argument("tier", StringArgumentType.word())
                                .then(Commands.argument("source", StringArgumentType.word())
                                        .then(Commands.argument("spawn_key", StringArgumentType.word())
                                                .then(Commands.argument(
                                                        "respawn_seconds",
                                                        IntegerArgumentType.integer(0))
                                                        .executes(ctx -> spawnControlledBoss(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "entity_type"),
                                                                StringArgumentType.getString(ctx, "tier"),
                                                                StringArgumentType.getString(ctx, "source"),
                                                                StringArgumentType.getString(ctx, "spawn_key"),
                                                                IntegerArgumentType.getInteger(
                                                                        ctx,
                                                                        "respawn_seconds"))))))));
    }

    private static int showStats(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("The selected target is not a valid mob."));
            return 0;
        }

        MobStats stats = MobStatsProvider.get(living).orElse(null);
        if (stats == null || !stats.isInitialized()) {
            source.sendSuccess(() -> Component.literal("The mob has no registered stats."), false);
            return 0;
        }

        net.minecraft.resources.ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        Component header = Component.literal(String.format(Locale.ROOT,
                "Mob %s - Level %d (%s)",
                id != null ? id.toString() : living.getDisplayName().getString(),
                stats.getLevel(),
                stats.getTier().name().toLowerCase(Locale.ROOT)));
        source.sendSuccess(() -> header, false);

        source.sendSuccess(() -> Component.literal("Stats: " + stats.describeStats()), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Multipliers -> HP %.2f | DMG %.2f | DEF %.2f | SPD %.2f",
                stats.getHealthMultiplier(),
                stats.getDamageMultiplier(),
                stats.getDefenseMultiplier(),
                stats.getSpeedMultiplier())), false);

        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Total points: %d", stats.getTotalPoints())), false);
        boolean manualBoss = MobSpawnOverrides.isManualBoss(living);
        int minimumLevel = MobSpawnOverrides.getMinimumLevel(living).orElse(1);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Manual boss: %s | minimum level: %d",
                manualBoss ? "yes" : "no",
                minimumLevel)), false);
        return 1;
    }

    private static int inspectManualBoss(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("The selected target is not a living entity."));
            return 0;
        }

        boolean manualBoss = MobSpawnOverrides.isManualBoss(living);
        int minimumLevel = MobSpawnOverrides.getMinimumLevel(living).orElse(1);
        MobTier tier = MobStatsProvider.get(living)
                .resolve()
                .map(MobStats::getTier)
                .orElse(MobTier.NORMAL);

        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Boss inspect -> %s manual=%s tier=%s minLevel=%d",
                living.getName().getString(),
                manualBoss ? "yes" : "no",
                tier.name(),
                minimumLevel)).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int markManualBoss(CommandSourceStack source, Entity entity, int minLevel) {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("The selected target is not a living entity."));
            return 0;
        }

        MobSpawnOverrides.setManualBoss(living, true);
        MobSpawnOverrides.setMinimumLevel(living, minLevel);

        MobStats stats = MobStatsProvider.get(living).orElse(null);
        if (stats != null) {
            promoteToElite(stats);
            applyMinimumLevel(living, stats, minLevel);
            MobAttributeHelper.applyAttributes(living, stats);
            Network.sendTrackingEntityAndSelf(living, new SyncMobStatsPacket(living.getId(), stats));
        }

        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Marked %s as manual boss with minimum level %d.",
                living.getName().getString(),
                minLevel)).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int clearManualBoss(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("The selected target is not a living entity."));
            return 0;
        }

        MobSpawnOverrides.clearManualBoss(living);

        MobStats stats = MobStatsProvider.get(living).orElse(null);
        if (stats != null) {
            Network.sendTrackingEntityAndSelf(living, new SyncMobStatsPacket(living.getId(), stats));
        }

        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Cleared manual boss override on %s.",
                living.getName().getString())).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static void promoteToElite(MobStats stats) {
        MobTier previousTier = stats.getTier();
        if (previousTier == MobTier.ELITE) {
            return;
        }

        double previousHealth = Math.max(0.0001D, MobConfig.healthMultiplier(previousTier));
        double previousDamage = Math.max(0.0001D, MobConfig.damageMultiplier(previousTier));
        double previousDefense = Math.max(0.0001D, MobConfig.defenseMultiplier(previousTier));

        stats.setHealthMultiplier(stats.getHealthMultiplier() * (MobConfig.healthMultiplier(MobTier.ELITE) / previousHealth));
        stats.setDamageMultiplier(stats.getDamageMultiplier() * (MobConfig.damageMultiplier(MobTier.ELITE) / previousDamage));
        stats.setDefenseMultiplier(stats.getDefenseMultiplier() * (MobConfig.defenseMultiplier(MobTier.ELITE) / previousDefense));
        stats.setTier(MobTier.ELITE);
    }

    private static void applyMinimumLevel(LivingEntity living, MobStats stats, int minLevel) {
        if (stats.getLevel() >= minLevel) {
            return;
        }

        int currentTotal = stats.getTotalPoints();
        int requiredTotal = Math.max(currentTotal, MobConfig.basePoints(stats.getTier()) + minLevel * MobConfig.pointsPerLevel(stats.getTier()));
        int extraPoints = Math.max(0, requiredTotal - currentTotal);

        stats.setLevel(minLevel);
        if (extraPoints > 0) {
            new MobStatDistributor(new Random()).distributeByWeights(stats, extraPoints, stats.getMobClass().getWeights());
        }
    }

    private static int showWorldDifficulty(CommandSourceStack source) {
        ServerLevel serverLevel = source.getLevel();

        MobLevelManager manager = new MobLevelManager(new Random());
        DifficultyReport report = manager.getDifficultyReport(serverLevel);

        source.sendSuccess(() -> Component.literal("=== World Difficulty ===").withStyle(ChatFormatting.GOLD), false);
        report.lines().forEach(line -> source.sendSuccess(() -> Component.literal(line), false));
        return 1;
    }

    private static int showBossWorldState(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }

        ActiveBossesSavedData data = ActiveBossesSavedData.get(source.getServer());
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Boss world state -> active=%d cooldowns=%d",
                data.getActiveBosses().size(),
                data.getRespawnCooldowns().size())).withStyle(ChatFormatting.GOLD), false);

        data.getActiveBosses().stream()
                .sorted(Comparator.comparing(ActiveBossesSavedData.BossEntry::displayName))
                .limit(MAX_LIST_LINES)
                .forEach(entry -> source.sendSuccess(() -> Component.literal(String.format(
                        Locale.ROOT,
                        "ACTIVE %s [%s] @ %s (%d %d %d)%s",
                        entry.displayName(),
                        entry.tier().name(),
                        entry.dimensionId(),
                        entry.x(),
                        entry.y(),
                        entry.z(),
                        entry.hasSpawnKey() ? " key=" + entry.spawnKey() : "")).withStyle(ChatFormatting.RED), false));

        data.getRespawnCooldowns().stream()
                .sorted(Comparator.comparing(ActiveBossesSavedData.RespawnEntry::spawnKey))
                .limit(MAX_LIST_LINES)
                .forEach(entry -> source.sendSuccess(() -> Component.literal(String.format(
                        Locale.ROOT,
                        "COOLDOWN %s [%s] ready=%s next=%d",
                        entry.spawnKey(),
                        entry.tier().name(),
                        entry.isReady(source.getLevel().getGameTime()) ? "yes" : "no",
                        entry.nextAllowedGameTime())).withStyle(ChatFormatting.AQUA), false));

        return 1;
    }

    private static int inspectBossTarget(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("The selected target is not a living entity."));
            return 0;
        }

        MobTier tier = MobStatsProvider.get(living)
                .resolve()
                .map(MobStats::getTier)
                .orElse(MobTier.NORMAL);
        BossSpawnMetadata.SpawnInfo info = BossSpawnMetadata.read(living, tier);

        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Boss inspect -> %s tier=%s source=%s key=%s respawnTicks=%d",
                living.getName().getString(),
                tier.name(),
                info.source().name(),
                info.spawnKey().isBlank() ? "<none>" : info.spawnKey(),
                info.respawnDelayTicks())).withStyle(ChatFormatting.GOLD), false);

        if (source.getServer() != null && !info.spawnKey().isBlank()) {
            ActiveBossesSavedData data = ActiveBossesSavedData.get(source.getServer());
            data.getRespawnEntry(info.spawnKey()).ifPresentOrElse(
                    entry -> source.sendSuccess(() -> Component.literal(String.format(
                            Locale.ROOT,
                            "Cooldown -> ready=%s next=%d lastDefeated=%d",
                            entry.isReady(source.getLevel().getGameTime()) ? "yes" : "no",
                            entry.nextAllowedGameTime(),
                            entry.lastDefeatedGameTime())).withStyle(ChatFormatting.AQUA), false),
                    () -> source.sendSuccess(() -> Component.literal("Cooldown -> none").withStyle(ChatFormatting.GRAY),
                            false));
        }

        return 1;
    }

    private static int markNatural(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("The selected target is not a living entity."));
            return 0;
        }

        BossSpawnMetadata.markNatural(living);
        refreshBossState(living);
        source.sendSuccess(
                () -> Component.literal("Marked boss spawn metadata as NATURAL.").withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    private static int markControlled(
            CommandSourceStack source,
            Entity entity,
            String sourceName,
            String spawnKey,
            int respawnSeconds) {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("The selected target is not a living entity."));
            return 0;
        }

        BossSpawnSource spawnSource = BossSpawnSource.parse(sourceName).orElse(null);
        if (spawnSource == null) {
            source.sendFailure(Component.literal("Invalid source. Use: natural, structure, altar, event, summon, debug."));
            return 0;
        }
        if (spawnSource == BossSpawnSource.NATURAL) {
            source.sendFailure(Component.literal("Use mark_natural for natural bosses."));
            return 0;
        }

        int respawnTicks = Math.max(0, respawnSeconds * 20);
        BossSpawnMetadata.markControlled(living, spawnSource, spawnKey, respawnTicks);
        refreshBossState(living);
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Marked controlled boss source=%s key=%s respawn=%ss",
                spawnSource.name(),
                spawnKey.trim().toLowerCase(Locale.ROOT),
                respawnSeconds)).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int clearMetadata(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("The selected target is not a living entity."));
            return 0;
        }

        BossSpawnMetadata.clear(living);
        refreshBossState(living);
        source.sendSuccess(() -> Component.literal("Cleared boss spawn metadata.").withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    private static int spawnControlledBoss(
            CommandSourceStack source,
            String entityTypeId,
            String tierName,
            String sourceName,
            String spawnKey,
            int respawnSeconds) {
        ServerLevel level = source.getLevel();
        ResourceLocation id = ResourceLocation.tryParse(entityTypeId);
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            source.sendFailure(Component.literal("Unknown entity type: " + entityTypeId));
            return 0;
        }

        MobTier tier;
        try {
            tier = MobTier.valueOf(tierName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Invalid tier. Use: mini_boss, boss or mvp."));
            return 0;
        }

        BossSpawnSource spawnSource = BossSpawnSource.parse(sourceName).orElse(null);
        if (spawnSource == null || !spawnSource.isControlled()) {
            source.sendFailure(Component.literal("Invalid source. Use: structure, altar, event, summon or debug."));
            return 0;
        }

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(id);
        BlockPos pos = BlockPos.containing(source.getPosition());
        BossSpawnService.SpawnResult result = BossSpawnService.spawnControlledBoss(
                level,
                pos,
                entityType,
                tier,
                spawnSource,
                spawnKey,
                respawnSeconds);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Spawned %s as %s with key=%s and respawn=%ss",
                id,
                tier.name(),
                result.spawnKey(),
                respawnSeconds)).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int giveBossSigil(
            CommandSourceStack source,
            ServerPlayer player,
            String entityTypeId,
            String tierName,
            String spawnKey,
            int respawnSeconds) {
        ResourceLocation id = ResourceLocation.tryParse(entityTypeId);
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            source.sendFailure(Component.literal("Unknown entity type: " + entityTypeId));
            return 0;
        }

        MobTier tier;
        try {
            tier = MobTier.valueOf(tierName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Invalid tier. Use: mini_boss, boss or mvp."));
            return 0;
        }

        if (!tier.shouldPersistWorldState()) {
            source.sendFailure(Component.literal("Boss sigils require MINI_BOSS, BOSS or MVP tier."));
            return 0;
        }

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(id);
        ItemStack stack = BossSigilItem.createConfiguredStack(entityType, tier, spawnKey, respawnSeconds);
        boolean added = player.getInventory().add(stack);
        if (!added) {
            player.drop(stack, false);
        }
        player.inventoryMenu.broadcastChanges();

        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Gave boss sigil to %s -> %s %s key=%s respawn=%ss",
                player.getName().getString(),
                tier.name(),
                id,
                spawnKey.trim().toLowerCase(Locale.ROOT),
                respawnSeconds)).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int showCooldown(CommandSourceStack source, String spawnKey) {
        if (source.getServer() == null) {
            return 0;
        }

        String normalizedKey = spawnKey.trim().toLowerCase(Locale.ROOT);
        ActiveBossesSavedData data = ActiveBossesSavedData.get(source.getServer());
        boolean active = data.isSpawnKeyActive(normalizedKey);
        data.getRespawnEntry(normalizedKey).ifPresentOrElse(
                entry -> source.sendSuccess(() -> Component.literal(String.format(
                        Locale.ROOT,
                        "Cooldown %s -> active=%s ready=%s next=%d lastDefeated=%d",
                        normalizedKey,
                        active ? "yes" : "no",
                        entry.isReady(source.getLevel().getGameTime()) ? "yes" : "no",
                        entry.nextAllowedGameTime(),
                        entry.lastDefeatedGameTime())).withStyle(ChatFormatting.AQUA), false),
                () -> source.sendSuccess(() -> Component.literal(String.format(
                        Locale.ROOT,
                        "Cooldown %s -> active=%s entry=<none>",
                        normalizedKey,
                        active ? "yes" : "no")).withStyle(ChatFormatting.GRAY), false));
        return 1;
    }

    private static int clearCooldown(CommandSourceStack source, String spawnKey) {
        if (source.getServer() == null) {
            return 0;
        }

        String normalizedKey = spawnKey.trim().toLowerCase(Locale.ROOT);
        boolean removed = ActiveBossesSavedData.get(source.getServer()).removeRespawnCooldown(normalizedKey);
        if (!removed) {
            source.sendFailure(Component.literal("No cooldown entry found for " + normalizedKey + "."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("Cleared cooldown for " + normalizedKey + ".").withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    private static void refreshBossState(LivingEntity living) {
        if (!(living.level() instanceof ServerLevel serverLevel) || serverLevel.getServer() == null) {
            return;
        }

        MobStatsProvider.get(living)
                .resolve()
                .map(MobStats::getTier)
                .filter(MobTier::shouldPersistWorldState)
                .ifPresent(tier -> ActiveBossesSavedData.get(serverLevel.getServer())
                        .registerBoss(serverLevel, living, tier));
    }
}
