package com.etema.ragnarmmo.player.command;

import com.etema.ragnarmmo.common.api.stats.StatKeys;
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
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class StatKeyArgumentType implements ArgumentType<StatKeys> {
    private static final DynamicCommandExceptionType UNKNOWN_STAT = new DynamicCommandExceptionType(
            input -> Component.translatable("commands.ragnarmmo.admin.stats.unknown", input));

    private static final StatKeyArgumentType INSTANCE = new StatKeyArgumentType();

    private StatKeyArgumentType() {
    }

    public static StatKeyArgumentType stat() {
        return INSTANCE;
    }

    public static StatKeys getStat(CommandContext<?> context, String name) {
        return context.getArgument(name, StatKeys.class);
    }

    @Override
    public StatKeys parse(StringReader reader) throws CommandSyntaxException {
        String input = reader.readUnquotedString().toLowerCase(Locale.ROOT);
        return StatKeys.fromId(input).orElseThrow(() -> UNKNOWN_STAT.create(input));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Stream.of(StatKeys.values()).map(StatKeys::id), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.asList("str", "agi", "vit");
    }
}

