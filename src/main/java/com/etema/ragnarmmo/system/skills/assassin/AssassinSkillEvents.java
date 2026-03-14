package com.etema.ragnarmmo.system.skills.assassin;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Assassin skill event hooks.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class AssassinSkillEvents {

    /**
     * Dual Wield and Katar Mastery Hook: Multiplies damage based on masteries.
     */
    @SubscribeEvent
    public static void onAssassinAttack(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            float multiplier = 1.0f;
            ItemStack main = player.getMainHandItem();
            ItemStack offhand = player.getOffhandItem();

            // 1. Katar Mastery (+3% per level with single-handed swords)
            int katarLv = player.getPersistentData().getInt("katar_mastery_level");
            if (katarLv > 0 && main.getItem() instanceof SwordItem && !(main.getItem() instanceof AxeItem)) {
                multiplier += (katarLv * 0.03f);
            }

            // 2. Dual Wield Logic
            int dualLv = player.getPersistentData().getInt("dual_wield_level");
            if (dualLv > 0) {
                boolean isDual = (main.getItem() instanceof SwordItem || main.getItem() instanceof AxeItem) &&
                                 (offhand.getItem() instanceof SwordItem || offhand.getItem() instanceof AxeItem);
                
                if (isDual) {
                    // Base Dual Wield bonus: (20 + level * 10)%
                    float dualBonus = (20 + dualLv * 10) / 100.0f;
                    
                    // Left Hand Mastery adds level * 5% additional to the offhand portion
                    int leftLv = player.getPersistentData().getInt("left_mastery_level");
                    dualBonus += (leftLv * 0.05f);
                    
                    // Right Hand Mastery adds level * 5% additional to the main hand portion
                    int rightLv = player.getPersistentData().getInt("right_mastery_level");
                    float rightBonus = (rightLv * 0.05f);
                    
                    multiplier += dualBonus + rightBonus;
                }
            }

            if (multiplier > 1.0f) {
                event.setAmount(event.getAmount() * multiplier);
            }
        }
    }
}
