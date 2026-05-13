package com.etema.ragnarmmo.bestiary.data;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.bestiary.api.BestiaryCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.Map;

public final class BestiaryClassificationService {
    private static final Map<ResourceLocation, BestiaryCategory> VANILLA_OVERRIDES = Map.ofEntries(
            Map.entry(id("enderman"), BestiaryCategory.NEUTRAL),
            Map.entry(id("piglin"), BestiaryCategory.NEUTRAL),
            Map.entry(id("zombified_piglin"), BestiaryCategory.NEUTRAL),
            Map.entry(id("bee"), BestiaryCategory.NEUTRAL),
            Map.entry(id("wolf"), BestiaryCategory.NEUTRAL),
            Map.entry(id("iron_golem"), BestiaryCategory.NEUTRAL),
            Map.entry(id("polar_bear"), BestiaryCategory.NEUTRAL),
            Map.entry(id("ender_dragon"), BestiaryCategory.BOSS),
            Map.entry(id("wither"), BestiaryCategory.BOSS),
            Map.entry(id("warden"), BestiaryCategory.BOSS));

    private BestiaryClassificationService() {
    }

    public static BestiaryCategory classify(ResourceLocation entityId, EntityType<?> type) {
        BestiaryCategory override = VANILLA_OVERRIDES.get(entityId);
        if (override != null) {
            return override;
        }

        MobCategory category = type.getCategory();
        if (category == MobCategory.MONSTER) {
            return BestiaryCategory.AGGRESSIVE;
        }
        if (category == MobCategory.CREATURE
                || category == MobCategory.AMBIENT
                || category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.AXOLOTLS
                || category == MobCategory.UNDERGROUND_WATER_CREATURE) {
            return BestiaryCategory.PASSIVE;
        }
        return BestiaryCategory.UNKNOWN;
    }

    public static boolean isSupportedCategory(MobCategory category) {
        return category == MobCategory.MONSTER
                || category == MobCategory.CREATURE
                || category == MobCategory.AMBIENT
                || category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.AXOLOTLS
                || category == MobCategory.UNDERGROUND_WATER_CREATURE;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
}
