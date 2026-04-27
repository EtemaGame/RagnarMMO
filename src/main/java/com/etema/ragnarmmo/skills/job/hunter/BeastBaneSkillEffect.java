package com.etema.ragnarmmo.skills.job.hunter;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.runtime.PlayerSkillsProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.resources.ResourceLocation;
import com.etema.ragnarmmo.common.init.RagnarCore;

@Mod.EventBusSubscriber(modid = RagnarCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BeastBaneSkillEffect implements ISkillEffect {

    private static final String SKILL_ID = "ragnarmmo:beast_bane";

    @Override
    public void execute(ServerPlayer player, int currentLevel) {
        // Passive skill, does nothing on cast
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.ragnarmmo.skill_passive"));
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LivingEntity target = event.getEntity();
        MobCategory category = target.getType().getCategory();

        // Apply only to Beasts (Animals/Creatures) and Spiders (Arthropods)
        boolean isBeastOrInsect = (category == MobCategory.CREATURE || category == MobCategory.WATER_CREATURE ||
                target instanceof net.minecraft.world.entity.monster.Spider ||
                target instanceof net.minecraft.world.entity.monster.CaveSpider ||
                target instanceof net.minecraft.world.entity.monster.Silverfish);

        if (!isBeastOrInsect) {
            return;
        }

        int level = player.getCapability(PlayerSkillsProvider.CAP)
                .map(skills -> skills.getSkillLevel(ResourceLocation.parse(SKILL_ID)))
                .orElse(0);
        if (level > 0) {
            // Flat + percentage damage increase against Beasts/Insects
            float baseBoost = level * 4.0F;
            float percentBoost = 1.0F + (level * 0.05F); // Up to 50% extra at level 10

            event.setAmount((event.getAmount() + baseBoost) * percentBoost);
        }
    }
}
