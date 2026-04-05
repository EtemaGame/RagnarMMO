package com.etema.ragnarmmo.roitems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RagnarBowWeaponItem extends BowItem {

    private final String displayName;
    private final String description;

    public RagnarBowWeaponItem(Properties properties, @Nullable String displayName, @Nullable String description) {
        super(properties);
        this.displayName = displayName;
        this.description = description;
    }

    @Override
    public Component getName(ItemStack stack) {
        return TooltipTextHelper.displayName(this.getDescriptionId(), displayName);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        TooltipTextHelper.appendDescription(tooltip, this.getDescriptionId(), description);
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
