package com.etema.ragnarmmo.common.command;

import com.etema.ragnarmmo.common.api.jobs.JobType;
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

public final class JobArgumentType implements ArgumentType<JobType> {
    private static final DynamicCommandExceptionType UNKNOWN_JOB = new DynamicCommandExceptionType(
            input -> Component.translatable("commands.ragnarmmo.admin.job.unknown", input));

    private static final JobArgumentType INSTANCE = new JobArgumentType();

    private JobArgumentType() {
    }

    public static JobArgumentType job() {
        return INSTANCE;
    }

    public static JobType getJob(CommandContext<?> context, String name) {
        return context.getArgument(name, JobType.class);
    }

    @Override
    public JobType parse(StringReader reader) throws CommandSyntaxException {
        String input = reader.readUnquotedString();
        String cleaned = input;
        if (cleaned.contains(":")) {
            cleaned = cleaned.substring(cleaned.indexOf(':') + 1);
        }
        try {
            return JobType.valueOf(cleaned.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw UNKNOWN_JOB.create(input);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Stream.of(JobType.values()).map(JobType::getId), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.asList("novice", "swordsman", "mage");
    }
}

