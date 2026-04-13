package com.etema.ragnarmmo.system.mobstats.commands;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.InternalManualMobEntry;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobRegistryService;
import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobBackendResolver;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.Comparator;
import java.util.Locale;

public final class MobManualCommand {

    private MobManualCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createNode() {
        return Commands.literal("mobmanual")
                .requires(stack -> stack.hasPermission(2))
                .then(Commands.literal("list")
                        .executes(ctx -> listEntries(ctx.getSource())))
                .then(Commands.literal("create")
                        .then(Commands.argument("entity_type", StringArgumentType.word())
                                .executes(ctx -> createStub(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "entity_type")))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("entity_type", StringArgumentType.word())
                                .executes(ctx -> remove(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "entity_type")))))
                .then(Commands.literal("setlevel")
                        .then(Commands.argument("entity_type", StringArgumentType.word())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                        .executes(ctx -> setLevel(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "entity_type"),
                                                IntegerArgumentType.getInteger(ctx, "level"))))))
                .then(Commands.literal("setrank")
                        .then(Commands.argument("entity_type", StringArgumentType.word())
                                .then(Commands.argument("rank", StringArgumentType.word())
                                        .executes(ctx -> setRank(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "entity_type"),
                                                StringArgumentType.getString(ctx, "rank"))))))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("entity_type", StringArgumentType.word())
                                .executes(ctx -> inspect(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "entity_type")))));
    }

    private static int listEntries(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        var entries = ManualMobRegistryService.list(server).stream()
                .sorted(Comparator.comparing(e -> e.entityTypeId().toString()))
                .toList();
        source.sendSuccess(() -> Component.literal("Internal manual entries: " + entries.size()), false);
        for (InternalManualMobEntry entry : entries.stream().limit(20).toList()) {
            source.sendSuccess(() -> Component.literal(String.format(
                    Locale.ROOT,
                    "- %s lvl=%d rank=%s enabled=%s",
                    entry.entityTypeId(),
                    entry.level(),
                    entry.rank().name().toLowerCase(Locale.ROOT),
                    entry.enabled() ? "yes" : "no")), false);
        }
        return 1;
    }

    private static int createStub(CommandSourceStack source, String idText) {
        ResourceLocation id = parseId(idText, source);
        if (id == null) {
            return 0;
        }
        InternalManualMobEntry entry = ManualMobRegistryService.createOrGetStub(
                source.getServer(),
                id,
                source.getTextName());
        source.sendSuccess(() -> Component.literal("Created/loaded internal stub for " + entry.entityTypeId()), false);
        return 1;
    }

    private static int remove(CommandSourceStack source, String idText) {
        ResourceLocation id = parseId(idText, source);
        if (id == null) {
            return 0;
        }
        boolean removed = ManualMobRegistryService.remove(source.getServer(), id);
        source.sendSuccess(() -> Component.literal((removed ? "Removed " : "Not found: ") + id), false);
        return removed ? 1 : 0;
    }

    private static int setLevel(CommandSourceStack source, String idText, int level) {
        ResourceLocation id = parseId(idText, source);
        if (id == null) {
            return 0;
        }
        InternalManualMobEntry current = ManualMobRegistryService.createOrGetStub(source.getServer(), id, source.getTextName());
        InternalManualMobEntry updated = new InternalManualMobEntry(
                current.entityTypeId(),
                current.enabled(),
                level,
                current.rank(),
                current.race(),
                current.element(),
                current.size(),
                current.maxHp(),
                current.atkMin(),
                current.atkMax(),
                current.def(),
                current.mdef(),
                current.hit(),
                current.flee(),
                current.crit(),
                current.aspd(),
                current.moveSpeed(),
                current.notes(),
                source.getTextName(),
                System.currentTimeMillis());
        ManualMobRegistryService.upsert(source.getServer(), updated);
        source.sendSuccess(() -> Component.literal("Updated level for " + id + " to " + level), false);
        return 1;
    }

    private static int setRank(CommandSourceStack source, String idText, String rankText) {
        ResourceLocation id = parseId(idText, source);
        if (id == null) {
            return 0;
        }
        MobRank rank;
        try {
            rank = MobRank.valueOf(rankText.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Invalid rank. Allowed: normal, elite, boss"));
            return 0;
        }

        InternalManualMobEntry current = ManualMobRegistryService.createOrGetStub(source.getServer(), id, source.getTextName());
        InternalManualMobEntry updated = new InternalManualMobEntry(
                current.entityTypeId(),
                current.enabled(),
                current.level(),
                rank,
                current.race(),
                current.element(),
                current.size(),
                current.maxHp(),
                current.atkMin(),
                current.atkMax(),
                current.def(),
                current.mdef(),
                current.hit(),
                current.flee(),
                current.crit(),
                current.aspd(),
                current.moveSpeed(),
                current.notes(),
                source.getTextName(),
                System.currentTimeMillis());
        ManualMobRegistryService.upsert(source.getServer(), updated);
        source.sendSuccess(() -> Component.literal("Updated rank for " + id + " to " + rank.name().toLowerCase(Locale.ROOT)), false);
        return 1;
    }

    private static int inspect(CommandSourceStack source, String idText) {
        ResourceLocation id = parseId(idText, source);
        if (id == null) {
            return 0;
        }
        var internal = ManualMobRegistryService.find(source.getServer(), id);
        var resolution = ManualMobBackendResolver.resolve(id, source.getServer(), com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess.getManualMobBackend());
        source.sendSuccess(() -> Component.literal("Coverage: " + resolution.coverage().covered()
                + " backend=" + resolution.coverage().backend()
                + " reason=" + resolution.coverage().reason()), false);
        source.sendSuccess(() -> Component.literal("Internal entry present: " + (internal.isPresent() ? "yes" : "no")), false);
        return 1;
    }

    private static ResourceLocation parseId(String idText, CommandSourceStack source) {
        ResourceLocation id = ResourceLocation.tryParse(idText);
        if (id == null) {
            source.sendFailure(Component.literal("Invalid entity id: " + idText));
        }
        return id;
    }
}
