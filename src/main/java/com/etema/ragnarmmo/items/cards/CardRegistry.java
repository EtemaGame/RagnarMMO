package com.etema.ragnarmmo.items.cards;

import com.mojang.logging.LogUtils;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of card definitions loaded from JSON data packs.
 * Provides lookup by id and by mob entity type, plus weighted drop rolling.
 */
public final class CardRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final CardRegistry INSTANCE = new CardRegistry();

    /** All cards indexed by their unique id. */
    private final Map<String, CardDefinition> byId = new ConcurrentHashMap<>();

    /**
     * Cards indexed by mob registry key (e.g. "minecraft:zombie"). Multiple cards
     * can share a mob.
     */
    private final Map<String, List<CardDefinition>> byMob = new ConcurrentHashMap<>();

    private CardRegistry() {
    }

    public static CardRegistry getInstance() {
        return INSTANCE;
    }

    public void clear() {
        byId.clear();
        byMob.clear();
    }

    public void register(CardDefinition def) {
        byId.put(def.id(), def);
        byMob.computeIfAbsent(def.mobId(), k -> new ArrayList<>()).add(def);
        LOGGER.debug("Registered card: {} (mob: {}, rate: {}, equipType: {})",
                def.id(), def.mobId(), def.dropRate(), def.equipType());
    }

    public int size() {
        return byId.size();
    }

    @Nullable
    public CardDefinition get(String cardId) {
        return byId.get(cardId);
    }

    /**
     * Returns all card definitions that can drop from the given mob.
     */
    public List<CardDefinition> getForMob(String mobId) {
        return byMob.getOrDefault(mobId, List.of());
    }

    /**
     * Rolls a card drop for the given mob, factoring in LUK.
     * LUK scaling: effective rate = baseRate * (1 + luk * 0.01)
     *
     * @param mobId  registry key of the killed mob (e.g. "minecraft:zombie")
     * @param luk    player's LUK stat
     * @param random random source
     * @return the dropped card definition, or null if no card drops
     */
    @Nullable
    public CardDefinition rollDrop(String mobId, int luk, RandomSource random) {
        List<CardDefinition> candidates = getForMob(mobId);
        if (candidates.isEmpty())
            return null;

        double lukMultiplier = 1.0 + (luk * 0.01);

        for (CardDefinition card : candidates) {
            double effectiveRate = card.dropRate() * lukMultiplier;
            if (random.nextDouble() < effectiveRate) {
                return card;
            }
        }

        return null;
    }
}
