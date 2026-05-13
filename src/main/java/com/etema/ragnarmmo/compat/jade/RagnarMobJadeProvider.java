package com.etema.ragnarmmo.compat.jade;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum RagnarMobJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(RagnarMMO.MODID, "mob_profile");
    private static final String ROOT = "RagnarMMO";
    private static final String INITIALIZED = "Initialized";
    private static final String LEVEL = "Level";
    private static final String RANK = "Rank";
    private static final String TIER = "Tier";
    private static final String RACE = "Race";
    private static final String ELEMENT = "Element";
    private static final String SIZE = "Size";
    private static final String MAX_HP = "MaxHp";
    private static final String BASE_EXP = "BaseExp";
    private static final String JOB_EXP = "JobExp";

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        Entity entity = accessor.getEntity();
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        MobProfileProvider.get(living)
                .resolve()
                .filter(MobProfileState::isInitialized)
                .map(MobProfileState::profile)
                .ifPresent(profile -> data.put(ROOT, encode(profile)));
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        Entity entity = accessor.getEntity();
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        CompoundTag profile = accessor.getServerData().getCompound(ROOT);
        if (!profile.getBoolean(INITIALIZED)) {
            return;
        }

        int level = Math.max(1, profile.getInt(LEVEL));
        tooltip.add(Component.translatable("jade.ragnarmmo.level", level).withStyle(ChatFormatting.GOLD), UID);
        tooltip.add(Component.literal(taxonomy(profile)).withStyle(ChatFormatting.GRAY), UID);
        tooltip.add(Component.literal(healthLine(living, profile)).withStyle(ChatFormatting.RED), UID);

        if (accessor.showDetails()) {
            tooltip.add(Component.literal(rewardLine(profile)).withStyle(ChatFormatting.DARK_GREEN), UID);
        }
    }

    private static CompoundTag encode(MobProfile profile) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(INITIALIZED, true);
        tag.putInt(LEVEL, profile.level());
        tag.putString(RANK, profile.rank().name());
        tag.putString(TIER, profile.tier().name());
        tag.putString(RACE, profile.race());
        tag.putString(ELEMENT, profile.element());
        tag.putString(SIZE, profile.size());
        tag.putInt(MAX_HP, profile.maxHp());
        tag.putInt(BASE_EXP, profile.baseExp());
        tag.putInt(JOB_EXP, profile.jobExp());
        return tag;
    }

    private static String taxonomy(CompoundTag profile) {
        return formatToken(profile.getString(RANK))
                + " / " + formatToken(profile.getString(RACE))
                + " / " + formatToken(profile.getString(ELEMENT))
                + " / " + formatToken(profile.getString(SIZE));
    }

    private static String healthLine(LivingEntity living, CompoundTag profile) {
        int hp = Math.max(0, Math.round(living.getHealth()));
        int maxHp = Math.max(1, Math.round(Math.max(living.getMaxHealth(), profile.getInt(MAX_HP))));
        return "HP " + Mth.clamp(hp, 0, maxHp) + " / " + maxHp;
    }

    private static String rewardLine(CompoundTag profile) {
        return "EXP " + Math.max(0, profile.getInt(BASE_EXP))
                + " / Job " + Math.max(0, profile.getInt(JOB_EXP));
    }

    private static String formatToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Unknown";
        }

        String normalized = raw.replace('_', ' ').replace('-', ' ').trim().toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(normalized.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                out.append(c);
                continue;
            }
            out.append(capitalizeNext ? Character.toUpperCase(c) : c);
            capitalizeNext = false;
        }
        return out.toString();
    }
}
