package com.etema.ragnarmmo.skill.job.thief;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
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

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "steal");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        var definition = SkillRegistry.require(ID);
        LivingEntity target = getMeleeTarget(player, definition.getLevelDouble("range", level, 3.0D));
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
        // SuccessRate = [DropRatio * (DEX_difference + 5 * Skill_Lvl + 10)] / 100
        // Simplified for MC: (level * 5 + 10 + player_DEX / 2)%
        int playerDex = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.getDEX(player);
        float chance = (float) definition.getLevelDouble("base_success_chance", level, (level * 5.0D + 10.0D) / 100.0D)
                + (float) (playerDex * definition.getLevelDouble("dex_success_ratio", level, 0.005D));

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

    private LivingEntity getMeleeTarget(Player player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(range));

        AABB searchBox = player.getBoundingBox().inflate(range);
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
