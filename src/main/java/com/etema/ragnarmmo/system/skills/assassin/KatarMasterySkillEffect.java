package com.etema.ragnarmmo.system.skills.assassin;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Katar Mastery — Passive (Assassin)
 * RO: Increases ATK with Katar weapons.
 * MC: +3% damage per level with single-handed swords (Katar equivalent).
 *     Stored in PersistentData. AssassinSkillEvents reads it in LivingHurtEvent.
 */
public class KatarMasterySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "katar_mastery");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("katar_mastery_level", level);
        if (level > 0) {
            player.sendSystemMessage(Component.literal(
                    "§4✦ Katar Mastery §flv." + level + " — Daño con espadas +" + (level * 3) + "%"));
        }
    }
}
