package com.etema.ragnarmmo.skills.api;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
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

    default ResourceLocation getSkillId() {
        return null;
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

    default void execute(ServerPlayer player, int level) {
    }

    default void execute(LivingEntity entity, int level) {
        if (entity instanceof ServerPlayer player) {
            execute(player, level);
        }
    }

    /**
     * Get the casting time in ticks (20 ticks = 1 second).
     * Default is 0 (instant).
     */
    default int getCastTime(int level) {
        return 0;
    }

    /**
     * Get the skill delay (global cooldown) in ticks after execution.
     * Default is 0 (uses value from skill definition).
     */
    default int getCastDelay(int level) {
        return 0;
    }

    /**
     * Whether this skill's cast can be interrupted by movement or damage.
     * Default is true.
     */
    default boolean isInterruptible() {
        return true;
    }

    /**
     * Get the resource cost (SP/Mana) for this skill at a given level.
     * @param defaultCost The cost calculated from the skill definition.
     */
    default int getResourceCost(int level, int defaultCost) {
        return defaultCost;
    }
}
