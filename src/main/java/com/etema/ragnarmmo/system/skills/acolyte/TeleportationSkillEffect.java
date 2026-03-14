package com.etema.ragnarmmo.system.skills.acolyte;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Teleportation — Active (Acolyte)
 * RO: Randomly teleports the caster to a new location on the same map.
 * MC: Teleports to a random safe location within a radius of (30 + level * 20)
 *     blocks. Attempts up to 10 raycasts to find solid ground.
 *     Level 1 = 50 block radius, Level 2 = 70, ... Level 4 = 110 (max in RO).
 */
public class TeleportationSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "teleportation");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        double radius = 30 + level * 20;
        var random = player.getRandom();

        for (int attempt = 0; attempt < 15; attempt++) {
            double dx = (random.nextDouble() * 2 - 1) * radius;
            double dz = (random.nextDouble() * 2 - 1) * radius;
            double tx = player.getX() + dx;
            double tz = player.getZ() + dz;

            // Find topmost solid block y
            BlockPos target = new BlockPos((int) tx, (int) player.getY(), (int) tz);
            ServerLevel sl = (ServerLevel) player.level();

            // Walk downward from world height to find ground
            int topY = sl.getHeight();
            BlockPos groundPos = null;
            for (int y = topY; y > sl.getMinBuildHeight(); y--) {
                BlockPos check = new BlockPos((int) tx, y, (int) tz);
                BlockState below = sl.getBlockState(check.below());
                BlockState at    = sl.getBlockState(check);
                BlockState above = sl.getBlockState(check.above());
                if (below.isSolidRender(sl, check.below())
                        && at.isAir()
                        && above.isAir()) {
                    groundPos = check;
                    break;
                }
            }

            if (groundPos != null) {
                double finalX = groundPos.getX() + 0.5;
                double finalY = groundPos.getY();
                double finalZ = groundPos.getZ() + 0.5;

                // Play departure sound
                sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);

                player.teleportTo(finalX, finalY, finalZ);

                // Play arrival sound
                sl.playSound(null, finalX, finalY, finalZ,
                        SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.2f);

                player.sendSystemMessage(Component.literal(
                        "§d✦ Teleportation §f→ §7(" + (int)finalX + ", " + (int)finalY + ", " + (int)finalZ + ")"));
                return;
            }
        }

        player.sendSystemMessage(Component.literal("§cTeleportation: §fNo se encontró posición segura."));
    }
}
