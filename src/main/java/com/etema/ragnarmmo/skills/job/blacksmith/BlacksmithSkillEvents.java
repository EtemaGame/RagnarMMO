package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Blacksmith skill event hooks.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class BlacksmithSkillEvents {

    private static final String MOD_ID = "ragnarmmo";

    private static ResourceLocation skillId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

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
            int researchLv = com.etema.ragnarmmo.skills.data.progression.SkillProgressManager.getProgress(player, skillId("research_weaponry")).getLevel();
            flatBonus += (researchLv * 0.5f);

            // 2. Tempering
            if (held.getItem() instanceof TieredItem tiered) {
                if (tiered.getTier() == Tiers.IRON || tiered.getTier() == Tiers.STONE) {
                    int ironLv = com.etema.ragnarmmo.skills.data.progression.SkillProgressManager.getProgress(player, skillId("iron_tempering")).getLevel();
                    multiplier += (ironLv * 0.01f);
                } else if (tiered.getTier() == Tiers.DIAMOND || tiered.getTier() == Tiers.NETHERITE) {
                    int steelLv = com.etema.ragnarmmo.skills.data.progression.SkillProgressManager.getProgress(player, skillId("steel_tempering")).getLevel();
                    multiplier += (steelLv * 0.015f);
                }
            }

            // 3. Unfair Trick (+level * 3% if target < 50% HP)
            int unfairLv = com.etema.ragnarmmo.skills.data.progression.SkillProgressManager.getProgress(player, skillId("unfair_trick")).getLevel();
            if (unfairLv > 0 && event.getEntity().getHealth() < (event.getEntity().getMaxHealth() * 0.5f)) {
                multiplier += (unfairLv * 0.03f);
            }

            // 4. Over Thrust (+level * 5% ATK)
            if (player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST)) {
                int otLv = player.getPersistentData().getInt("ragnarmmo_over_thrust_level");
                if (otLv > 0) {
                    multiplier += (otLv * 0.05f);
                }
            }

        }
    }

    /**
     * Defensive Hook: Skin Tempering (Fire resistance).
     */
    @SubscribeEvent
    public static void onBlacksmithDefend(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            int skinLv = com.etema.ragnarmmo.skills.data.progression.SkillProgressManager.getProgress(player, skillId("skin_tempering")).getLevel();
            if (skinLv > 0) {
                // Reduction: level * 5% for fire damage
                if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
                }
            }
        }
    }
}
