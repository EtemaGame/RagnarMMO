package com.etema.ragnarmmo.skills.job.wizard;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SenseSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.parse("ragnarmmo:sense");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Sense: Displays entity info (HP, level, etc).
        LivingEntity target = getClosestTarget(player, 15.0);
        if (target == null)
            return;

        player.sendSystemMessage(Component.literal("§e--- Entity Info ---"));
        player.sendSystemMessage(Component.literal("§7Name: §f" + target.getName().getString()));
        player.sendSystemMessage(
                Component.literal("§7HP: §a" + (int) target.getHealth() + " / " + (int) target.getMaxHealth()));
        player.sendSystemMessage(Component.literal("§7Type: §f" + target.getType().getDescription().getString()));
        player.sendSystemMessage(Component.literal("§e------------------"));
    }

    private LivingEntity getClosestTarget(ServerPlayer player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && player.hasLineOfSight(e));
        if (targets.isEmpty())
            return null;
        return targets.get(0);
    }
}
