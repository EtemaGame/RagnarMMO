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
        if (level <= 0)
            return;

        // Divine Protection: Increases defense against Undead and Demon monsters.
        // RO formula: (3 + (Level * 0.04 * BaseLv)) reduction? Or flat reduction.
        // For Minecraft, we'll use a straight damage reduction percentage or flat
        // value.
        // Let's use a percentage reduction: 3% per level (up to 30% at lvl 10) against
        // Undead/Demon.

        if (event.getSource().getEntity() instanceof Enemy enemy) {
            // Check if Undead or "Demon"
            // In Minecraft, Undead is MobType.UNDEAD.
            // "Demon" isn't a native MobType, but often Illagers or Nether mobs are treated
            // as such.
            // For now, we rely on UNDEAD as the primary check, maybe expand later with
            // tags.
            boolean isUndeadOrDemon = event.getSource().getEntity().getType().is(UNDEAD_TAG)
                    || event.getSource().getEntity().getType().is(RAIDER_TAG);

            if (isUndeadOrDemon) {
                float reductionPercent = SkillRegistry.get(ID)
                        .map(def -> (float) def.getLevelDouble("damage_reduction_percent", level, 3.0D * level))
                        .orElse(3.0f * level);
                float reductionMultiplier = 1.0f - (reductionPercent / 100.0f);
                event.setAmount(event.getAmount() * Math.max(0.1f, reductionMultiplier));
            }
        }
    }
}
