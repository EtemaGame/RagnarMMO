package com.etema.ragnarmmo.skills.job.thief;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.data.progression.SkillProgressManager;
import com.etema.ragnarmmo.skills.data.progression.SkillProgress;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

@Mod.EventBusSubscriber(modid = "ragnarmmo")
public class DoubleAttackSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "double_attack");
    private static final TagKey<Item> DAGGER_TAG = ItemTags.create(ResourceLocation.fromNamespaceAndPath("ragnarmmo", "daggers"));

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    // Double Attack: 5% to 50% chance to attack twice when using a Dagger.
    // We hook into LivingHurtEvent to artificially double the damage and play an
    // effect if proc'd.

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player))
            return;

        SkillProgress progress = SkillProgressManager.getProgress(player, ID);
        if (progress != null && progress.getLevel() > 0) {
            if (!player.getMainHandItem().is(DAGGER_TAG))
                return;

            float chance = SkillRegistry.get(ID)
                    .map(def -> (float) def.getLevelDouble("proc_chance", progress.getLevel(),
                            progress.getLevel() * 0.05D))
                    .orElse(progress.getLevel() * 0.05f);

            if (player.getRandom().nextFloat() < chance) {
                float damageMultiplier = SkillRegistry.get(ID)
                        .map(def -> (float) def.getLevelDouble("damage_multiplier", progress.getLevel(), 2.0D))
                        .orElse(2.0f);

                // Visual / Audio feedback
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.5f);

                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, event.getEntity().getX(),
                            event.getEntity().getY() + 1.0, event.getEntity().getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                    serverLevel.sendParticles(ParticleTypes.CRIT, event.getEntity().getX(),
                            event.getEntity().getY() + 1.0, event.getEntity().getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }
    }
}
