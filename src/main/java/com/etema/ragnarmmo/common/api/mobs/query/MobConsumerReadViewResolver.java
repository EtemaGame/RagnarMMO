package com.etema.ragnarmmo.common.api.mobs.query;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

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
        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        boolean bossLikeForCompatibility = profile.rank() == MobRank.BOSS
                || profile.rank() == MobRank.MVP;

        return new MobConsumerReadView(
                entityTypeId,
                MobConsumerDataOrigin.NEW_RUNTIME_PROFILE,
                profile.level(),
                profile.rank(),
                profile.race(),
                profile.element(),
                profile.size(),
                bossLikeForCompatibility,
                true,
                new MobConsumerInspectionStatsView(
                        profile.maxHp(),
                        profile.atkMin(),
                        profile.atkMax(),
                        profile.def(),
                        profile.mdef()));
    }
}
