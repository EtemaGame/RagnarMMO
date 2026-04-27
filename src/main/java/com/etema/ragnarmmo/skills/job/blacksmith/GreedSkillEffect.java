package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class GreedSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "greed");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (player.level() instanceof ServerLevel serverLevel) {
            double range = 3.0;
            AABB area = player.getBoundingBox().inflate(range);
            List<ItemEntity> items = serverLevel.getEntitiesOfClass(ItemEntity.class, area);

            if (!items.isEmpty()) {
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f,
                        1.0f);
                for (ItemEntity item : items) {
                    item.teleportTo(player.getX(), player.getY(), player.getZ());
                }
            }
        }
    }
}
