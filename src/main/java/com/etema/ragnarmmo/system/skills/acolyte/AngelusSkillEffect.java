package com.etema.ragnarmmo.system.skills.acolyte;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Angelus — Passive (Acolyte)
 * RO: Increases DEF of all party members in range.
 * MC: While the skill level is > 0, reduces damage received from all sources
 *     by (level * 4)% via a damage hook. Level stored in PersistentData so
 *     AcolyteSkillEvents can read it passively.
 */
public class AngelusSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "angelus");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Passive — toggle notice
        player.getPersistentData().putInt("angelus_level", level);
        if (level > 0) {
            player.sendSystemMessage(Component.literal(
                    "§6✦ Angelus §flv." + level + " activo — DEF +" + (level * 4) + "%"));
        } else {
            player.sendSystemMessage(Component.literal("§8Angelus desactivado."));
        }
    }
}
