package com.etema.ragnarmmo.skill.job.hunter;

import com.etema.ragnarmmo.common.init.RagnarCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Mod.EventBusSubscriber(modid = RagnarCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HunterTrapManager {

    private static final Map<ServerLevel, List<TrapInstance>> ACTIVE_TRAPS = new ConcurrentHashMap<>();

    // Trap Definition that dictates behavior
    public record TrapDefinition(
            String id,
            double radius,
            int maxDurationTicks,
            ParticleOptions idleParticle,
            ParticleOptions triggerParticle,
            BiConsumer<TrapInstance, LivingEntity> onTrigger) {
    }

    // A placed instance of a trap in the world
    public static class TrapInstance {
        public final Player owner;
        public final TrapDefinition definition;
        public final BlockPos position;
        public final ServerLevel level;
        public final int skillLevel;
        public int ticksAlive;

        public TrapInstance(Player owner, TrapDefinition definition, BlockPos position, ServerLevel level, int skillLevel) {
            this.owner = owner;
            this.definition = definition;
            this.position = position;
            this.level = level;
            this.skillLevel = skillLevel;
            this.ticksAlive = 0;
        }

        public AABB getBoundingBox() {
            return new AABB(position).inflate(definition.radius, definition.radius, definition.radius);
        }
    }

    /**
     * Places a trap in the world.
     */
    public static void placeTrap(Player owner, ServerLevel level, BlockPos pos, TrapDefinition definition, int skillLevel) {
        ACTIVE_TRAPS.computeIfAbsent(level, l -> new ArrayList<>())
                .add(new TrapInstance(owner, definition, pos, level, skillLevel));
    }

    /**
     * Removes all traps placed by {@code owner} within {@code radius} blocks in {@code level}.
     * @return number of traps removed.
     */
    public static int removePlayerTrapsNear(Player owner, ServerLevel level, double radius) {
        List<TrapInstance> traps = ACTIVE_TRAPS.get(level);
        if (traps == null || traps.isEmpty()) return 0;

        net.minecraft.world.phys.AABB bounds = owner.getBoundingBox().inflate(radius);
        int count = 0;
        Iterator<TrapInstance> it = traps.iterator();
        while (it.hasNext()) {
            TrapInstance trap = it.next();
            if (trap.owner == owner && bounds.contains(
                    trap.position.getX() + 0.5, trap.position.getY() + 0.5, trap.position.getZ() + 0.5)) {
                it.remove();
                count++;
            }
        }
        return count;
    }

    /**
     * Ticks all traps every server tick.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        List<TrapInstance> traps = ACTIVE_TRAPS.get(serverLevel);
        if (traps == null || traps.isEmpty()) {
            return;
        }

        // Use iterator to safely remove triggered or expired traps
        Iterator<TrapInstance> iterator = traps.iterator();
        while (iterator.hasNext()) {
            TrapInstance trap = iterator.next();
            trap.ticksAlive++;

            // Expire logic
            if (trap.ticksAlive > trap.definition.maxDurationTicks) {
                iterator.remove();
                continue;
            }

            // Visuals
            if (trap.ticksAlive % 20 == 0 && trap.definition.idleParticle != null) {
                spawnIdleParticles(serverLevel, trap);
            }

            // Collision check
            AABB bounds = trap.getBoundingBox();
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, bounds,
                    entity -> entity.isAlive() && entity != trap.owner && !entity.isAlliedTo(trap.owner));

            if (!targets.isEmpty()) {
                // Trap went off
                for (LivingEntity target : targets) {
                    if (trap.definition.onTrigger != null) {
                        try {
                            trap.definition.onTrigger.accept(trap, target);
                        } catch (Exception e) {
                            com.mojang.logging.LogUtils.getLogger().error("Error triggering Hunter Traps", e);
                        }
                    }
                }

                // Trigger particles
                if (trap.definition.triggerParticle != null) {
                    spawnTriggerParticles(serverLevel, trap);
                }

                // Remove after triggering
                iterator.remove();
            }
        }
    }

    private static void spawnIdleParticles(ServerLevel level, TrapInstance trap) {
        level.sendParticles(
                trap.definition.idleParticle,
                trap.position.getX() + 0.5,
                trap.position.getY() + 0.1,
                trap.position.getZ() + 0.5,
                3,
                trap.definition.radius / 2.0,
                0.1,
                trap.definition.radius / 2.0,
                0.0);
    }

    private static void spawnTriggerParticles(ServerLevel level, TrapInstance trap) {
        level.sendParticles(
                trap.definition.triggerParticle,
                trap.position.getX() + 0.5,
                trap.position.getY() + 0.5,
                trap.position.getZ() + 0.5,
                30,
                trap.definition.radius,
                trap.definition.radius,
                trap.definition.radius,
                0.1);
    }
}
