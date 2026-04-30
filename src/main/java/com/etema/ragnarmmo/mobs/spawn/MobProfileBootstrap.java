package com.etema.ragnarmmo.mobs.spawn;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.config.access.MobConfigAccess;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import com.etema.ragnarmmo.mobs.companion.CompanionProfileService;
import com.etema.ragnarmmo.mobs.difficulty.DifficultyContext;
import com.etema.ragnarmmo.mobs.difficulty.MobDifficultyResolver;
import com.etema.ragnarmmo.mobs.network.SyncMobProfilePacket;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import com.etema.ragnarmmo.mobs.profile.MobProfileFactory;
import com.etema.ragnarmmo.mobs.util.MobAttributeHelper;
import com.etema.ragnarmmo.mobs.util.MobProfileEligibility;
import com.etema.ragnarmmo.mobs.world.BossWorldRegistrationBridge;

import java.util.Optional;
import java.util.OptionalInt;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Canonical mob profile bootstrap shared by spawn and combat lazy-init paths.
 */
public final class MobProfileBootstrap {
    private static final MobSpawnInitializer INITIALIZER = new MobSpawnInitializer(
            new MobDifficultyResolver(),
            new MobProfileFactory());

    private MobProfileBootstrap() {
    }

    public enum InitReason {
        SPAWN,
        COMBAT_LAZY
    }

    public static Optional<MobProfile> ensureInitialized(LivingEntity living, InitReason reason) {
        if (living == null || living.level().isClientSide() || !MobConfigAccess.isEnabled()) {
            return Optional.empty();
        }

        return MobProfileProvider.get(living).resolve().flatMap(state -> ensureInitialized(living, state, reason));
    }

    public static Optional<MobProfile> ensureInitialized(LivingEntity living, MobProfileState state, InitReason reason) {
        if (living == null || state == null || living.level().isClientSide() || !MobConfigAccess.isEnabled()) {
            return Optional.empty();
        }
        if (state.isInitialized()) {
            return Optional.of(state.profile());
        }

        MobProfileEligibility.Classification classification = MobProfileEligibility.classify(living);
        if (classification == MobProfileEligibility.Classification.INELIGIBLE) {
            state.clearProfile();
            Network.sendTrackingEntityAndSelf(living, SyncMobProfilePacket.clear(living));
            return Optional.empty();
        }
        if (classification == MobProfileEligibility.Classification.COMPANION) {
            CompanionProfileService.refreshOnJoin(living, state);
            return state.isInitialized() ? Optional.of(state.profile()) : Optional.empty();
        }

        MobProfile profile = INITIALIZER.initialize(createDifficultyContext(living));
        state.setProfile(profile);
        MobAttributeHelper.HealthPreservationMode healthMode = reason == InitReason.SPAWN
                ? MobAttributeHelper.HealthPreservationMode.SPAWN_FULL_HEAL
                : MobAttributeHelper.HealthPreservationMode.PRESERVE_RATIO;
        MobAttributeHelper.applyAttributes(living, profile, healthMode);
        SyncMobProfilePacket.fromEntity(living).ifPresent(packet -> Network.sendTrackingEntityAndSelf(living, packet));
        BossWorldRegistrationBridge.handleRegistration(living, profile.rank());
        RagnarDebugLog.mobSpawns(
                "PROFILE_BOOTSTRAP reason={} mob={} pos={} rank={} level={}",
                reason,
                RagnarDebugLog.entityLabel(living),
                RagnarDebugLog.blockPos(living.blockPosition()),
                profile.rank(),
                profile.level());
        return Optional.of(profile);
    }

    private static DifficultyContext createDifficultyContext(LivingEntity living) {
        ResourceLocation entityTypeId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
        ResourceLocation dimensionId = living.level().dimension().location();
        BlockPos worldSpawnPos = living.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                ? serverLevel.getSharedSpawnPos()
                : BlockPos.ZERO;
        Player nearest = living.level().getNearestPlayer(living,
                MobConfigAccess.getDifficultyRules().playerLevelRadius());
        OptionalInt nearestPlayerLevel = nearest != null
                ? RagnarCoreAPI.get(nearest).map(stats -> OptionalInt.of(stats.getLevel())).orElse(OptionalInt.empty())
                : OptionalInt.empty();
        long worldSeed = living.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                ? serverLevel.getSeed()
                : 0L;
        Optional<ResourceLocation> biomeId = living.level()
                .getBiome(living.blockPosition())
                .unwrapKey()
                .map(key -> key.location());

        return new DifficultyContext(
                entityTypeId,
                dimensionId,
                living.blockPosition(),
                worldSpawnPos,
                biomeId,
                Optional.empty(),
                nearestPlayerLevel,
                worldSeed);
    }
}
