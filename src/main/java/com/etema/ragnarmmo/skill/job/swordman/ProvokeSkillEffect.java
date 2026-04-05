package com.etema.ragnarmmo.skill.job.swordman;

import com.etema.ragnarmmo.combat.aggro.AggroManager;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class ProvokeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "provoke");

    // Fixed UUIDs for the Provoke modifiers (Must match AggroEvents)
    private static final UUID PROVOKE_DEF_DEBUFF_UUID = UUID.fromString("a1b2c3d4-dead-beef-cafe-000000000001");
    private static final UUID PROVOKE_ATK_BUFF_UUID = UUID.fromString("a1b2c3d4-dead-beef-cafe-000000000002");
    private static final String PROVOKE_DEF_DEBUFF_NAME = "ragnarmmo:provoke_def_debuff";
    private static final String PROVOKE_ATK_BUFF_NAME = "ragnarmmo:provoke_atk_buff";
    private static final String PROVOKE_UNTIL_TAG = "ragnar_provoke_until";

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        LivingEntity target = getTarget(player);
        if (target == null || target == player)
            return;

        // RO: Provoke does not work on Undead monsters
        if (target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) {
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("§cProvoke failed — target is Undead."));
            return;
        }

        // Provoke only works on Mob entities (not other players)
        if (!(target instanceof Mob mob)) {
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("§cProvoke failed — target cannot be taunted."));
            return;
        }

        // Duration: scales with level. Level 1 = 20s, Level 10 = 40s
        int durationTicks = (20 + level * 2) * 20;

        // --- 1. AGGRO: Force the mob to retarget the casting player ---
        AggroManager.applyAggro(mob, player, durationTicks);
        mob.setTarget(player); // Immediate retarget

        // --- 2. DEF REDUCTION & ATK BUFF ---
        // RO Formulas:
        // DEF Reduction: 10% + 3% * level (max 40%)
        // ATK Increase: 100% + (2 * level + 3)% (max 123%)
        
        var armorAttr = mob.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(PROVOKE_DEF_DEBUFF_UUID); 
            double defReduction = -(0.10 + 0.03 * level);
            armorAttr.addTransientModifier(new AttributeModifier(
                PROVOKE_DEF_DEBUFF_UUID,
                PROVOKE_DEF_DEBUFF_NAME,
                defReduction,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }

        var atkAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null) {
            atkAttr.removeModifier(PROVOKE_ATK_BUFF_UUID);
            // Increase is calculated as a multiplier to the total. 
            // 123% total = +23% addition to the multiplier.
            double atkIncrease = (2.0 * level + 3.0) / 100.0;
            atkAttr.addTransientModifier(new AttributeModifier(
                PROVOKE_ATK_BUFF_UUID,
                PROVOKE_ATK_BUFF_NAME,
                atkIncrease,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }

        // Store expiry time so AggroEvents can clean up both modifiers
        mob.getPersistentData().putLong(PROVOKE_UNTIL_TAG,
            mob.level().getGameTime() + durationTicks);

        // --- 3. FEEDBACK ---
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 0.5f);

        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    target.getX(), target.getY() + 2.0, target.getZ(),
                    8, 0.4, 0.3, 0.4, 0.0);
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    5, 0.3, 0.2, 0.3, 0.0);
        }
    }

    private LivingEntity getTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(5.0)); // 5-block range for Provoke
        AABB searchBox = player.getBoundingBox().inflate(5.0);

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
