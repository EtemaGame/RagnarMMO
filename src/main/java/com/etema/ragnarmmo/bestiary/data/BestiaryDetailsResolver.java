package com.etema.ragnarmmo.bestiary.data;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.bestiary.api.*;
import com.etema.ragnarmmo.common.api.mobs.data.load.MobDefinitionRegistry;
import com.etema.ragnarmmo.items.ZenyItems;
import com.etema.ragnarmmo.items.cards.CardDefinition;
import com.etema.ragnarmmo.items.cards.CardRegistry;
import com.etema.ragnarmmo.items.cards.RagnarCardItems;
import com.etema.ragnarmmo.mobs.profile.AuthoredMobDefinition;
import com.etema.ragnarmmo.mobs.profile.AuthoredMobProfileResolver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BestiaryDetailsResolver {
    private static final int MAX_NATURAL_DROPS = 24;

    private BestiaryDetailsResolver() {
    }

    public static BestiaryEntryDetailsDto resolve(ResourceLocation entityId, BestiaryOverride override, MinecraftServer server) {
        String descriptionId = override != null ? override.descriptionId() : "";
        BestiarySpawnInfoDto spawn = override != null ? override.spawn().orElse(null) : null;
        List<BestiaryDropInfoDto> drops = new ArrayList<>();
        if (override != null) {
            drops.addAll(override.drops());
        }
        drops.addAll(resolveNaturalLootDrops(entityId, server));
        drops.addAll(resolveCardDrops(entityId));
        resolveZenyDrop(entityId).ifPresent(drops::add);

        return new BestiaryEntryDetailsDto(
                entityId,
                descriptionId,
                resolveStats(entityId),
                drops,
                spawn);
    }

    private static List<BestiaryDropInfoDto> resolveNaturalLootDrops(ResourceLocation entityId, MinecraftServer server) {
        if (server == null) {
            return List.of();
        }
        var type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (type == null) {
            return List.of();
        }
        ResourceLocation lootTableId = type.getDefaultLootTable();
        if (lootTableId == null || lootTableId.equals(net.minecraft.world.level.storage.loot.BuiltInLootTables.EMPTY)) {
            return List.of();
        }

        LootTable table = server.getLootData().getLootTable(lootTableId);
        if (table == LootTable.EMPTY) {
            return List.of();
        }

        Set<ResourceLocation> itemIds = new LinkedHashSet<>();
        collectLootItems(server, lootTableId, table, itemIds, new LinkedHashSet<>(), 0);
        return itemIds.stream()
                .limit(MAX_NATURAL_DROPS)
                .map(itemId -> new BestiaryDropInfoDto(
                        itemId,
                        0,
                        0,
                        0.0D,
                        BestiaryDropSource.NATURAL_LOOT_TABLE,
                        "",
                        "gui.ragnarmmo.bestiary.drop.natural_loot"))
                .toList();
    }

    private static void collectLootItems(
            MinecraftServer server,
            ResourceLocation tableId,
            LootTable table,
            Set<ResourceLocation> itemIds,
            Set<ResourceLocation> visitedTables,
            int depth) {
        if (table == null || table == LootTable.EMPTY || depth > 4 || !visitedTables.add(tableId)) {
            return;
        }
        for (LootPool pool : reflectArray(table, "pools", LootPool.class)) {
            for (LootPoolEntryContainer entry : reflectArray(pool, "entries", LootPoolEntryContainer.class)) {
                collectLootEntry(server, entry, itemIds, visitedTables, depth);
            }
        }
    }

    private static void collectLootEntry(
            MinecraftServer server,
            LootPoolEntryContainer entry,
            Set<ResourceLocation> itemIds,
            Set<ResourceLocation> visitedTables,
            int depth) {
        if (entry instanceof LootItem) {
            Item item = reflectField(entry, "item", Item.class);
            ResourceLocation itemId = item != null ? ForgeRegistries.ITEMS.getKey(item) : null;
            if (itemId != null) {
                itemIds.add(itemId);
            }
            return;
        }
        if (entry instanceof LootTableReference) {
            ResourceLocation nestedId = reflectField(entry, "name", ResourceLocation.class);
            if (nestedId != null) {
                LootTable nested = server.getLootData().getLootTable(nestedId);
                collectLootItems(server, nestedId, nested, itemIds, visitedTables, depth + 1);
            }
            return;
        }

        for (LootPoolEntryContainer child : reflectArray(entry, "children", LootPoolEntryContainer.class)) {
            collectLootEntry(server, child, itemIds, visitedTables, depth + 1);
        }
    }

    private static <T> List<T> reflectArray(Object owner, String fieldName, Class<T> type) {
        Object value = reflectField(owner, fieldName, Object.class);
        if (value == null || !value.getClass().isArray()) {
            return List.of();
        }
        int length = java.lang.reflect.Array.getLength(value);
        List<T> out = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            Object item = java.lang.reflect.Array.get(value, i);
            if (type.isInstance(item)) {
                out.add(type.cast(item));
            }
        }
        return out;
    }

    private static <T> T reflectField(Object owner, String fieldName, Class<T> type) {
        Class<?> cls = owner.getClass();
        while (cls != null) {
            try {
                Field field = cls.getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(owner);
                return type.isInstance(value) ? type.cast(value) : null;
            } catch (NoSuchFieldException ignored) {
                cls = cls.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException ex) {
                RagnarMMO.LOGGER.debug("Unable to inspect loot field {} on {}: {}",
                        fieldName, owner.getClass().getName(), ex.getMessage());
                return null;
            }
        }
        return null;
    }

    private static BestiaryStatPreviewDto resolveStats(ResourceLocation entityId) {
        if (MobDefinitionRegistry.getInstance().getDefinition(entityId).isEmpty()) {
            return null;
        }
        AuthoredMobDefinition authored = AuthoredMobProfileResolver.resolvePartialDefinition(entityId).orElse(null);
        if (authored == null
                || authored.baseLevel().isEmpty()
                || authored.baselineRank().isEmpty()
                || authored.tier().isEmpty()
                || authored.race().isEmpty()
                || authored.element().isEmpty()
                || authored.size().isEmpty()
                || authored.baseHp().isEmpty()
                || authored.atkMin().isEmpty()
                || authored.atkMax().isEmpty()
                || authored.def().isEmpty()
                || authored.mdef().isEmpty()) {
            return null;
        }
        return new BestiaryStatPreviewDto(
                true,
                authored.baseLevel().getAsInt(),
                authored.baselineRank().get().name(),
                authored.tier().get().name(),
                authored.race().get(),
                authored.element().get(),
                authored.size().get(),
                authored.baseHp().getAsInt(),
                authored.atkMin().getAsInt(),
                authored.atkMax().getAsInt(),
                authored.def().getAsInt(),
                authored.mdef().getAsInt(),
                true);
    }

    private static List<BestiaryDropInfoDto> resolveCardDrops(ResourceLocation entityId) {
        ResourceLocation cardItemId = ForgeRegistries.ITEMS.getKey(RagnarCardItems.CARD.get());
        if (cardItemId == null) {
            cardItemId = ResourceLocation.fromNamespaceAndPath(RagnarMMO.MODID, "others/cards/card");
        }
        List<BestiaryDropInfoDto> drops = new ArrayList<>();
        for (CardDefinition card : CardRegistry.getInstance().getForMob(entityId.toString())) {
            drops.add(new BestiaryDropInfoDto(
                    cardItemId,
                    1,
                    1,
                    card.dropRate(),
                    BestiaryDropSource.RAGNAR_CARD,
                    card.displayName(),
                    card.translationKey()));
        }
        return drops;
    }

    private static java.util.Optional<BestiaryDropInfoDto> resolveZenyDrop(ResourceLocation entityId) {
        var type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (type == null || MobRewardClassifier.classify(type) != MobRewardDisposition.REWARD_ELIGIBLE) {
            return java.util.Optional.empty();
        }
        ResourceLocation copperId = ForgeRegistries.ITEMS.getKey(ZenyItems.COPPER_ZENY.get());
        if (copperId == null) {
            copperId = ResourceLocation.fromNamespaceAndPath(RagnarMMO.MODID, "others/zeny/copper_zeny");
        }
        return java.util.Optional.of(new BestiaryDropInfoDto(
                copperId,
                0,
                0,
                0.0D,
                BestiaryDropSource.RAGNAR_ZENY,
                "Zeny",
                "gui.ragnarmmo.bestiary.drop.zeny_dynamic"));
    }
}
