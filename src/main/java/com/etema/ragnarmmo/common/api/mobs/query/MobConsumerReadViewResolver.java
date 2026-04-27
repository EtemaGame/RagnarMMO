package com.etema.ragnarmmo.common.api.mobs.query;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.Optional;

public final class MobConsumerReadViewResolver {

    private MobConsumerReadViewResolver() {
    }

    public static Optional<MobConsumerReadView> resolve(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");

        MobProfile canonicalProfile = MobProfileProvider.get(entity)
                .resolve()
                .filter(MobProfileState::isInitialized)
                .map(MobProfileState::profile)
                .orElse(null);
        if (canonicalProfile != null) {
            return Optional.of(fromCanonicalProfile(entity, canonicalProfile));
        }
        return Optional.empty();
    }

    private static MobConsumerReadView fromCanonicalProfile(
            LivingEntity entity,
            MobProfile profile) {
        ResourceLocation entityTypeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        boolean bossLike = profile.rank() == MobRank.MINI_BOSS
                || profile.rank() == MobRank.BOSS;

        return new MobConsumerReadView(
                entityTypeId,
                MobConsumerDataOrigin.NEW_RUNTIME_PROFILE,
                profile.level(),
                profile.rank(),
                profile.race(),
                profile.element(),
                profile.size(),
                bossLike,
                true,
                new MobConsumerInspectionStatsView(
                        profile.maxHp(),
                        profile.atkMin(),
                        profile.atkMax(),
                        profile.def(),
                        profile.mdef()));
    }
}
