package com.etema.ragnarmmo.combat.credit;

import java.util.Optional;
import java.util.UUID;

import com.etema.ragnarmmo.combat.api.CombatHitResultType;
import com.etema.ragnarmmo.combat.api.CombatResolution;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-side kill credit owned by the RO combat domain.
 */
public final class RoKillCreditService {
    private static final String CREDIT_PLAYER_TAG = "ragnarmmo_ro_kill_credit_player";
    private static final String CREDIT_TICK_TAG = "ragnarmmo_ro_kill_credit_tick";
    private static final String CREDIT_DAMAGE_TAG = "ragnarmmo_ro_kill_credit_damage";
    private static final int CREDIT_TTL_TICKS = 100;

    private RoKillCreditService() {
    }

    public static void recordPlayerContribution(ServerPlayer attacker, LivingEntity target, CombatResolution resolution) {
        if (attacker == null || target == null || resolution == null || target.level().isClientSide()) {
            return;
        }
        if (resolution.resultType() != CombatHitResultType.HIT && resolution.resultType() != CombatHitResultType.CRIT) {
            return;
        }
        CompoundTag data = target.getPersistentData();
        data.putUUID(CREDIT_PLAYER_TAG, attacker.getUUID());
        data.putLong(CREDIT_TICK_TAG, target.tickCount);
        data.putDouble(CREDIT_DAMAGE_TAG, Math.max(0.0D, resolution.finalAmount()));
    }

    public static void clearPlayerContribution(ServerPlayer attacker, LivingEntity target) {
        if (attacker == null || target == null) {
            return;
        }
        CompoundTag data = target.getPersistentData();
        if (data.hasUUID(CREDIT_PLAYER_TAG) && attacker.getUUID().equals(data.getUUID(CREDIT_PLAYER_TAG))) {
            data.remove(CREDIT_PLAYER_TAG);
            data.remove(CREDIT_TICK_TAG);
            data.remove(CREDIT_DAMAGE_TAG);
        }
    }

    public static Optional<ServerPlayer> resolveKiller(LivingEntity killed, DamageSource source) {
        if (killed == null || killed.level().isClientSide()) {
            return Optional.empty();
        }
        if (source != null && source.getEntity() instanceof ServerPlayer directPlayer) {
            return Optional.of(directPlayer);
        }
        return resolveRecordedCredit(killed);
    }

    private static Optional<ServerPlayer> resolveRecordedCredit(LivingEntity killed) {
        CompoundTag data = killed.getPersistentData();
        if (!data.hasUUID(CREDIT_PLAYER_TAG)) {
            return Optional.empty();
        }
        long tick = data.getLong(CREDIT_TICK_TAG);
        if (killed.tickCount - tick > CREDIT_TTL_TICKS) {
            clearRecordedCredit(killed);
            return Optional.empty();
        }
        MinecraftServer server = killed.level().getServer();
        if (server == null) {
            return Optional.empty();
        }
        UUID playerId = data.getUUID(CREDIT_PLAYER_TAG);
        return Optional.ofNullable(server.getPlayerList().getPlayer(playerId));
    }

    private static void clearRecordedCredit(LivingEntity killed) {
        CompoundTag data = killed.getPersistentData();
        data.remove(CREDIT_PLAYER_TAG);
        data.remove(CREDIT_TICK_TAG);
        data.remove(CREDIT_DAMAGE_TAG);
    }
}
