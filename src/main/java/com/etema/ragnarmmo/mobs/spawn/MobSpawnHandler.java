package com.etema.ragnarmmo.mobs.spawn;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.config.access.MobConfigAccess;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import com.etema.ragnarmmo.mobs.difficulty.DifficultyContext;
import com.etema.ragnarmmo.mobs.difficulty.MobDifficultyResolver;
import com.etema.ragnarmmo.mobs.network.SyncMobProfilePacket;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import com.etema.ragnarmmo.mobs.profile.MobProfileFactory;
import com.etema.ragnarmmo.mobs.world.BossWorldRegistrationBridge;
import com.etema.ragnarmmo.mobs.util.MobAttributeHelper;
import com.etema.ragnarmmo.player.stats.util.AntiFarmManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;

public class MobSpawnHandler {

    private final Random rng = new Random();
    private final MobSpawnInitializer profileInitializer = new MobSpawnInitializer(
            new MobDifficultyResolver(),
            new MobProfileFactory());

    @SubscribeEvent
    public void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!RagnarConfigs.SERVER.progression.antiFarmSpawnReduction.get()) {
            return;
        }

        Player nearest = event.getLevel().getNearestPlayer(event.getX(), event.getY(), event.getZ(), 64, false);
        if (nearest != null) {
            double penalty = AntiFarmManager.getPenaltyFactor(nearest);
            if (penalty < 1.0D && rng.nextDouble() > penalty) {
                event.setSpawnCancelled(true);
            }
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public void onMobJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living) || living instanceof Player) {
            return;
        }
        ResourceLocation entityTypeId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
        if (!MobConfigAccess.isEnabled() || MobConfigAccess.isExcluded(entityTypeId)) {
            return;
        }

        MobProfileProvider.get(living).ifPresent(state -> {
            MobProfile profile = initializeCanonicalProfile(living, state);
            MobAttributeHelper.applyAttributes(living, profile);
            BossWorldRegistrationBridge.handleRegistration(living, profile.rank());
            RagnarDebugLog.mobSpawns(
                    "CANONICAL SPAWN mob={} pos={} rank={} level={}",
                    RagnarDebugLog.entityLabel(living),
                    RagnarDebugLog.blockPos(living.blockPosition()),
                    profile.rank(),
                    profile.level());
        });
    }

    private MobProfile initializeCanonicalProfile(LivingEntity living, MobProfileState state) {
        if (state.isInitialized()) {
            return state.profile();
        }

        DifficultyContext context = createDifficultyContext(living);
        MobProfile profile = profileInitializer.initialize(context);
        state.setProfile(profile);
        SyncMobProfilePacket.fromEntity(living).ifPresent(packet -> Network.sendTrackingEntityAndSelf(living, packet));
        return profile;
    }

    private DifficultyContext createDifficultyContext(LivingEntity living) {
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

        return new DifficultyContext(
                entityTypeId,
                dimensionId,
                living.blockPosition(),
                worldSpawnPos,
                Optional.empty(),
                nearestPlayerLevel,
                worldSeed);
    }
}
