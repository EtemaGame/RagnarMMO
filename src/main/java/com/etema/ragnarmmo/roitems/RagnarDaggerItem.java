package com.etema.ragnarmmo.roitems;

import net.minecraft.world.item.Tier;
import org.jetbrains.annotations.Nullable;

public class RagnarDaggerItem extends RagnarSwordLikeItem {
    public RagnarDaggerItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        this(tier, attackDamage, attackSpeed, properties, null, null);
    }

    public RagnarDaggerItem(Tier tier, int attackDamage, float attackSpeed, Properties properties,
            @Nullable String displayName, @Nullable String description) {
        super(tier, attackDamage, attackSpeed, properties, displayName, description);
    }
}
