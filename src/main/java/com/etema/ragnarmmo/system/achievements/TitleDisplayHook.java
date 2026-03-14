package com.etema.ragnarmmo.system.achievements;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.system.achievements.capability.PlayerAchievementsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Prepends the player's active title to their displayed name in chat and
 * nametags.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TitleDisplayHook {

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        event.getEntity().getCapability(PlayerAchievementsProvider.PLAYER_ACHIEVEMENTS).ifPresent(cap -> {
            String title = cap.getActiveTitle();
            if (title != null && !title.isEmpty()) {
                MutableComponent titleComp = Component.literal("[" + title + "] ")
                        .withStyle(net.minecraft.ChatFormatting.GOLD);

                Component newName = titleComp.append(event.getDisplayname());
                event.setDisplayname(newName);
            }
        });
    }
}
