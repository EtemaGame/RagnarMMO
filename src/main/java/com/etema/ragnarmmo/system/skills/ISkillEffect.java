package com.etema.ragnarmmo.system.skills;

import com.etema.ragnarmmo.common.api.skills.SkillType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Common interface for all skill effect handlers.
 */
public interface ISkillEffect {

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
     * Execute specific active skill logic (triggered by packet).
     * 
     * @param player The player casting the skill.
     * @param level  The skill level.
     */
    default void execute(ServerPlayer player, int level) {
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