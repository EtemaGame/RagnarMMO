package com.etema.ragnarmmo.skill.runtime;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * SkillSequencer — Server-side utility to schedule delayed tasks.
 * Primarily used for multi-hit skills, travel-time projectiles, 
 * and sequenced visual effects without blocking the server.
 */
@Mod.EventBusSubscriber(modid = "ragnarmmo")
public final class SkillSequencer {

    private static final List<ScheduledTask> TASKS = new ArrayList<>();

    private SkillSequencer() {}

    /**
     * Schedules a task to run after a certain number of ticks.
     * @param delayTicks Number of ticks to wait
     * @param action The code to execute
     */
    public static void schedule(int delayTicks, Runnable action) {
        if (delayTicks <= 0) {
            action.run();
        } else {
            TASKS.add(new ScheduledTask(delayTicks, action));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        synchronized (TASKS) {
            Iterator<ScheduledTask> it = TASKS.iterator();
            while (it.hasNext()) {
                ScheduledTask task = it.next();
                task.ticksRemaining--;
                if (task.ticksRemaining <= 0) {
                    try {
                        task.action.run();
                    } catch (Exception e) {
                        e.printStackTrace(); // Log but don't crash
                    }
                    it.remove();
                }
            }
        }
    }

    private static class ScheduledTask {
        int ticksRemaining;
        final Runnable action;

        ScheduledTask(int ticks, Runnable action) {
            this.ticksRemaining = ticks;
            this.action = action;
        }
    }
}
