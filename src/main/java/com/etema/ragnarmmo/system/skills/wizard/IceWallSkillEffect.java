package com.etema.ragnarmmo.system.skills.wizard;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

public class IceWallSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo:ice_wall");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Ice Wall: Creates a wall of ice blocks.
        BlockPos base = player.blockPosition().relative(player.getDirection(), 2);

        for (int i = -1; i <= 1; i++) {
            BlockPos p = base.relative(player.getDirection().getClockWise(), i);
            if (player.level().getBlockState(p).isAir()) {
                player.level().setBlock(p, Blocks.ICE.defaultBlockState(), 3);
            }
            if (player.level().getBlockState(p.above()).isAir()) {
                player.level().setBlock(p.above(), Blocks.ICE.defaultBlockState(), 3);
            }
        }
    }
}
