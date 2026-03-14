package com.etema.ragnarmmo.system.loot;

import com.etema.ragnarmmo.system.loot.cards.CardDefinition;
import com.etema.ragnarmmo.system.loot.cards.CardItem;
import com.etema.ragnarmmo.system.loot.cards.CardRegistry;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class RagnarLootModifier extends LootModifier {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Supplier<Codec<RagnarLootModifier>> CODEC = Suppliers
            .memoize(() -> RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, RagnarLootModifier::new)));

    public RagnarLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
            LootContext context) {

        RandomSource random = context.getRandom();

        // 1. Handle Block Drops (Mining/Woodcutting/Excavation/Farming)
        // In block loot, THIS_ENTITY = the player breaking the block
        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state != null) {
            Entity blockBreaker = context.getParamOrNull(LootContextParams.THIS_ENTITY);
            if (blockBreaker instanceof ServerPlayer player) {
                handleBlockDrops(generatedLoot, context, player, state, random);
            }
            return generatedLoot;
        }

        // 2. Handle Entity Drops (Combat — affix items + card drops)
        // In entity loot, THIS_ENTITY = dying mob, LAST_DAMAGE_PLAYER = the player
        if (context.hasParam(LootContextParams.LAST_DAMAGE_PLAYER)) {
            Entity killer = context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);
            Entity killedMob = context.getParamOrNull(LootContextParams.THIS_ENTITY);
            if (killer instanceof ServerPlayer player && killedMob instanceof LivingEntity mob) {
                handleCombatDrops(generatedLoot, context, player, mob, random);
            }
        }

        return generatedLoot;
    }

    private void handleBlockDrops(ObjectArrayList<ItemStack> loot, LootContext context, ServerPlayer player,
            BlockState state, RandomSource random) {
        // Blocks no longer give Base/Job XP or direct drops via this modifier in
        // RagnarMMO.
        // Life Skill progression is handled by LifeSkillEventHandler.
    }

    private void handleCombatDrops(ObjectArrayList<ItemStack> loot, LootContext context, ServerPlayer player,
            LivingEntity killedMob, RandomSource random) {
        com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
            int luk = stats.getLUK();

            // --- Card drops (RO style) ---
            ResourceLocation mobKey = ForgeRegistries.ENTITY_TYPES.getKey(killedMob.getType());
            if (mobKey != null) {
                try {
                    CardDefinition card = CardRegistry.getInstance().rollDrop(
                            mobKey.toString(), luk, random);
                    if (card != null) {
                        ItemStack cardStack = CardItem.createStack(card);
                        loot.add(cardStack);
                        LOGGER.debug("RagnarMMO: Dropped card '{}' from {}", card.id(), mobKey);
                    }
                } catch (Exception e) {
                    LOGGER.debug("RagnarMMO: Could not generate card drop: {}", e.getMessage());
                }
            }

        });
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
