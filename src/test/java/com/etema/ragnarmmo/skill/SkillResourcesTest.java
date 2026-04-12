package com.etema.ragnarmmo.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SkillResourcesTest {
    private static final Path SKILL_DIR = Path.of("src/main/resources/data/ragnarmmo/skills");
    private static final Path SKILL_TREE_DIR = Path.of("src/main/resources/data/ragnarmmo/skill_trees");
    private static final List<String> FIRST_JOB_TREES = List.of(
            "archer_1",
            "swordsman_1",
            "mage_1",
            "acolyte_1",
            "thief_1",
            "merchant_1");
    private static final Set<String> USAGE_TYPES = Set.of("ACTIVE", "PASSIVE");
    private static final int EXPECTED_FIRST_JOB_SKILL_COUNT = 54;

    @Test
    void firstJobTreeSkillsHaveCanonicalJsonDefinitions() throws IOException {
        Set<String> canonicalSkillIds = new LinkedHashSet<>();

        for (String treeName : FIRST_JOB_TREES) {
            JsonObject tree = readObject(SKILL_TREE_DIR.resolve(treeName + ".json"));
            String job = tree.get("job").getAsString();
            Set<String> occupiedPositions = new LinkedHashSet<>();

            for (JsonElement skillElement : tree.getAsJsonArray("skills")) {
                JsonObject treeEntry = skillElement.getAsJsonObject();
                String skillId = treeEntry.get("id").getAsString();
                String position = treeEntry.get("x").getAsInt() + "," + treeEntry.get("y").getAsInt();

                assertTrue(occupiedPositions.add(position), () -> treeName + " has duplicate position " + position);
                assertTrue(canonicalSkillIds.add(skillId), () -> "Duplicate first-job skill id: " + skillId);

                Path skillFile = skillFileFor(skillId);
                assertTrue(Files.isRegularFile(skillFile),
                        () -> skillId + " must be authored in JSON, not injected from SkillType fallback");

                JsonObject skill = readObject(skillFile);
                assertEquals(skillId, skill.get("id").getAsString(), skillFile + " id must match tree entry");
                assertTrue(skill.has("display_name"), () -> skillId + " must have display_name");
                assertTrue(USAGE_TYPES.contains(skill.get("usage").getAsString()),
                        () -> skillId + " has invalid usage " + skill.get("usage"));
                assertTrue(skill.has("effect_class"), () -> skillId + " must point to a Java effect during migration");
                assertTrue(skillHasJob(skill, job), () -> skillId + " must include job " + job);
                assertTrue(getMaxLevel(skill) >= 1, () -> skillId + " max level must be >= 1");
            }
        }

        assertEquals(EXPECTED_FIRST_JOB_SKILL_COUNT, canonicalSkillIds.size(),
                "First-job canonical scope changed; update SkillDataLoader and docs/rework together");
    }

    @Test
    void firstJobCanonicalSkillsHaveCompleteLevelData() throws IOException {
        for (String skillId : firstJobSkillIds()) {
            JsonObject skill = readObject(skillFileFor(skillId));
            int maxLevel = getMaxLevel(skill);
            boolean active = "ACTIVE".equals(skill.get("usage").getAsString());

            assertTrue(skill.has("level_data") && skill.get("level_data").isJsonObject(),
                    () -> skillId + " must use level_data as its tuning source");

            JsonObject levelData = skill.getAsJsonObject("level_data");
            for (int level = 1; level <= maxLevel; level++) {
                String key = Integer.toString(level);
                assertTrue(levelData.has(key) && levelData.get(key).isJsonObject(),
                        () -> skillId + " is missing level_data." + key);
                if (active) {
                    JsonObject row = levelData.getAsJsonObject(key);
                    assertHasNumber(row, "sp_cost", skillId, level);
                    assertHasNumber(row, "cast_time_ticks", skillId, level);
                    assertHasNumber(row, "cast_delay_ticks", skillId, level);
                    assertHasNumber(row, "cooldown_ticks", skillId, level);
                }
            }
        }
    }

    @Test
    void vulturesEyeIsAccuracyAndProjectileControlOnly() throws IOException {
        JsonObject skill = readObject(skillFileFor("ragnarmmo:vultures_eye"));
        JsonObject levelData = skill.getAsJsonObject("level_data");

        for (int level = 1; level <= getMaxLevel(skill); level++) {
            JsonObject row = levelData.getAsJsonObject(Integer.toString(level));
            assertFalse(row.has("damage_percent"), "Vulture's Eye must not grant direct damage");
            assertTrue(row.has("accuracy_bonus"), "Vulture's Eye must keep an accuracy tuning value");
        }

        JsonObject level10 = levelData.getAsJsonObject("10");
        assertEquals(10, level10.get("accuracy_bonus").getAsInt());
        assertTrue(level10.get("projectile_velocity_mult").getAsDouble() > 1.0D);
        assertTrue(level10.get("projectile_gravity_mult").getAsDouble() < 1.0D);
        assertTrue(level10.get("projectile_spread_mult").getAsDouble() < 1.0D);
    }

    @Test
    void doubleStrafeKeepsTwoHitPreRenewalShape() throws IOException {
        JsonObject skill = readObject(skillFileFor("ragnarmmo:double_strafe"));
        JsonObject levelData = skill.getAsJsonObject("level_data");

        for (int level = 1; level <= getMaxLevel(skill); level++) {
            JsonObject row = levelData.getAsJsonObject(Integer.toString(level));
            assertEquals(2, row.get("hit_count").getAsInt(), "Double Strafe must stay two logical hits");
            assertEquals(12, row.get("sp_cost").getAsInt(), "Double Strafe SP cost must be table-driven");
            assertEquals(0, row.get("cast_time_ticks").getAsInt(), "Double Strafe should remain instant-cast");
        }

        assertEquals(200, levelData.getAsJsonObject("10").get("damage_percent").getAsInt());
    }

    @Test
    void swordsmanCoreSkillsKeepPreRenewalContracts() throws IOException {
        JsonObject bashLevel10 = levelData("ragnarmmo:bash", 10);
        assertEquals(400, bashLevel10.get("damage_percent").getAsInt());
        assertEquals(15, bashLevel10.get("sp_cost").getAsInt());

        JsonObject provokeLevel10 = levelData("ragnarmmo:provoke", 10);
        assertTrue(provokeLevel10.has("aggro_bonus"), "Provoke must keep its Minecraft aggro adaptation");
        assertEquals(55, provokeLevel10.get("def_reduction_percent").getAsInt());
        assertEquals(32, provokeLevel10.get("attack_bonus_percent").getAsInt());
    }

    @Test
    void mageBoltsKeepTenHitShape() throws IOException {
        for (String skillId : List.of("ragnarmmo:fire_bolt", "ragnarmmo:cold_bolt", "ragnarmmo:lightning_bolt")) {
            JsonObject row = levelData(skillId, 10);
            assertEquals(10, row.get("hit_count").getAsInt(), skillId + " lv10 must use 10 bolt hits");
            assertEquals(0, row.get("cooldown_ticks").getAsInt(), skillId + " cooldown must be row-driven");
        }
    }

    @Test
    void acolyteSupportSkillsKeepExplicitTables() throws IOException {
        JsonObject healLevel10 = levelData("ragnarmmo:heal", 10);
        assertEquals(0.5D, healLevel10.get("undead_damage_ratio").getAsDouble(), 0.0001D);

        JsonObject blessingLevel10 = levelData("ragnarmmo:blessing", 10);
        assertEquals(9, blessingLevel10.get("effect_amplifier").getAsInt());

        JsonObject pneumaLevel1 = levelData("ragnarmmo:pneuma", 1);
        assertTrue(pneumaLevel1.has("aoe_radius"), "Pneuma must keep its tactical protection radius in data");
    }

    @Test
    void thiefSkillsKeepStatusAndWeaponContracts() throws IOException {
        JsonObject doubleAttackLevel10 = levelData("ragnarmmo:double_attack", 10);
        assertEquals(0.50D, doubleAttackLevel10.get("proc_chance").getAsDouble(), 0.0001D);
        assertEquals(2.0D, doubleAttackLevel10.get("damage_multiplier").getAsDouble(), 0.0001D);

        JsonObject envenomLevel10 = levelData("ragnarmmo:envenom", 10);
        assertTrue(envenomLevel10.has("status_chance"), "Envenom must keep poison chance in data");
        assertTrue(envenomLevel10.has("duration_ticks"), "Envenom must keep poison duration in data");
    }

    @Test
    void merchantEconomicSkillsKeepDataDrivenTuning() throws IOException {
        JsonObject discountLevel10 = levelData("ragnarmmo:discount", 10);
        assertEquals(0.24D, discountLevel10.get("vendor_buy_discount").getAsDouble(), 0.0001D);

        JsonObject overchargeLevel10 = levelData("ragnarmmo:overcharge", 10);
        assertEquals(0.24D, overchargeLevel10.get("vendor_sell_bonus").getAsDouble(), 0.0001D);

        JsonObject mammoniteLevel10 = levelData("ragnarmmo:mammonite", 10);
        assertEquals(1000, mammoniteLevel10.get("zeny_cost").getAsInt());
        assertEquals(600, mammoniteLevel10.get("damage_percent").getAsInt());

        JsonObject weightLimitLevel10 = levelData("ragnarmmo:enlarge_weight_limit", 10);
        assertEquals(2000, weightLimitLevel10.get("weight_limit_bonus").getAsInt());
    }

    private static boolean skillHasJob(JsonObject skill, String job) {
        if (!skill.has("jobs") || !skill.get("jobs").isJsonArray()) {
            return false;
        }

        for (JsonElement jobElement : skill.getAsJsonArray("jobs")) {
            if (job.equals(jobElement.getAsString())) {
                return true;
            }
        }

        return false;
    }

    private static Set<String> firstJobSkillIds() throws IOException {
        Set<String> canonicalSkillIds = new LinkedHashSet<>();

        for (String treeName : FIRST_JOB_TREES) {
            JsonObject tree = readObject(SKILL_TREE_DIR.resolve(treeName + ".json"));
            for (JsonElement skillElement : tree.getAsJsonArray("skills")) {
                canonicalSkillIds.add(skillElement.getAsJsonObject().get("id").getAsString());
            }
        }

        return canonicalSkillIds;
    }

    private static JsonObject levelData(String skillId, int level) throws IOException {
        JsonObject skill = readObject(skillFileFor(skillId));
        JsonObject levelData = skill.getAsJsonObject("level_data");
        String levelKey = Integer.toString(level);
        assertTrue(levelData.has(levelKey), () -> skillId + " is missing level_data." + levelKey);
        return levelData.getAsJsonObject(levelKey);
    }

    private static void assertHasNumber(JsonObject row, String key, String skillId, int level) {
        assertTrue(row.has(key) && row.get(key).isJsonPrimitive() && row.get(key).getAsJsonPrimitive().isNumber(),
                () -> skillId + " level_data." + level + " must define numeric " + key);
    }

    private static int getMaxLevel(JsonObject skill) {
        if (skill.has("progression") && skill.get("progression").isJsonObject()) {
            return skill.getAsJsonObject("progression").get("max_level").getAsInt();
        }

        return skill.get("max_level").getAsInt();
    }

    private static Path skillFileFor(String skillId) {
        assertTrue(skillId.startsWith("ragnarmmo:"), () -> "First-job skill id must use ragnarmmo namespace: " + skillId);
        return SKILL_DIR.resolve(skillId.substring("ragnarmmo:".length()) + ".json");
    }

    private static JsonObject readObject(Path file) throws IOException {
        String json = Files.readString(file, StandardCharsets.UTF_8);
        JsonElement element = JsonParser.parseString(json);
        assertTrue(element.isJsonObject(), file + " must be a JSON object");
        return element.getAsJsonObject();
    }
}
