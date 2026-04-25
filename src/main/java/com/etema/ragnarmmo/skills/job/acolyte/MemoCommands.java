package com.etema.ragnarmmo.skills.job.acolyte;

import com.etema.ragnarmmo.common.command.CommandUtil;
import com.etema.ragnarmmo.skills.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.skills.runtime.SkillManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class MemoCommands {
    private static final ResourceLocation WARP_PORTAL_SKILL = new ResourceLocation("ragnarmmo", "warp_portal");

    private MemoCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createNode() {
        return Commands.literal("memo")
                .executes(MemoCommands::showMemoList)
                .then(Commands.literal("list").executes(MemoCommands::showMemoList))
                .then(Commands.literal("select")
                        .then(Commands.literal("save").executes(ctx -> selectDestination(ctx, 0)))
                        .then(Commands.argument("slot",
                                IntegerArgumentType.integer(1, SkillManager.getMaxWarpMemos()))
                                .executes(ctx -> selectDestination(ctx,
                                        IntegerArgumentType.getInteger(ctx, "slot")))))
                .then(Commands.argument("slot", IntegerArgumentType.integer(1, SkillManager.getMaxWarpMemos()))
                        .executes(MemoCommands::saveMemo));
    }

    private static int showMemoList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = CommandUtil.requirePlayer(ctx);
        SkillManager skills = requireWarpPortalSkill(ctx.getSource(), player);
        if (skills == null) {
            return 0;
        }

        int selected = skills.getSelectedWarpDestination();
        CommandUtil.sendOk(ctx.getSource(), Component.literal("§5Warp Portal Destinations"));
        CommandUtil.sendOk(ctx.getSource(), withSelectionPrefix(selected == 0).append(WarpPortalHelper.describeSavePoint(player)));

        for (int slot = 1; slot <= SkillManager.getMaxWarpMemos(); slot++) {
            final int memoSlot = slot;
            Component line = skills.getWarpMemo(slot)
                    .<Component>map(memo -> WarpPortalHelper.describeMemo(memoSlot, memo))
                    .orElseGet(() -> Component.literal("Memo " + memoSlot + " §8→ §7vacío"));
            CommandUtil.sendOk(ctx.getSource(), withSelectionPrefix(selected == slot).append(line));
        }
        return 1;
    }

    private static int saveMemo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = CommandUtil.requirePlayer(ctx);
        SkillManager skills = requireWarpPortalSkill(ctx.getSource(), player);
        if (skills == null) {
            return 0;
        }

        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        skills.setWarpMemo(slot, player.level().dimension().location(), player.blockPosition());
        syncSkills(player, skills);

        CommandUtil.sendOk(ctx.getSource(), Component.literal(
                "§dMemo " + slot + " §fguardado en §7" + player.level().dimension().location() + " §f("
                        + player.blockPosition().getX() + ", " + player.blockPosition().getY() + ", "
                        + player.blockPosition().getZ() + ")"));
        return 1;
    }

    private static int selectDestination(CommandContext<CommandSourceStack> ctx, int selection)
            throws CommandSyntaxException {
        ServerPlayer player = CommandUtil.requirePlayer(ctx);
        SkillManager skills = requireWarpPortalSkill(ctx.getSource(), player);
        if (skills == null) {
            return 0;
        }

        if (selection > 0 && skills.getWarpMemo(selection).isEmpty()) {
            CommandUtil.sendFail(ctx.getSource(), Component.literal("Memo " + selection + " está vacío.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        skills.setSelectedWarpDestination(selection);
        syncSkills(player, skills);

        Component selectedLine = selection == 0
                ? WarpPortalHelper.describeSavePoint(player)
                : skills.getWarpMemo(selection)
                        .map(memo -> WarpPortalHelper.describeMemo(selection, memo))
                        .orElse(Component.literal("Memo " + selection));

        CommandUtil.sendOk(ctx.getSource(),
                Component.literal("§aDestino activo: ").append(selectedLine));
        return 1;
    }

    private static SkillManager requireWarpPortalSkill(CommandSourceStack source, ServerPlayer player) {
        SkillManager skills = PlayerSkillsProvider.get(player).resolve().orElse(null);
        if (skills == null) {
            CommandUtil.sendFail(source, Component.literal("No se encontraron los datos de habilidades.")
                    .withStyle(ChatFormatting.RED));
            return null;
        }

        if (skills.getSkillLevel(WARP_PORTAL_SKILL) <= 0) {
            CommandUtil.sendFail(source, Component.literal("Necesitas aprender Warp Portal para usar /memo.")
                    .withStyle(ChatFormatting.RED));
            return null;
        }

        return skills;
    }

    private static void syncSkills(ServerPlayer player, SkillManager skills) {
        com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                new com.etema.ragnarmmo.player.stats.network.ClientboundSkillSyncPacket(skills.serializeNBT()));
    }

    private static MutableComponent withSelectionPrefix(boolean selected) {
        return Component.literal(selected ? "§a[Activo] §r" : "§8[     ] §r");
    }
}
