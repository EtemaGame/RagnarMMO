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

public class DivineProtectionSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "divine_protection");
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
        return Set.of(TriggerType.DEFENSIVE_HURT);
    }

    @Override
    public void onDefensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }
}
