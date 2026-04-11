package com.etema.ragnarmmo.skill.execution.projectile;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.compute.DerivedStats;
import com.etema.ragnarmmo.skill.data.SkillDefinition;
import com.etema.ragnarmmo.system.stats.compute.EquipmentStatSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared bow/projectile helper for Archer-style skills and ranged passives.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ProjectileSkillHelper {
    public static final String PROJECTILE_TUNING_APPLIED_TAG = "ragnarmmo_projectile_tuning_applied";
    public static final String PROJECTILE_GRAVITY_MULT_TAG = "ragnarmmo_projectile_gravity_mult";
    public static final String SKILL_DAMAGE_MULTIPLIER_TAG = "ragnarmmo_skill_damage_multiplier";
    public static final String FORCED_DRAW_RATIO_TAG = "ragnarmmo_forced_draw_ratio";

    private static final Set<UUID> GRAVITY_ADJUSTED_PROJECTILES = ConcurrentHashMap.newKeySet();

    private ProjectileSkillHelper() {
    }

    public static boolean isBow(ItemStack stack) {
        return stack != null && stack.getItem() instanceof BowItem;
    }

    public static boolean requireBow(ServerPlayer player) {
        if (isBow(player.getMainHandItem())) {
            return true;
        }

        player.sendSystemMessage(Component.translatable("message.ragnarmmo.requires_bow")
                .withStyle(ChatFormatting.RED));
        return false;
    }

    public static Arrow spawnArrow(ServerPlayer player, SkillDefinition definition, int level, Vec3 direction,
            double baseDamage, float baseVelocity, float baseSpread, boolean visualCrit, double yOffset) {
        return spawnArrow(player, definition, level, direction, baseDamage, baseVelocity, baseSpread, visualCrit,
                yOffset, 0.0D, 1.0D);
    }

    public static Arrow spawnArrow(ServerPlayer player, SkillDefinition definition, int level, Vec3 direction,
            double baseDamage, float baseVelocity, float baseSpread, boolean visualCrit, double yOffset,
            double skillDamageMultiplier, double forcedDrawRatio) {
        ProjectileTuning tuning = resolveTuning(player, definition, level);
        Arrow arrow = new Arrow(player.level(), player);
        arrow.setPos(player.getX(), player.getEyeY() + yOffset, player.getZ());
        arrow.shoot(direction.x, direction.y, direction.z,
                baseVelocity * (float) tuning.velocityMult(), baseSpread * (float) tuning.spreadMult());
        arrow.setBaseDamage(baseDamage);
        arrow.setCritArrow(visualCrit);
        applyGravityMultiplier(arrow, tuning.gravityMult());
        if (skillDamageMultiplier > 0.0D) {
            markSkillDamage(arrow, skillDamageMultiplier, forcedDrawRatio);
        }
        arrow.getPersistentData().putBoolean(PROJECTILE_TUNING_APPLIED_TAG, true);
        player.level().addFreshEntity(arrow);
        return arrow;
    }

    public static void applyPassiveProjectileModifiers(AbstractArrow arrow, Player player) {
        if (arrow == null || player == null || arrow.getPersistentData().getBoolean(PROJECTILE_TUNING_APPLIED_TAG)) {
            return;
        }

        ProjectileTuning tuning = resolveTuning(player, null, 0);
        if (Math.abs(tuning.velocityMult() - 1.0D) > 1.0E-4) {
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(tuning.velocityMult()));
            arrow.hasImpulse = true;
        }

        applyGravityMultiplier(arrow, tuning.gravityMult());
        arrow.getPersistentData().putBoolean(PROJECTILE_TUNING_APPLIED_TAG, true);
    }

    public static ProjectileTuning resolveTuning(Player player, SkillDefinition definition, int level) {
        DerivedStats derived = RagnarCoreAPI.get(player)
                .flatMap(stats -> RagnarCoreAPI.computeDerivedStats(player, stats, EquipmentStatSnapshot.capture(player)))
                .orElseGet(DerivedStats::new);

        double velocity = sanitizeMultiplier(derived.projectileVelocityMult);
        double gravity = sanitizeMultiplier(derived.projectileGravityMult);
        double spread = sanitizeMultiplier(derived.projectileSpreadMult);

        if (definition != null && level > 0) {
            velocity *= sanitizeMultiplier(definition.getLevelDouble("projectile_velocity_mult", level, 1.0D));
            gravity *= sanitizeMultiplier(definition.getLevelDouble("projectile_gravity_mult", level, 1.0D));
            spread *= sanitizeMultiplier(definition.getLevelDouble("projectile_spread_mult", level, 1.0D));
        }

        return new ProjectileTuning(velocity, gravity, spread);
    }

    public static void markSkillDamage(AbstractArrow arrow, double damageMultiplier, double forcedDrawRatio) {
        if (arrow == null) {
            return;
        }
        arrow.getPersistentData().putDouble(SKILL_DAMAGE_MULTIPLIER_TAG, Math.max(0.0D, damageMultiplier));
        arrow.getPersistentData().putDouble(FORCED_DRAW_RATIO_TAG, Math.max(0.1D, Math.min(1.0D, forcedDrawRatio)));
    }

    private static void applyGravityMultiplier(AbstractArrow arrow, double gravityMult) {
        double sanitized = Math.max(0.0D, Math.min(2.0D, gravityMult));
        if (Math.abs(sanitized - 1.0D) <= 1.0E-4) {
            return;
        }

        arrow.getPersistentData().putDouble(PROJECTILE_GRAVITY_MULT_TAG, sanitized);
        GRAVITY_ADJUSTED_PROJECTILES.add(arrow.getUUID());
    }

    private static double sanitizeMultiplier(double value) {
        if (Double.isNaN(value) || value <= 0.0D) {
            return 1.0D;
        }
        return Math.max(0.05D, Math.min(4.0D, value));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || GRAVITY_ADJUSTED_PROJECTILES.isEmpty()) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        GRAVITY_ADJUSTED_PROJECTILES.removeIf(uuid -> !adjustGravity(server, uuid));
    }

    private static boolean adjustGravity(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (!(entity instanceof AbstractArrow arrow) || !arrow.isAlive()) {
                continue;
            }

            double gravityMult = arrow.getPersistentData().getDouble(PROJECTILE_GRAVITY_MULT_TAG);
            if (gravityMult <= 0.0D || Math.abs(gravityMult - 1.0D) <= 1.0E-4) {
                return false;
            }

            double vanillaArrowGravity = 0.05D;
            double correction = vanillaArrowGravity * (1.0D - gravityMult);
            if (Math.abs(correction) > 1.0E-5) {
                Vec3 velocity = arrow.getDeltaMovement();
                arrow.setDeltaMovement(velocity.x, velocity.y + correction, velocity.z);
                arrow.hasImpulse = true;
            }
            return true;
        }

        return false;
    }

    public record ProjectileTuning(double velocityMult, double gravityMult, double spreadMult) {
    }
}
