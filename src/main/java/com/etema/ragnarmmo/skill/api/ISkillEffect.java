package com.etema.ragnarmmo.skill.api;

import com.etema.ragnarmmo.skill.api.SkillType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Collections;
import java.util.Set;

/**
 * Common interface for all skill effect handlers.
 */
public interface ISkillEffect {

    enum TriggerType {
        OFFENSIVE_HURT,
        DEFENSIVE_HURT,
        PERIODIC_TICK,
        ITEM_USE_FINISH
    }

    /**
     * Returns the triggers that this skill effect responds to.
     * Used for optimization in SkillEffectHandler.
     */
    default Set<TriggerType> getSupportedTriggers() {
        return Collections.emptySet();
    }

    /**
     * Returns the ResourceLocation ID for this skill effect.
     * This is the canonical method for identifying skills.
     *
     * @return The skill ID (e.g., "ragnarmmo:bash")
     */
    @SuppressWarnings("removal")
    default ResourceLocation getSkillId() {
        SkillType type = getSkillType();
        return type != null ? type.toResourceLocation() : null;
    }

    /**
     * @return The type of skill this effect belongs to.
     * @deprecated Use {@link #getSkillId()} instead
     */
    @Deprecated(forRemoval = true)
    @SuppressWarnings("removal")
    default SkillType getSkillType() {
        ResourceLocation id = getSkillId();
        return id != null ? SkillType.fromResourceLocation(id) : null;
    }

    /**
     * Handle offensive effects (when the player is the attacker).
     */
    default void onOffensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
    }

    /**
     * Handle defensive effects (when the player is being hurt).
     */
    default void onDefensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
    }

    /**
     * Handle periodic effects (e.g., every 5 seconds).
     */
    default void onPeriodicTick(TickEvent.PlayerTickEvent event, ServerPlayer player, int level) {
    }

    /**
     * Called when the player finishes using an item (e.g. eating food).
     */
    default void onItemUseFinish(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish event, ServerPlayer player, int level) {
    }

    /**
     * Execute specific active skill logic (triggered by packet or AI).
     * 
     * @param entity The entity casting the skill.
     * @param level  The skill level.
     */
    default void execute(net.minecraft.world.entity.LivingEntity entity, int level) {
    }

    /**
     * @deprecated Use {@link #execute(net.minecraft.world.entity.LivingEntity, int)} instead.
     */
    @Deprecated
    default void execute(ServerPlayer player, int level) {
        execute((net.minecraft.world.entity.LivingEntity) player, level);
    }

    /**
     * Get the casting time in ticks (20 ticks = 1 second).
     * Default is 0 (instant).
     */
    default int getCastTime(int level) {
        return 0;
    }

    /**
     * Whether this skill's cast can be interrupted by movement or damage.
     * Default is true.
     */
    default boolean isInterruptible() {
        return true;
    }
}
