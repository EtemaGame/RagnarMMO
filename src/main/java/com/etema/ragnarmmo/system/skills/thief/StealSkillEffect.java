package com.etema.ragnarmmo.system.skills.thief;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class StealSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "steal");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        LivingEntity target = getMeleeTarget(player);
        if (target == null)
            return;

        // Cannot steal from players
        if (target instanceof Player) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Cannot steal from players."));
            return;
        }

        // Check if already stolen from (Custom NBT check would go here later)
        if (target.getTags().contains("ragnarmmo_stolen")) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Already stolen from this target."));
            return;
        }

        // Steal Logic implementation anchor.
        // In full impl, this hooks into proper loot table generation using LootContext
        // and spawns an ItemStack at the target's feet, then tags the mob.

        // For now: Chance visual logic
        float chance = 0.1f + (level * 0.05f); // 15% to 60% base chance

        if (player.getRandom().nextFloat() < chance) {
            target.addTag("ragnarmmo_stolen");
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0f, 1.2f);

            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1.0,
                        target.getZ(), 10, 0.4, 0.4, 0.4, 0.1);
            }
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("Steal Successful! (Loot injection pending)"));
        } else {
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Steal Failed."));
        }
    }

    private LivingEntity getMeleeTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(3.0));

        AABB searchBox = player.getBoundingBox().inflate(3.0);
        List<LivingEntity> possibleTargets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        LivingEntity closestTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity e : possibleTargets) {
            AABB targetBox = e.getBoundingBox().inflate(e.getPickRadius() + 0.5);
            var hitOpt = targetBox.clip(start, end);
            if (hitOpt.isPresent() || targetBox.contains(start)) {
                double dist = start.distanceToSqr(e.position());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestTarget = e;
                }
            }
        }
        return closestTarget;
    }
}
