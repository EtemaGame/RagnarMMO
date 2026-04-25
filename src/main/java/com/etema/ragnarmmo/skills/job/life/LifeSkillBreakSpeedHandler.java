package com.etema.ragnarmmo.skills.job.life;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import com.etema.ragnarmmo.skills.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.skills.runtime.SourceConfig;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Applies life-skill break speed bonuses using {@link PlayerEvent.BreakSpeed}.
 *
 * Attribute modifiers like {@code ATTACK_SPEED} do not affect block breaking speed.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class LifeSkillBreakSpeedHandler {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player == null) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        Item item = held.getItem();
        LifeSkillType skill = getSkillForTool(item);
        if (skill == null) {
            return;
        }

        BlockState state = event.getState();
        ResourceLocation skillId = ResourceLocation.fromNamespaceAndPath(RagnarMMO.MODID, skill.getId());
        int xp = SourceConfig.getInstance().getXp(state, skillId);
        if (xp <= 0) {
            return;
        }

        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            int level = skills.getSkillLevel(skillId);
            if (level <= 0) {
                return;
            }

            // Break speed bonus: +3% per level.
            float multiplier = 1.0f + (level * 0.03f);
            event.setNewSpeed(event.getNewSpeed() * multiplier);
        });
    }

    private static LifeSkillType getSkillForTool(Item item) {
        if (item instanceof PickaxeItem) {
            return LifeSkillType.MINING;
        }
        if (item instanceof AxeItem) {
            return LifeSkillType.WOODCUTTING;
        }
        if (item instanceof ShovelItem) {
            return LifeSkillType.EXCAVATION;
        }
        if (item instanceof HoeItem) {
            return LifeSkillType.FARMING;
        }
        return null;
    }
}

