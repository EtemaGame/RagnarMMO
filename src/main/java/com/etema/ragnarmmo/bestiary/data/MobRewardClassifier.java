package com.etema.ragnarmmo.bestiary.data;

import com.etema.ragnarmmo.bestiary.api.MobRewardDisposition;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Slime;
import net.minecraftforge.registries.ForgeRegistries;

public final class MobRewardClassifier {
    private MobRewardClassifier() {
    }

    public static MobRewardDisposition classify(LivingEntity entity) {
        if (entity instanceof Monster
                || entity instanceof WitherBoss
                || entity instanceof EnderDragon
                || entity instanceof Slime
                || entity instanceof Phantom
                || entity instanceof Ghast) {
            return MobRewardDisposition.REWARD_ELIGIBLE;
        }
        return MobRewardDisposition.NO_REWARD;
    }

    public static MobRewardDisposition classify(EntityType<?> type) {
        var key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (key == null) {
            return MobRewardDisposition.UNKNOWN;
        }
        String id = key.toString();
        if (id.equals("minecraft:wither")
                || id.equals("minecraft:ender_dragon")
                || id.equals("minecraft:slime")
                || id.equals("minecraft:magma_cube")
                || id.equals("minecraft:phantom")
                || id.equals("minecraft:ghast")
                || type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {
            return MobRewardDisposition.REWARD_ELIGIBLE;
        }
        return MobRewardDisposition.NO_REWARD;
    }
}
