package com.etema.ragnarmmo.system.mobstats.commands;

import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadView;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadViewResolver;
import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerInspectionStatsView;
import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
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
import com.etema.ragnarmmo.system.mobstats.world.BossTierResolver;
import com.etema.ragnarmmo.system.mobstats.world.MobSpawnOverrides;
import com.etema.ragnarmmo.system.mobstats.world.read.MobWorldStateActiveEntryReadView;
import com.etema.ragnarmmo.system.mobstats.world.read.MobWorldStateCooldownEntryReadView;
import com.etema.ragnarmmo.system.mobstats.world.read.MobWorldStateEncounterReadView;
import com.etema.ragnarmmo.system.mobstats.world.read.MobWorldStateEntityReadView;
import com.etema.ragnarmmo.system.mobstats.world.read.MobWorldStateReadResolver;
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
                        .then(Commands.literal("worldstate")
                                .executes(ctx -> showBossWorldState(ctx.getSource())))
                        .then(Commands.literal("inspect_target")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(ctx -> inspectBossTarget(
                                                ctx.getSource(),
                                                EntityArgument.getEntity(ctx, "target")))))
                        .then(Commands.literal("cooldown")
                                .then(Commands.argument("spawn_key", StringArgumentType.word())
                                        .executes(ctx -> showCooldown(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "spawn_key")))))
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

        MobConsumerReadView readView = MobConsumerReadViewResolver
                .resolve(living)
                .orElse(null);
        if (readView == null) {
            source.sendSuccess(() -> Component.literal("The mob has no normalized mob data available."), false);
            return 0;
        }

        ResourceLocation id = readView.entityTypeId();
        Component header = Component.literal(String.format(Locale.ROOT,
                "Mob %s - Level %d (%s)",
                id != null ? id.toString() : living.getDisplayName().getString(),
                readView.level(),
                readView.rank().name().toLowerCase(Locale.ROOT)));
        source.sendSuccess(() -> header, false);

        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Read source: %s | new runtime profile: %s",
                readView.dataOrigin().name().toLowerCase(Locale.ROOT),
                readView.hasNewRuntimeProfile() ? "yes" : "no")), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Taxonomy -> race=%s | element=%s | size=%s",
                readView.race(),
                readView.element(),
                readView.size())), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Compatibility -> bossLike=%s",
                readView.isBossLikeForCompatibility() ? "yes" : "no")), false);
        sendInspectionStats(source, readView);
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

        MobConsumerReadView readView = MobConsumerReadViewResolver
                .resolve(living)
                .orElse(null);
        if (readView == null) {
            source.sendFailure(Component.literal("The selected mob has no normalized read view available."));
            return 0;
        }

        boolean manualBoss = MobSpawnOverrides.isManualBoss(living);
        int minimumLevel = MobSpawnOverrides.getMinimumLevel(living).orElse(1);

        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Boss inspect -> %s manual=%s rank=%s minLevel=%d source=%s",
                living.getName().getString(),
                manualBoss ? "yes" : "no",
                readView.rank().name().toLowerCase(Locale.ROOT),
                minimumLevel,
                readView.dataOrigin().name().toLowerCase(Locale.ROOT))).withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Taxonomy -> race=%s | element=%s | size=%s | bossLike=%s",
                readView.race(),
                readView.element(),
                readView.size(),
                readView.isBossLikeForCompatibility() ? "yes" : "no")), false);
        sendInspectionStats(source, readView);
        return 1;
    }

    private static void sendInspectionStats(CommandSourceStack source, MobConsumerReadView readView) {
        if (readView.inspectionStats() == null) {
            return;
        }

        MobConsumerInspectionStatsView stats = readView.inspectionStats();
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Final stats -> maxHp=%d atk=%d-%d def=%d mdef=%d",
                stats.maxHp(),
                stats.atkMin(),
                stats.atkMax(),
                stats.def(),
                stats.mdef())), false);
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
            SyncMobStatsPacket.fromEntity(living)
                    .ifPresent(packet -> Network.sendTrackingEntityAndSelf(living, packet));
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
            SyncMobStatsPacket.fromEntity(living)
                    .ifPresent(packet -> Network.sendTrackingEntityAndSelf(living, packet));
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

        double previousHealth = Math.max(0.0001D, MobStatsConfigAccess.getHealthMultiplier(previousTier));
        double previousDamage = Math.max(0.0001D, MobStatsConfigAccess.getDamageMultiplier(previousTier));
        double previousDefense = Math.max(0.0001D, MobStatsConfigAccess.getDefenseMultiplier(previousTier));

        stats.setHealthMultiplier(stats.getHealthMultiplier() * (MobStatsConfigAccess.getHealthMultiplier(MobTier.ELITE) / previousHealth));
        stats.setDamageMultiplier(stats.getDamageMultiplier() * (MobStatsConfigAccess.getDamageMultiplier(MobTier.ELITE) / previousDamage));
        stats.setDefenseMultiplier(stats.getDefenseMultiplier() * (MobStatsConfigAccess.getDefenseMultiplier(MobTier.ELITE) / previousDefense));
        stats.setTier(MobTier.ELITE);
    }

    private static void applyMinimumLevel(LivingEntity living, MobStats stats, int minLevel) {
        if (stats.getLevel() >= minLevel) {
            return;
        }

        int currentTotal = stats.getTotalPoints();
        int requiredTotal = Math.max(currentTotal, MobStatsConfigAccess.getBasePoints(stats.getTier()) + minLevel * MobStatsConfigAccess.getPointsPerLevel(stats.getTier()));
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

        var server = source.getServer();
        var activeEntries = MobWorldStateReadResolver.listActiveEntries(server);
        var cooldownEntries = MobWorldStateReadResolver.listCooldownEntries(server);
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Boss world state -> active=%d cooldowns=%d",
                activeEntries.size(),
                cooldownEntries.size())).withStyle(ChatFormatting.GOLD), false);

        activeEntries.stream()
                .limit(MAX_LIST_LINES)
                .forEach(entry -> {
                    String encounterKey = entry.encounterKey() != null ? entry.encounterKey() : "<none>";
                    String spawnSource = entry.spawnSource() != null
                            ? entry.spawnSource().name().toLowerCase(Locale.ROOT)
                            : "<none>";
                    String respawnDelay = entry.respawnDelayTicks() != null
                            ? String.valueOf(entry.respawnDelayTicks())
                            : "<none>";
                    String lastSeen = formatOptionalLong(entry.lastSeenGameTime());
                    String nextAllowed = formatOptionalLong(entry.nextAllowedGameTime());
                    String lastDefeated = formatOptionalLong(entry.lastDefeatedGameTime());
                    source.sendSuccess(() -> Component.literal(String.format(
                            Locale.ROOT,
                            "ACTIVE %s [%s] @ %s (%d %d %d) registered=%s key=%s source=%s respawnTicks=%s lastSeen=%s cooldownPresent=%s ready=%s next=%s lastDefeated=%s",
                            entry.displayName(),
                            entry.entityTypeId(),
                            entry.dimensionId(),
                            entry.x(),
                            entry.y(),
                            entry.z(),
                            entry.activeRegistrationPresent() ? "yes" : "no",
                            encounterKey,
                            spawnSource,
                            respawnDelay,
                            lastSeen,
                            entry.cooldownPresent() ? "yes" : "no",
                            entry.cooldownReady() ? "yes" : "no",
                            nextAllowed,
                            lastDefeated)).withStyle(ChatFormatting.RED), false);
                });

        cooldownEntries.stream()
                .limit(MAX_LIST_LINES)
                .forEach(entry -> {
                    String nextAllowed = formatOptionalLong(entry.nextAllowedGameTime());
                    String lastDefeated = formatOptionalLong(entry.lastDefeatedGameTime());
                    source.sendSuccess(() -> Component.literal(String.format(
                            Locale.ROOT,
                            "COOLDOWN %s (%s @ %s) active=%s present=%s ready=%s next=%s lastDefeated=%s",
                            entry.encounterKey(),
                            entry.displayName(),
                            entry.dimensionId(),
                            entry.activeRegistrationPresent() ? "yes" : "no",
                            entry.cooldownPresent() ? "yes" : "no",
                            entry.cooldownReady() ? "yes" : "no",
                            nextAllowed,
                            lastDefeated)).withStyle(ChatFormatting.AQUA), false);
                });

        return 1;
    }

    private static int inspectBossTarget(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("The selected target is not a living entity."));
            return 0;
        }

        MobConsumerReadView readView = MobConsumerReadViewResolver
                .resolve(living)
                .orElse(null);
        if (readView == null) {
            source.sendFailure(Component.literal("The selected boss has no normalized read view available."));
            return 0;
        }

        MobWorldStateEntityReadView entityWorldState = MobWorldStateReadResolver.resolveEntity(living).orElse(null);

        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Boss inspect -> %s rank=%s",
                living.getName().getString(),
                readView.rank().name().toLowerCase(Locale.ROOT))).withStyle(ChatFormatting.GOLD), false);

        if (entityWorldState == null) {
            source.sendSuccess(
                    () -> Component.literal("World-state -> entity=<unresolved>").withStyle(ChatFormatting.GRAY),
                    false);
            return 1;
        }

        String encounterKey = entityWorldState.encounterKey() != null ? entityWorldState.encounterKey() : "<none>";
        String spawnSource = entityWorldState.spawnSource() != null
                ? entityWorldState.spawnSource().name().toLowerCase(Locale.ROOT)
                : "<none>";
        String respawnDelay = entityWorldState.respawnDelayTicks() != null
                ? String.valueOf(entityWorldState.respawnDelayTicks())
                : "<none>";
        String entityTypeId = entityWorldState.entityTypeId() != null ? entityWorldState.entityTypeId() : "<none>";
        String lastSeen = formatOptionalLong(entityWorldState.lastSeenGameTime());

        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "World-state -> type=%s source=%s key=%s respawnTicks=%s lastSeen=%s",
                entityTypeId,
                spawnSource,
                encounterKey,
                respawnDelay,
                lastSeen)), false);

        if (entityWorldState.encounterKey() == null) {
            source.sendSuccess(() -> Component.literal("Cooldown -> key=<none>").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        String nextAllowed = formatOptionalLong(entityWorldState.nextAllowedGameTime());
        String lastDefeated = formatOptionalLong(entityWorldState.lastDefeatedGameTime());
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Cooldown -> present=%s ready=%s next=%s lastDefeated=%s",
                entityWorldState.cooldownPresent() ? "yes" : "no",
                entityWorldState.cooldownReady() ? "yes" : "no",
                nextAllowed,
                lastDefeated)).withStyle(entityWorldState.cooldownPresent() ? ChatFormatting.AQUA : ChatFormatting.GRAY), false);
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
        MobWorldStateCooldownEntryReadView worldState = MobWorldStateReadResolver
                .resolveCooldownEntry(source.getServer(), normalizedKey)
                .orElse(null);
        if (worldState == null) {
            source.sendSuccess(() -> Component.literal(String.format(
                    Locale.ROOT,
                    "Cooldown %s -> worldState=<unresolved>",
                    normalizedKey.isBlank() ? "<blank>" : normalizedKey)).withStyle(ChatFormatting.GRAY), false);
            return 0;
        }

        String nextAllowed = formatOptionalLong(worldState.nextAllowedGameTime());
        String lastDefeated = formatOptionalLong(worldState.lastDefeatedGameTime());
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Cooldown %s (%s [%s] @ %s) -> active=%s present=%s ready=%s next=%s lastDefeated=%s",
                worldState.encounterKey(),
                worldState.displayName(),
                worldState.entityTypeId(),
                worldState.dimensionId(),
                worldState.activeRegistrationPresent() ? "yes" : "no",
                worldState.cooldownPresent() ? "yes" : "no",
                worldState.cooldownReady() ? "yes" : "no",
                nextAllowed,
                lastDefeated)).withStyle(worldState.cooldownPresent() ? ChatFormatting.AQUA : ChatFormatting.GRAY), false);
        return 1;
    }

    private static String formatOptionalLong(Long value) {
        return value != null ? String.valueOf(value) : "<none>";
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

        BossTierResolver.resolveTier(living)
                .filter(MobTier::shouldPersistWorldState)
                .ifPresent(tier -> ActiveBossesSavedData.get(serverLevel.getServer())
                        .registerBoss(serverLevel, living, tier));
    }
}
