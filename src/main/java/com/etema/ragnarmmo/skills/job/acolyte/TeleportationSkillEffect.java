package com.etema.ragnarmmo.skills.job.acolyte;

import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Teleportation — Active (Acolyte)
 * Lv.1 teleports the caster randomly on the current map.
 * Lv.2 returns the caster to the Save Point.
 */
public class TeleportationSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "teleportation");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        if (level >= 2) {
            var destination = WarpPortalHelper.resolveSavePoint(player);
            ServerLevel serverLevel = player.serverLevel();
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    RagnarSounds.TELEPORTATION.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

            if (WarpPortalHelper.teleport(player, destination)) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        RagnarSounds.TELEPORTATION.get(), SoundSource.PLAYERS, 1.0f, 1.2f);
                player.sendSystemMessage(Component.literal("§d✦ Teleportation §f→ §7Save Point"));
            } else {
                player.sendSystemMessage(Component.literal("§cTeleportation: §fNo se pudo llegar al Save Point."));
            }
            return;
        }

        double radius = SkillRegistry.get(ID)
                .map(def -> def.getLevelDouble("random_radius", level, 30.0D + level * 20.0D))
                .orElse(30.0D + level * 20.0D);
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
                        RagnarSounds.TELEPORTATION.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

                player.teleportTo(finalX, finalY, finalZ);

                // Play arrival sound
                sl.playSound(null, finalX, finalY, finalZ,
                        RagnarSounds.TELEPORTATION.get(), SoundSource.PLAYERS, 1.0f, 1.2f);

                player.sendSystemMessage(Component.literal(
                        "§d✦ Teleportation §f→ §7(" + (int)finalX + ", " + (int)finalY + ", " + (int)finalZ + ")"));
                return;
            }
        }

        player.sendSystemMessage(Component.literal("§cTeleportation: §fNo se encontró posición segura."));
    }
}
