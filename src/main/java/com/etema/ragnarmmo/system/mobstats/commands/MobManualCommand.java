package com.etema.ragnarmmo.system.mobstats.commands;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.InternalManualMobEntry;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobRegistryService;
import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobBackendResolver;
import com.etema.ragnarmmo.common.config.access.MobStatsConfigAccess;
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
                .requires(stack -> stack.hasPermission(2) && (MobStatsConfigAccess.isManualMobEditorEnabled() || MobStatsConfigAccess.isManualMobDiscoveryEnabled()))
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
                .then(Commands.literal("enable")
                        .then(Commands.argument("entity_type", StringArgumentType.word())
                                .executes(ctx -> setEnabled(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "entity_type"),
                                        true))))
                .then(Commands.literal("disable")
                        .then(Commands.argument("entity_type", StringArgumentType.word())
                                .executes(ctx -> setEnabled(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "entity_type"),
                                        false))))
                .then(Commands.literal("edit")
                        .then(Commands.argument("entity_type", StringArgumentType.word())
                                .then(Commands.argument("field", StringArgumentType.word())
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                                .executes(ctx -> editField(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "entity_type"),
                                                        StringArgumentType.getString(ctx, "field"),
                                                        StringArgumentType.getString(ctx, "value")))))))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("entity_type", StringArgumentType.word())
                                .executes(ctx -> inspect(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "entity_type")))));
    }

    private static boolean ensureDiscoveryEnabled(CommandSourceStack source) {
        if (!MobStatsConfigAccess.isManualMobDiscoveryEnabled()) {
            source.sendFailure(Component.literal("Manual mob discovery is disabled by config."));
            return false;
        }
        return true;
    }

    private static boolean ensureEditorEnabled(CommandSourceStack source) {
        if (!MobStatsConfigAccess.isManualMobEditorEnabled()) {
            source.sendFailure(Component.literal("Manual mob editor is disabled by config."));
            return false;
        }
        return true;
    }

    private static int listEntries(CommandSourceStack source) {
        if (!ensureDiscoveryEnabled(source)) {
            return 0;
        }
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
        if (!ensureEditorEnabled(source)) {
            return 0;
        }
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
        if (!ensureEditorEnabled(source)) {
            return 0;
        }
        ResourceLocation id = parseId(idText, source);
        if (id == null) {
            return 0;
        }
        boolean removed = ManualMobRegistryService.remove(source.getServer(), id);
        source.sendSuccess(() -> Component.literal((removed ? "Removed " : "Not found: ") + id), false);
        return removed ? 1 : 0;
    }

    private static int setLevel(CommandSourceStack source, String idText, int level) {
        if (!ensureEditorEnabled(source)) {
            return 0;
        }
        ResourceLocation id = parseId(idText, source);
        if (id == null) {
            return 0;
        }
        InternalManualMobEntry current = ManualMobRegistryService.createOrGetStub(source.getServer(), id, source.getTextName());
        InternalManualMobEntry updated = copy(current, source.getTextName(), builder -> builder.level = level);
        ManualMobRegistryService.upsert(source.getServer(), updated);
        source.sendSuccess(() -> Component.literal("Updated level for " + id + " to " + level), false);
        return 1;
    }

    private static int setRank(CommandSourceStack source, String idText, String rankText) {
        if (!ensureEditorEnabled(source)) {
            return 0;
        }
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
        InternalManualMobEntry updated = copy(current, source.getTextName(), builder -> builder.rank = rank);
        ManualMobRegistryService.upsert(source.getServer(), updated);
        source.sendSuccess(() -> Component.literal("Updated rank for " + id + " to " + rank.name().toLowerCase(Locale.ROOT)), false);
        return 1;
    }

    private static int setEnabled(CommandSourceStack source, String idText, boolean enabled) {
        if (!ensureEditorEnabled(source)) {
            return 0;
        }
        ResourceLocation id = parseId(idText, source);
        if (id == null) {
            return 0;
        }
        InternalManualMobEntry current = ManualMobRegistryService.createOrGetStub(source.getServer(), id, source.getTextName());
        InternalManualMobEntry updated = copy(current, source.getTextName(), builder -> builder.enabled = enabled);
        ManualMobRegistryService.upsert(source.getServer(), updated);
        source.sendSuccess(() -> Component.literal((enabled ? "Enabled " : "Disabled ") + id), false);
        return 1;
    }

    private static int editField(CommandSourceStack source, String idText, String field, String value) {
        if (!ensureEditorEnabled(source)) {
            return 0;
        }
        ResourceLocation id = parseId(idText, source);
        if (id == null) {
            return 0;
        }
        InternalManualMobEntry current = ManualMobRegistryService.createOrGetStub(source.getServer(), id, source.getTextName());
        String normalized = field.trim().toLowerCase(Locale.ROOT);
        try {
            InternalManualMobEntry updated = copy(current, source.getTextName(), builder -> applyFieldUpdate(builder, normalized, value));
            ManualMobRegistryService.upsert(source.getServer(), updated);
            source.sendSuccess(() -> Component.literal("Updated " + normalized + " for " + id + " to " + value), false);
            return 1;
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal("Edit failed: " + ex.getMessage()));
            return 0;
        }
    }

    private static int inspect(CommandSourceStack source, String idText) {
        if (!ensureDiscoveryEnabled(source)) {
            return 0;
        }
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
        if (internal.isPresent()) {
            InternalManualMobEntry entry = internal.get();
            source.sendSuccess(() -> Component.literal(String.format(
                    Locale.ROOT,
                    "Internal -> enabled=%s level=%d rank=%s race=%s element=%s size=%s",
                    entry.enabled() ? "yes" : "no",
                    entry.level(),
                    entry.rank().name().toLowerCase(Locale.ROOT),
                    entry.race(),
                    entry.element(),
                    entry.size())), false);
            source.sendSuccess(() -> Component.literal(String.format(
                    Locale.ROOT,
                    "Stats -> hp=%d atk=%d-%d def=%d mdef=%d hit=%d flee=%d crit=%d aspd=%d move=%.3f",
                    entry.maxHp(),
                    entry.atkMin(),
                    entry.atkMax(),
                    entry.def(),
                    entry.mdef(),
                    entry.hit(),
                    entry.flee(),
                    entry.crit(),
                    entry.aspd(),
                    entry.moveSpeed())), false);
        }
        return 1;
    }

    private static InternalManualMobEntry copy(
            InternalManualMobEntry source,
            String editorName,
            java.util.function.Consumer<EntryBuilder> mutator) {
        EntryBuilder builder = new EntryBuilder(source);
        mutator.accept(builder);
        return builder.build(editorName);
    }

    private static void applyFieldUpdate(EntryBuilder b, String field, String value) {
        switch (field) {
            case "level" -> b.level = Integer.parseInt(value);
            case "rank" -> b.rank = MobRank.valueOf(value.trim().toUpperCase(Locale.ROOT));
            case "race" -> b.race = value;
            case "element" -> b.element = value;
            case "size" -> b.size = value;
            case "maxhp", "max_hp" -> b.maxHp = Integer.parseInt(value);
            case "atkmin", "atk_min" -> b.atkMin = Integer.parseInt(value);
            case "atkmax", "atk_max" -> b.atkMax = Integer.parseInt(value);
            case "def" -> b.def = Integer.parseInt(value);
            case "mdef" -> b.mdef = Integer.parseInt(value);
            case "hit" -> b.hit = Integer.parseInt(value);
            case "flee" -> b.flee = Integer.parseInt(value);
            case "crit" -> b.crit = Integer.parseInt(value);
            case "aspd" -> b.aspd = Integer.parseInt(value);
            case "movespeed", "move_speed" -> b.moveSpeed = Double.parseDouble(value);
            case "notes" -> b.notes = value;
            case "enabled" -> b.enabled = Boolean.parseBoolean(value);
            default -> throw new IllegalArgumentException("Unsupported field: " + field);
        }
    }

    private static final class EntryBuilder {
        private final ResourceLocation entityTypeId;
        private boolean enabled;
        private int level;
        private MobRank rank;
        private String race;
        private String element;
        private String size;
        private int maxHp;
        private int atkMin;
        private int atkMax;
        private int def;
        private int mdef;
        private int hit;
        private int flee;
        private int crit;
        private int aspd;
        private double moveSpeed;
        private String notes;

        private EntryBuilder(InternalManualMobEntry source) {
            this.entityTypeId = source.entityTypeId();
            this.enabled = source.enabled();
            this.level = source.level();
            this.rank = source.rank();
            this.race = source.race();
            this.element = source.element();
            this.size = source.size();
            this.maxHp = source.maxHp();
            this.atkMin = source.atkMin();
            this.atkMax = source.atkMax();
            this.def = source.def();
            this.mdef = source.mdef();
            this.hit = source.hit();
            this.flee = source.flee();
            this.crit = source.crit();
            this.aspd = source.aspd();
            this.moveSpeed = source.moveSpeed();
            this.notes = source.notes();
        }

        private InternalManualMobEntry build(String editorName) {
            return new InternalManualMobEntry(
                    entityTypeId,
                    enabled,
                    level,
                    rank,
                    race,
                    element,
                    size,
                    maxHp,
                    atkMin,
                    atkMax,
                    def,
                    mdef,
                    hit,
                    flee,
                    crit,
                    aspd,
                    moveSpeed,
                    notes,
                    editorName,
                    System.currentTimeMillis());
        }
    }

    private static ResourceLocation parseId(String idText, CommandSourceStack source) {
        ResourceLocation id = ResourceLocation.tryParse(idText);
        if (id == null) {
            source.sendFailure(Component.literal("Invalid entity id: " + idText));
        }
        return id;
    }
}
