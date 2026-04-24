package com.etema.ragnarmmo.skill.runtime;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Brigadier argument type for skills.
 */
public class SkillArgumentType implements ArgumentType<net.minecraft.resources.ResourceLocation> {

    private static final DynamicCommandExceptionType UNKNOWN_SKILL = new DynamicCommandExceptionType(
            input -> Component.literal("Unknown skill: " + input));

    private static final SkillArgumentType INSTANCE = new SkillArgumentType();

    private SkillArgumentType() {
    }

    public static SkillArgumentType skill() {
        return INSTANCE;
    }

    public static net.minecraft.resources.ResourceLocation getSkill(CommandContext<?> context, String name) {
        return context.getArgument(name, net.minecraft.resources.ResourceLocation.class);
    }

    @Override
    public net.minecraft.resources.ResourceLocation parse(StringReader reader) throws CommandSyntaxException {
        String input = reader.readUnquotedString();
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(input);

        if (id == null) {
            throw UNKNOWN_SKILL.create(input);
        }

        // Validate against registry
        if (!com.etema.ragnarmmo.skill.data.SkillRegistry.contains(id)) {
            throw UNKNOWN_SKILL.create(id);
        }

        return id;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                com.etema.ragnarmmo.skill.data.SkillRegistry.getAllIds().stream()
                        .map(net.minecraft.resources.ResourceLocation::toString),
                builder);
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.asList("ragnarmmo:bash", "ragnarmmo:mining");
    }
}
