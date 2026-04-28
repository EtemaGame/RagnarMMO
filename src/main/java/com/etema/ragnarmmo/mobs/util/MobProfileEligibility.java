package com.etema.ragnarmmo.mobs.util;

import com.etema.ragnarmmo.common.config.access.MobConfigAccess;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public final class MobProfileEligibility {
    public enum Classification {
        INELIGIBLE,
        STANDARD_MOB,
        COMPANION
    }

    private MobProfileEligibility() {
    }

    public static Classification classify(LivingEntity entity) {
        if (entity == null || entity instanceof Player) {
            return Classification.INELIGIBLE;
        }

        ResourceLocation entityTypeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (MobConfigAccess.isExcluded(entityTypeId)) {
            return Classification.INELIGIBLE;
        }

        if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
            return Classification.COMPANION;
        }

        if (ownerUuid(entity).isPresent()) {
            return Classification.COMPANION;
        }

        if (isPassiveWildCategory(entity.getType().getCategory())) {
            return Classification.INELIGIBLE;
        }

        return entity instanceof Mob ? Classification.STANDARD_MOB : Classification.INELIGIBLE;
    }

    public static boolean isEligible(LivingEntity entity) {
        return classify(entity) != Classification.INELIGIBLE;
    }

    public static boolean isCompanion(LivingEntity entity) {
        return classify(entity) == Classification.COMPANION;
    }

    public static Optional<UUID> ownerUuid(LivingEntity entity) {
        if (entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return Optional.of(ownable.getOwnerUUID());
        }
        if (entity instanceof TamableAnimal tamable && tamable.getOwnerUUID() != null) {
            return Optional.of(tamable.getOwnerUUID());
        }
        return Optional.empty();
    }

    public static boolean isPassiveWildCategory(MobCategory category) {
        return category == MobCategory.CREATURE
                || category == MobCategory.AMBIENT
                || category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.AXOLOTLS
                || category == MobCategory.UNDERGROUND_WATER_CREATURE;
    }
}
