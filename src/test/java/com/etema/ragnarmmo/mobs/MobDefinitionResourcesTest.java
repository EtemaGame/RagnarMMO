package com.etema.ragnarmmo.mobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.data.MobDefinition;
import com.etema.ragnarmmo.common.api.mobs.data.MobDirectStatsBlock;
import com.etema.ragnarmmo.common.api.mobs.data.MobRoStatsBlock;
import com.etema.ragnarmmo.common.api.mobs.data.load.MobDefinitionRegistry;
import com.etema.ragnarmmo.common.api.mobs.runtime.resolve.ManualMobProfileResolver;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Resources directory 'mob_definitions' is missing in this environment")
class MobDefinitionResourcesTest {
    private static final Path DEFINITIONS_DIR =
            Path.of("src/main/resources/data/ragnarmmo/mob_definitions/vanilla");

    private static final Set<String> EXPECTED_HOSTILE_VANILLA_MOBS = Set.of(
            "minecraft:blaze",
            "minecraft:cave_spider",
            "minecraft:creeper",
            "minecraft:drowned",
            "minecraft:elder_guardian",
            "minecraft:ender_dragon",
            "minecraft:enderman",
            "minecraft:endermite",
            "minecraft:evoker",
            "minecraft:ghast",
            "minecraft:guardian",
            "minecraft:hoglin",
            "minecraft:husk",
            "minecraft:illusioner",
            "minecraft:magma_cube",
            "minecraft:phantom",
            "minecraft:piglin",
            "minecraft:piglin_brute",
            "minecraft:pillager",
            "minecraft:ravager",
            "minecraft:shulker",
            "minecraft:silverfish",
            "minecraft:skeleton",
            "minecraft:slime",
            "minecraft:spider",
            "minecraft:stray",
            "minecraft:vex",
            "minecraft:vindicator",
            "minecraft:warden",
            "minecraft:witch",
            "minecraft:wither",
            "minecraft:wither_skeleton",
            "minecraft:zoglin",
            "minecraft:zombie",
            "minecraft:zombie_villager",
            "minecraft:zombified_piglin");

    private static final Set<String> TOP_LEVEL_FIELDS = Set.of(
            "entity", "rank", "level", "ro_stats", "direct_stats", "race", "element", "size");
    private static final Set<String> RANKS = Set.of("NORMAL", "ELITE", "BOSS");
    private static final Set<String> RO_STATS_FIELDS = Set.of("str", "agi", "vit", "int", "dex", "luk");
    private static final Set<String> DIRECT_STATS_FIELDS = Set.of(
            "max_hp", "atk_min", "atk_max", "def", "mdef", "aspd", "move_speed");
    private static final Set<String> RACES = Set.of(
            "demihuman", "brute", "insect", "fish", "demon", "undead", "formless", "plant", "angel", "dragon");
    private static final Set<String> ELEMENTS = Set.of(
            "neutral", "water", "earth", "fire", "wind", "poison", "holy", "dark", "ghost", "undead");
    private static final Set<String> SIZES = Set.of("small", "medium", "large");

    @Test
    void vanillaHostileMobDefinitionsAreComplete() throws IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.list(DEFINITIONS_DIR)) {
            files = stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }

        Set<String> entities = new LinkedHashSet<>();
        for (Path file : files) {
            JsonObject definition = readObject(file);
            assertEquals(TOP_LEVEL_FIELDS, definition.keySet(), file + " top-level fields drifted");

            String entity = definition.get("entity").getAsString();
            assertTrue(entities.add(entity), () -> "Duplicate entity definition: " + entity);
            assertTrue(entity.startsWith("minecraft:"), file + " must target a vanilla minecraft entity");
            assertEquals(fileNameWithoutExtension(file), entity.substring("minecraft:".length()),
                    file + " should be named after its exact entity id");

            assertTrue(RANKS.contains(definition.get("rank").getAsString()), file + " has invalid rank");
            assertTrue(definition.get("level").getAsInt() >= 1, file + " level must be >= 1");
            assertTrue(RACES.contains(definition.get("race").getAsString()), file + " has invalid race");
            assertTrue(ELEMENTS.contains(definition.get("element").getAsString()), file + " has invalid element");
            assertTrue(SIZES.contains(definition.get("size").getAsString()), file + " has invalid size");

            JsonObject roStats = definition.getAsJsonObject("ro_stats");
            assertEquals(RO_STATS_FIELDS, roStats.keySet(), file + " must keep complete ro_stats");
            for (String field : RO_STATS_FIELDS) {
                assertTrue(roStats.get(field).getAsInt() >= 0, file + " ro_stats." + field + " must be >= 0");
            }

            JsonObject directStats = definition.getAsJsonObject("direct_stats");
            assertEquals(DIRECT_STATS_FIELDS, directStats.keySet(), file + " must keep complete direct_stats");
            assertTrue(directStats.get("max_hp").getAsInt() > 0, file + " max_hp must be > 0");
            assertTrue(directStats.get("atk_min").getAsInt() >= 0, file + " atk_min must be >= 0");
            assertTrue(directStats.get("atk_max").getAsInt() >= directStats.get("atk_min").getAsInt(),
                    file + " atk_max must be >= atk_min");
            assertTrue(directStats.get("def").getAsInt() >= 0, file + " def must be >= 0");
            assertTrue(directStats.get("mdef").getAsInt() >= 0, file + " mdef must be >= 0");
            assertTrue(directStats.get("aspd").getAsInt() > 0, file + " aspd must be > 0");
            assertTrue(directStats.get("move_speed").getAsDouble() > 0.0D, file + " move_speed must be > 0");
        }

        assertEquals(EXPECTED_HOSTILE_VANILLA_MOBS, entities, "vanilla hostile mob coverage changed");
    }

    @Test
    void mobDefinitionResolverDerivesPreRenewalMobHitFleeAndCrit() {
        ResourceLocation entityId = requireResourceLocation("minecraft:piglin");
        MobDefinition definition = new MobDefinition(
                entityId,
                null,
                MobRank.NORMAL,
                18,
                new MobRoStatsBlock(22, 18, 16, 10, 22, 7),
                new MobDirectStatsBlock(16, 4, 5, 0, 3, null, null, null, 155, 0.35D),
                "demihuman",
                "neutral",
                "medium");

        MobDefinitionRegistry registry = MobDefinitionRegistry.getInstance();
        var previousTemplatesById = registry.getTemplatesById();
        var previousDefinitionsById = registry.getDefinitionsById();
        var previousDefinitionsByEntityTypeId = registry.getDefinitionsByEntityTypeId();
        var previousLoadIssues = registry.getLoadIssues();
        registry.replace(Map.of(), Map.of(entityId, definition), Map.of(entityId, definition), List.of());
        try {
            var result = ManualMobProfileResolver.resolve(entityId, registry);
            assertTrue(result.isSuccess(), () -> "Expected authored mob profile to resolve: " + result.issues());
            assertNotNull(result.profile());
            assertEquals(40, result.profile().hit(), "authored mob HIT should derive as level + dex");
            assertEquals(36, result.profile().flee(), "authored mob FLEE should derive as level + agi");
            assertEquals(0, result.profile().crit(), "normal authored mob crit should not derive from LUK");
        } finally {
            registry.replace(
                    previousTemplatesById,
                    previousDefinitionsById,
                    previousDefinitionsByEntityTypeId,
                    previousLoadIssues);
        }
    }

    private static JsonObject readObject(Path file) throws IOException {
        String json = Files.readString(file, StandardCharsets.UTF_8);
        JsonElement element = JsonParser.parseString(json);
        assertTrue(element.isJsonObject(), file + " must be a JSON object");
        return element.getAsJsonObject();
    }

    private static String fileNameWithoutExtension(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - ".json".length());
    }

    private static ResourceLocation requireResourceLocation(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        assertNotNull(location, () -> "Invalid resource location in test: " + id);
        return location;
    }
}
