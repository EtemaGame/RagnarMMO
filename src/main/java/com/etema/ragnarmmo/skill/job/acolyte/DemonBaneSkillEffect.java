package com.etema.ragnarmmo.skill.job.acolyte;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;

import java.util.Set;

public class DemonBaneSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "demon_bane");
    private static final TagKey<EntityType<?>> UNDEAD_TAG = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation("minecraft", "undead"));
    private static final TagKey<EntityType<?>> RAIDER_TAG = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation("minecraft", "raider"));

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public Set<TriggerType> getSupportedTriggers() {
        return Set.of(TriggerType.OFFENSIVE_HURT);
    }

    @Override
    public void onOffensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Demon Bane: Increases attack power against Undead and Demon monsters.
        // RO formula: (3 + (Level * 0.05 * BaseLv)) flat damage increase.
        // For Minecraft, we'll use a percentage increase: 4% per level (up to 40% at
        // lvl 10) against Undead/Demon.

        if (event.getEntity() instanceof Enemy enemy) {
            boolean isUndeadOrDemon = event.getEntity().getType().is(UNDEAD_TAG)
                    || event.getEntity().getType().is(RAIDER_TAG);

            if (isUndeadOrDemon) {
                float damageMultiplier = 1.0f + (level * 0.04f); // Max 40% bonus
                event.setAmount(event.getAmount() * damageMultiplier);
            }
        }
    }
}
