package com.etema.ragnarmmo.system.skills.blacksmith;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Blacksmith skill event hooks.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class BlacksmithSkillEvents {

    /**
     * Damage Bonuses Hook: Iron/Steel Tempering, Research Weaponry, Unfair Trick.
     */
    @SubscribeEvent
    public static void onBlacksmithAttack(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            float multiplier = 1.0f;
            float flatBonus = 0.0f;
            ItemStack held = player.getMainHandItem();

            // 1. Research Weaponry (+0.5 flat per level)
            int researchLv = player.getPersistentData().getInt("research_weaponry_level");
            flatBonus += (researchLv * 0.5f);

            // 2. Tempering
            if (held.getItem() instanceof TieredItem tiered) {
                if (tiered.getTier() == Tiers.IRON || tiered.getTier() == Tiers.STONE) {
                    int ironLv = player.getPersistentData().getInt("iron_tempering_level");
                    multiplier += (ironLv * 0.01f);
                } else if (tiered.getTier() == Tiers.DIAMOND || tiered.getTier() == Tiers.NETHERITE) {
                    int steelLv = player.getPersistentData().getInt("steel_tempering_level");
                    multiplier += (steelLv * 0.015f);
                }
            }

            // 3. Unfair Trick (+level * 3% if target < 50% HP)
            int unfairLv = player.getPersistentData().getInt("unfair_trick_level");
            if (unfairLv > 0 && event.getEntity().getHealth() < (event.getEntity().getMaxHealth() * 0.5f)) {
                multiplier += (unfairLv * 0.03f);
            }

            event.setAmount((event.getAmount() + flatBonus) * multiplier);
        }
    }

    /**
     * Defensive Hook: Skin Tempering (Fire resistance).
     */
    @SubscribeEvent
    public static void onBlacksmithDefend(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            int skinLv = player.getPersistentData().getInt("skin_tempering_level");
            if (skinLv > 0) {
                // Reduction: level * 5% for fire damage
                if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
                    event.setAmount(event.getAmount() * (1.0f - (skinLv * 0.05f)));
                }
            }
        }
    }
}
