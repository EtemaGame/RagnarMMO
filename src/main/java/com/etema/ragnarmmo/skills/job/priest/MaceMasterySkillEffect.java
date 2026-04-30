package com.etema.ragnarmmo.skills.job.priest;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;

/**
 * Mace Mastery — Passive (Priest)
 * RO: Increases ATK when using maces.
 * MC: +2% damage per level with axes and pickaxes (mace equivalents).
 *     HandAttackProfileResolver reads it into the RO attack profile.
 */
public class MaceMasterySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "mace_mastery");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("mace_mastery_level", level);
        if (level > 0) {
            player.sendSystemMessage(Component.literal(
                    "§f✦ Mace Mastery §flv." + level + " — Daño con hachas/picos +" + (level * 2) + "%"));
        }
    }
}
