package com.etema.ragnarmmo.system.skills.acolyte;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Warp Portal — Active (Acolyte)
 * RO: Creates a portal others can use to teleport to a set location.
 * MC: Saves the caster's current position as a "warp origin" in PersistentData.
 *     Also saves an "active warp" BlockPos so that any player who walks through
 *     it within (15 + level * 5) seconds gets teleported there.
 *     The portal is NOT a real block — it's a position stored server-side,
 *     and WarpPortalEvents (future) will handle the walk-through detection.
 *     For now: levels 1-2 create temp warp, level 2 also broadcasts to nearby players.
 */
public class WarpPortalSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "warp_portal");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        BlockPos pos = player.blockPosition();
        int durationSec = 15 + level * 5;

        // Store warp data in PersistentData
        var nbt = player.getPersistentData();
        nbt.putInt("warp_x", pos.getX());
        nbt.putInt("warp_y", pos.getY());
        nbt.putInt("warp_z", pos.getZ());
        nbt.putLong("warp_expiry", player.level().getGameTime() + (long) durationSec * 20);
        nbt.putString("warp_dim", player.level().dimension().location().toString());

        // Visual: purple portal swirl particles around caster
        if (player.level() instanceof ServerLevel sl) {
            for (int i = 0; i < 30; i++) {
                double angle = Math.toRadians(i * 12);
                double px = pos.getX() + 0.5 + Math.cos(angle) * 0.8;
                double pz = pos.getZ() + 0.5 + Math.sin(angle) * 0.8;
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, px, pos.getY() + 1.0, pz, 1, 0, 0.2, 0, 0.01);
            }
            sl.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 0.7f);
        }

        player.sendSystemMessage(Component.literal(
                "§5✦ Warp Portal §fabierto en §7(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ") §fpor " + durationSec + "s."));

        // Level 2: notify nearby players
        if (level >= 2 && player.level() instanceof ServerLevel sl) {
            sl.players().stream()
                    .filter(p -> p != player && p.distanceTo(player) < 30)
                    .forEach(p -> p.sendSystemMessage(Component.literal(
                            "§5[Warp Portal] §f" + player.getName().getString() + " §fabrió un portal cercano.")));
        }
    }
}
