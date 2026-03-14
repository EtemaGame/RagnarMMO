package com.etema.ragnarmmo.system.skills.hunter;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Remove Trap — Active
 * RO: Removes a friendly or enemy trap from the ground.
 *     Recovers a portion of the items used to set the trap.
 *
 * Minecraft:
 *  - Iterates HunterTrapManager for traps near the player (within 4 blocks).
 *  - Removes ALL traps owned by the player within range.
 *  - Shows a count of removed traps via chat.
 */
public class RemoveTrapSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "remove_trap");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        if (!(player.level() instanceof ServerLevel sl)) return;

        double radius = 4.0 + level * 0.5;

        // Access the TrapManager's internal map via a helper we expose
        int removed = HunterTrapManager.removePlayerTrapsNear(player, sl, radius);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIPWIRE_DETACH, SoundSource.PLAYERS, 1.0f, 1.2f);

        if (player.level() instanceof ServerLevel sl2) {
            sl2.sendParticles(ParticleTypes.POOF,
                    player.getX(), player.getY() + 1, player.getZ(),
                    10, 1.0, 0.3, 1.0, 0.05);
        }

        if (removed > 0) {
            player.sendSystemMessage(Component.literal("§aRemoved §f" + removed + " trap" + (removed > 1 ? "s" : "") + "."));
        } else {
            player.sendSystemMessage(Component.literal("§7No traps found nearby."));
        }
    }
}
