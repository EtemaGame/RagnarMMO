package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
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
