package com.etema.ragnarmmo.mobs.world;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.items.UtilityItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class BossSigilItem extends Item {

    private static final String TAG_ENTITY_TYPE = "BossEntityType";
    private static final String TAG_RANK = "BossRank";
    private static final String TAG_SPAWN_KEY = "BossSpawnKey";
    private static final String TAG_RESPAWN_SECONDS = "BossRespawnSeconds";

    public BossSigilItem(Properties props) {
        super(props);
    }

    public static ItemStack createConfiguredStack(
            EntityType<?> entityType,
            MobRank rank,
            String spawnKey,
            int respawnSeconds) {
        ItemStack stack = new ItemStack(UtilityItems.BOSS_SIGIL.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_ENTITY_TYPE, ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString());
        tag.putString(TAG_RANK, rank.name());
        tag.putString(TAG_SPAWN_KEY, sanitizeSpawnKey(spawnKey));
        tag.putInt(TAG_RESPAWN_SECONDS, Math.max(0, respawnSeconds));
        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState state = level.getBlockState(clickedPos);
        if (!state.is(Blocks.LODESTONE)) {
            return InteractionResult.PASS;
        }
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.sidedSuccess(true);
        }

        Optional<EntityType<?>> entityType = getConfiguredEntityType(context.getItemInHand());
        Optional<MobRank> rank = getConfiguredRank(context.getItemInHand());
        String spawnKey = getSpawnKey(context.getItemInHand());
        int respawnSeconds = getRespawnSeconds(context.getItemInHand());

        if (entityType.isEmpty() || rank.isEmpty() || spawnKey.isBlank()) {
            sendPlayerMessage(context, Component.translatable("tooltip.ragnarmmo.boss_sigil.unconfigured")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        BlockPos spawnPos = clickedPos.above();
        if (!serverLevel.getBlockState(spawnPos).canBeReplaced()) {
            sendPlayerMessage(context, Component.translatable("tooltip.ragnarmmo.boss_sigil.space_blocked")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        BossSpawnService.SpawnResult result = BossSpawnService.spawnControlledBoss(
                serverLevel,
                spawnPos,
                entityType.get(),
                rank.get(),
                BossSpawnSource.ALTAR,
                spawnKey,
                respawnSeconds);
        if (!result.success()) {
            sendPlayerMessage(context, Component.literal(result.message()).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        sendPlayerMessage(context, Component.translatable("tooltip.ragnarmmo.boss_sigil.spawned", result.spawnKey())
                .withStyle(ChatFormatting.GREEN));
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        Optional<EntityType<?>> entityType = getConfiguredEntityType(stack);
        Optional<MobRank> rank = getConfiguredRank(stack);
        String spawnKey = getSpawnKey(stack);
        int respawnSeconds = getRespawnSeconds(stack);

        if (entityType.isEmpty() || rank.isEmpty() || spawnKey.isBlank()) {
            tooltip.add(Component.translatable("tooltip.ragnarmmo.boss_sigil.unconfigured")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        tooltip.add(Component.translatable("tooltip.ragnarmmo.boss_sigil.entity", entityType.get().getDescription())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ragnarmmo.boss_sigil.tier", formatRank(rank.get()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ragnarmmo.boss_sigil.spawn_key", spawnKey)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ragnarmmo.boss_sigil.respawn", respawnSeconds)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ragnarmmo.boss_sigil.use_lodestone")
                .withStyle(ChatFormatting.BLUE));
    }

    @Override
    public Component getName(ItemStack stack) {
        Optional<EntityType<?>> entityType = getConfiguredEntityType(stack);
        Optional<MobRank> rank = getConfiguredRank(stack);
        if (entityType.isPresent() && rank.isPresent()) {
            return Component.translatable(
                    "item.ragnarmmo.utility.boss_sigil.named",
                    formatRank(rank.get()),
                    entityType.get().getDescription());
        }
        return super.getName(stack);
    }

    public static Optional<EntityType<?>> getConfiguredEntityType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ENTITY_TYPE)) {
            return Optional.empty();
        }

        ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_ENTITY_TYPE));
        if (id == null || !ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ForgeRegistries.ENTITY_TYPES.getValue(id));
    }

    public static Optional<MobRank> getConfiguredRank(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return Optional.empty();
        }

        try {
            if (tag.contains(TAG_RANK)) {
                return Optional.of(MobRank.valueOf(tag.getString(TAG_RANK)));
            }
            if (tag.contains("BossTier")) {
                return Optional.of(MobRank.valueOf(tag.getString("BossTier")));
            }
            return Optional.empty();
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static String getSpawnKey(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SPAWN_KEY)) {
            return "";
        }
        return sanitizeSpawnKey(tag.getString(TAG_SPAWN_KEY));
    }

    public static int getRespawnSeconds(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_RESPAWN_SECONDS)) {
            return 0;
        }
        return Math.max(0, tag.getInt(TAG_RESPAWN_SECONDS));
    }

    private static void sendPlayerMessage(UseOnContext context, Component message) {
        if (context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(message, true);
        }
    }

    private static String sanitizeSpawnKey(String spawnKey) {
        return spawnKey == null ? "" : spawnKey.trim().toLowerCase(Locale.ROOT);
    }

    private static Component formatRank(MobRank rank) {
        return Component.literal(BossRankRules.displayName(rank));
    }
}
