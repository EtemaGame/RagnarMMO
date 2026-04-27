package com.etema.ragnarmmo.skills.job.acolyte;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
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

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "demon_bane");
    private static final TagKey<EntityType<?>> UNDEAD_TAG = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("minecraft", "undead"));
    private static final TagKey<EntityType<?>> RAIDER_TAG = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("minecraft", "raider"));

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
                float damageBonusPercent = SkillRegistry.get(ID)
                        .map(def -> (float) def.getLevelDouble("damage_bonus_percent", level, 4.0D * level))
                        .orElse(4.0f * level);
                float damageMultiplier = 1.0f + (damageBonusPercent / 100.0f);
                event.setAmount(event.getAmount() * damageMultiplier);
            }
        }
    }
}
