package com.etema.ragnarmmo.roitems;

import com.etema.ragnarmmo.client.render.item.RagnarShieldItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

public class RagnarShieldWeaponItem extends ShieldItem {

    private final String displayName;
    private final String description;

    public RagnarShieldWeaponItem(Properties properties, @Nullable String displayName, @Nullable String description) {
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

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return RagnarShieldItemRenderer.INSTANCE;
            }
        });
    }
}
