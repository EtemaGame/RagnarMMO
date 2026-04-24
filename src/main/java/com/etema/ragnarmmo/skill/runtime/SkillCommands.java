package com.etema.ragnarmmo.skill.runtime;

import com.etema.ragnarmmo.skill.api.IPlayerSkills;
import com.etema.ragnarmmo.common.command.CommandUtil;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player-facing commands for the skill system.
 * - /ragnar skills
 * - /ragnar skills <skill>
 */
public final class SkillCommands {
        private SkillCommands() {
        }

        public static LiteralArgumentBuilder<CommandSourceStack> createNode() {
                return Commands.literal("skills")
                                .executes(SkillCommands::showAllSelf)
                                .then(Commands.argument("skill", SkillArgumentType.skill())
                                                .executes(ctx -> showSkillSelf(ctx,
                                                                SkillArgumentType.getSkill(ctx, "skill"))));
        }

        private static int showAllSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
                ServerPlayer player = CommandUtil.requirePlayer(ctx);
                return CommandUtil.getSkills(player).map(skills -> showAllSkills(ctx.getSource(), skills))
                                .orElseGet(() -> {
                                        CommandUtil.sendFail(ctx.getSource(),
                                                        Component.translatable(
                                                                        "commands.ragnarmmo.player.missing_skills")
                                                                        .withStyle(ChatFormatting.RED));
                                        return 0;
                                });
        }

        private static int showSkillSelf(CommandContext<CommandSourceStack> ctx,
                        net.minecraft.resources.ResourceLocation skillId)
                        throws CommandSyntaxException {
                ServerPlayer player = CommandUtil.requirePlayer(ctx);
                return CommandUtil.getSkills(player).map(skills -> showSkillInfo(ctx.getSource(), skills, skillId))
                                .orElseGet(() -> {
                                        CommandUtil.sendFail(ctx.getSource(),
                                                        Component.translatable(
                                                                        "commands.ragnarmmo.player.missing_skills")
                                                                        .withStyle(ChatFormatting.RED));
                                        return 0;
                                });
        }

        private static int showAllSkills(CommandSourceStack source, IPlayerSkills skills) {
                CommandUtil.sendOk(source, Component.translatable("commands.ragnarmmo.skills.header")
                                .withStyle(ChatFormatting.GOLD));

                for (com.etema.ragnarmmo.skill.api.SkillCategory category : com.etema.ragnarmmo.skill.api.SkillCategory
                                .values()) {

                        // Filter skills by category using Registry
                        java.util.List<net.minecraft.resources.ResourceLocation> categorySkills = com.etema.ragnarmmo.skill.data.SkillRegistry
                                        .getAllIds().stream()
                                        .filter(id -> com.etema.ragnarmmo.skill.data.SkillRegistry.get(id)
                                                        .map(def -> def.getCategory() == category).orElse(false))
                                        .sorted()
                                        .collect(java.util.stream.Collectors.toList());

                        if (categorySkills.isEmpty())
                                continue;

                        CommandUtil.sendOk(source, Component.translatable("commands.ragnarmmo.skills.category",
                                        category.getDisplayName()).withStyle(ChatFormatting.YELLOW));

                        for (net.minecraft.resources.ResourceLocation id : categorySkills) {
                                int level = skills.getSkillLevel(id);
                                int percent = 0;
                                if (skills instanceof SkillManager sm) {
                                        com.etema.ragnarmmo.skill.data.progression.SkillState state = sm.getSkillState(id);
                                        if (state != null) {
                                                percent = (int) (state.getProgressPercent() * 100);
                                        }
                                }

                                String displayName = com.etema.ragnarmmo.skill.data.SkillRegistry.get(id)
                                                .map(com.etema.ragnarmmo.skill.api.ISkillDefinition::getDisplayName)
                                                .orElse(id.getPath());

                                CommandUtil.sendOk(source, Component.translatable("commands.ragnarmmo.skills.line",
                                                displayName, level, percent).withStyle(ChatFormatting.GRAY));
                        }
                }

                CommandUtil.sendOk(source,
                                Component.translatable("commands.ragnarmmo.skills.total", skills.getTotalLevel())
                                                .withStyle(ChatFormatting.GRAY));
                return 1;
        }

        private static int showSkillInfo(CommandSourceStack source, IPlayerSkills skills,
                        net.minecraft.resources.ResourceLocation skillId) {

                // Check if skill exists
                var defOpt = com.etema.ragnarmmo.skill.data.SkillRegistry.get(skillId);
                if (defOpt.isEmpty()) {
                        CommandUtil.sendFail(source,
                                        Component.literal("Unknown skill: " + skillId).withStyle(ChatFormatting.RED));
                        return 0;
                }
                var def = defOpt.get();

                int level = skills.getSkillLevel(skillId);
                int maxLevel = def.getMaxLevel();
                double xp = skills.getSkillXp(skillId);

                // Calculate next level XP using SkillState static helper
                double nextXp = com.etema.ragnarmmo.skill.data.progression.SkillState.calculateXpToLevel(level + 1);
                int percent = (int) ((xp / nextXp) * 100);
                if (level >= maxLevel)
                        percent = 100;

                CommandUtil.sendOk(source, Component.translatable("commands.ragnarmmo.skills.info.header",
                                def.getTranslatedName()).withStyle(ChatFormatting.GOLD));
                CommandUtil.sendOk(source, Component.translatable("commands.ragnarmmo.skills.info.category",
                                def.getCategory().getDisplayName()).withStyle(ChatFormatting.GRAY));
                CommandUtil.sendOk(source, Component.translatable("commands.ragnarmmo.skills.info.level",
                                level, maxLevel).withStyle(ChatFormatting.GRAY));
                CommandUtil.sendOk(source, Component.translatable("commands.ragnarmmo.skills.info.xp",
                                (int) xp, (int) nextXp).withStyle(ChatFormatting.GRAY));
                CommandUtil.sendOk(source, Component.translatable("commands.ragnarmmo.skills.info.progress",
                                percent).withStyle(ChatFormatting.GRAY));
                return 1;
        }
}
