package com.etema.ragnarmmo.system.mobstats.config;

import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.system.mobstats.RagnarMobStats;
import com.etema.ragnarmmo.common.api.stats.StatKeys;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carga configuraciones por especie desde mob_species.toml.
 * Este formato soporta:
 * - baseLevel, levelVariance, maxLevel
 * - tier / tiers
 * - pointsPerLevel
 * - growth: { str, agi, vit, int, dex, luk }
 * - multipliers: { health, damage, defense, speed }
 * - random: true/false
 */
public final class SpeciesConfig {

    public static final class SpeciesSettings {
        public static final SpeciesSettings EMPTY = new SpeciesSettings(
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                Optional.empty(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                false);

        private final OptionalInt baseLevel;
        private final OptionalInt levelVariance;
        private final OptionalInt maxLevel;
        private final OptionalInt pointsPerLevel;
        private final Optional<MobTier> forcedTier;
        private final Map<MobTier, Double> tierWeights;
        private final Map<StatKeys, Integer> statWeights;
        private final OptionalDouble healthMultiplier;
        private final OptionalDouble damageMultiplier;
        private final OptionalDouble defenseMultiplier;
        private final OptionalDouble speedMultiplier;
        private final boolean randomDistribution;

        public SpeciesSettings(OptionalInt baseLevel,
                OptionalInt levelVariance,
                OptionalInt maxLevel,
                OptionalInt pointsPerLevel,
                Optional<MobTier> forcedTier,
                Map<MobTier, Double> tierWeights,
                Map<StatKeys, Integer> statWeights,
                OptionalDouble healthMultiplier,
                OptionalDouble damageMultiplier,
                OptionalDouble defenseMultiplier,
                OptionalDouble speedMultiplier,
                boolean randomDistribution) {
            this.baseLevel = baseLevel;
            this.levelVariance = levelVariance;
            this.maxLevel = maxLevel;
            this.pointsPerLevel = pointsPerLevel;
            this.forcedTier = forcedTier;
            this.tierWeights = tierWeights;
            this.statWeights = statWeights;
            this.healthMultiplier = healthMultiplier;
            this.damageMultiplier = damageMultiplier;
            this.defenseMultiplier = defenseMultiplier;
            this.speedMultiplier = speedMultiplier;
            this.randomDistribution = randomDistribution;
        }

        public OptionalInt baseLevel() {
            return baseLevel;
        }

        public OptionalInt levelVariance() {
            return levelVariance;
        }

        public OptionalInt maxLevel() {
            return maxLevel;
        }

        public OptionalInt pointsPerLevel() {
            return pointsPerLevel;
        }

        public Optional<MobTier> forcedTier() {
            return forcedTier;
        }

        public Map<MobTier, Double> tierWeights() {
            return tierWeights;
        }

        public Map<StatKeys, Integer> statWeights() {
            return statWeights;
        }

        public OptionalDouble healthMultiplier() {
            return healthMultiplier;
        }

        public OptionalDouble damageMultiplier() {
            return damageMultiplier;
        }

        public OptionalDouble defenseMultiplier() {
            return defenseMultiplier;
        }

        public OptionalDouble speedMultiplier() {
            return speedMultiplier;
        }

        public boolean randomDistribution() {
            return randomDistribution;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("RagnarMobStats/SpeciesConfig");
    private static final Map<ResourceLocation, SpeciesSettings> SPECIES = new ConcurrentHashMap<>();

    private SpeciesConfig() {
    }

    // === CARGA ===
    public static void init() {
        reload();
    }

    public static void reload() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(RagnarMobStats.MOD_ID);
        try {
            Files.createDirectories(configDir);
        } catch (IOException ex) {
            LOGGER.error("Could not create configuration directory {}", configDir, ex);
            return;
        }

        Path tomlFile = configDir.resolve("mob_species.toml");
        ensureTemplateExists(tomlFile);
        Map<ResourceLocation, SpeciesSettings> loaded;

        if (Files.exists(tomlFile)) {
            loaded = readToml(tomlFile);
        } else {
            LOGGER.warn("mob_species.toml not found, using empty default configuration");
            loaded = Collections.emptyMap();
        }

        SPECIES.clear();
        loaded.forEach(SPECIES::put);
        LOGGER.info("Loaded {} mob configurations (TOML)", SPECIES.size());
    }

    private static void ensureTemplateExists(Path tomlFile) {
        if (Files.exists(tomlFile)) {
            return;
        }

        try {
            Files.writeString(tomlFile, defaultTemplate(), StandardCharsets.UTF_8);
            LOGGER.info("Created default mob species template at {}", tomlFile);
        } catch (IOException ex) {
            LOGGER.warn("Could not create default mob_species.toml at {}", tomlFile, ex);
        }
    }

    public static SpeciesSettings get(ResourceLocation id) {
        return SPECIES.getOrDefault(id, SpeciesSettings.EMPTY);
    }

    // === LECTOR TOML ===
    private static Map<ResourceLocation, SpeciesSettings> readToml(Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            Map<ResourceLocation, SpeciesSettings> result = new LinkedHashMap<>();
            Map<String, Object> current = new LinkedHashMap<>();
            String currentKey = null;

            for (int i = 0; i < lines.size(); i++) {
                String raw = lines.get(i);
                if (raw == null)
                    continue;

                String trimmed = raw.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//"))
                    continue;

                if (trimmed.startsWith("[")) {
                    if (currentKey != null) {
                        ResourceLocation id = ResourceLocation.tryParse(currentKey);
                        if (id != null)
                            result.put(id, toSettings(current));
                        else
                            LOGGER.warn("Invalid mob ID '{}' in {}", currentKey, file);
                    }

                    int end = trimmed.indexOf(']');
                    if (end <= 1) {
                        LOGGER.warn("Invalid TOML section at line {}", i + 1);
                        currentKey = null;
                        current = new LinkedHashMap<>();
                        continue;
                    }

                    currentKey = trimmed.substring(1, end).trim();
                    current = new LinkedHashMap<>();
                    continue;
                }

                int eq = trimmed.indexOf('=');
                if (eq < 0) {
                    LOGGER.warn("Invalid TOML entry at line {}", i + 1);
                    continue;
                }

                String key = trimmed.substring(0, eq).trim();
                String rest = trimmed.substring(eq + 1).trim();
                Object value = rest.startsWith("{") && rest.endsWith("}")
                        ? parseInlineMap(rest)
                        : parseScalar(rest);
                putTomlValue(current, key, value);
            }

            if (currentKey != null) {
                ResourceLocation id = ResourceLocation.tryParse(currentKey);
                if (id != null)
                    result.put(id, toSettings(current));
            }

            return result;
        } catch (IOException ex) {
            LOGGER.error("Error reading {}", file, ex);
            return Collections.emptyMap();
        }
    }

    private static void putTomlValue(Map<String, Object> map, String key, Object value) {
        int dot = key.indexOf('.');
        if (dot >= 0) {
            String parent = key.substring(0, dot).trim();
            String child = key.substring(dot + 1).trim();
            Map<String, Object> nested = castMap(map.computeIfAbsent(parent, k -> new LinkedHashMap<>()));
            putTomlValue(nested, child, value);
        } else {
            map.put(key.trim(), value);
        }
    }

    // === PARSEO DE VALORES ===

    private static SpeciesSettings toSettings(Map<String, Object> values) {
        OptionalInt baseLevel = readInt(values.get("baseLevel"));
        OptionalInt levelVariance = readInt(values.get("levelVariance"));
        OptionalInt maxLevel = readInt(values.get("maxLevel"));
        OptionalInt pointsPerLevel = readInt(values.get("pointsPerLevel"));
        Optional<MobTier> tierOverride = readTier(values.get("tier"));
        Map<MobTier, Double> tierWeights = parseTierWeights(values.get("tiers"));
        Map<StatKeys, Integer> statWeights = parseStatWeights(values.get("growth"));

        Map<String, Object> multipliers = castMap(values.get("multipliers"));
        OptionalDouble healthMultiplier = readDouble(multipliers.get("health"));
        OptionalDouble damageMultiplier = readDouble(multipliers.get("damage"));
        OptionalDouble defenseMultiplier = readDouble(multipliers.get("defense"));
        OptionalDouble speedMultiplier = readDouble(multipliers.get("speed"));

        boolean random = asBoolean(values.get("random")) || asBoolean(values.get("randomDistribution"));

        return new SpeciesSettings(
                baseLevel,
                levelVariance,
                maxLevel,
                pointsPerLevel,
                tierOverride,
                tierWeights,
                statWeights,
                healthMultiplier,
                damageMultiplier,
                defenseMultiplier,
                speedMultiplier,
                random);
    }

    private static Map<MobTier, Double> parseTierWeights(Object value) {
        Map<MobTier, Double> map = new EnumMap<>(MobTier.class);
        Map<String, Object> raw = castMap(value);
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            Optional<MobTier> tier = readTier(entry.getKey());
            OptionalDouble weight = readDouble(entry.getValue());
            if (tier.isPresent() && weight.isPresent() && weight.getAsDouble() > 0.0D) {
                map.put(tier.get(), weight.getAsDouble());
            }
        }
        return map;
    }

    private static Map<StatKeys, Integer> parseStatWeights(Object value) {
        Map<StatKeys, Integer> map = new EnumMap<>(StatKeys.class);
        Map<String, Object> raw = castMap(value);
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            StatKeys.fromId(entry.getKey()).ifPresent(key -> {
                OptionalInt weight = readInt(entry.getValue());
                weight.ifPresent(w -> map.put(key, Math.max(0, w)));
            });
        }
        return map;
    }

    private static Optional<MobTier> readTier(Object value) {
        if (value == null)
            return Optional.empty();
        if (value instanceof MobTier tier)
            return Optional.of(tier);
        String text = String.valueOf(value).trim();
        if (text.isEmpty())
            return Optional.empty();
        try {
            return Optional.of(MobTier.valueOf(text.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Unknown tier in configuration: {}", text);
            return Optional.empty();
        }
    }

    private static OptionalInt readInt(Object value) {
        if (value == null)
            return OptionalInt.empty();
        if (value instanceof Number number)
            return OptionalInt.of(number.intValue());
        try {
            return OptionalInt.of(Integer.parseInt(String.valueOf(value).trim()));
        } catch (NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }

    private static OptionalDouble readDouble(Object value) {
        if (value == null)
            return OptionalDouble.empty();
        if (value instanceof Number number)
            return OptionalDouble.of(number.doubleValue());
        try {
            return OptionalDouble.of(Double.parseDouble(String.valueOf(value).trim()));
        } catch (NumberFormatException ex) {
            return OptionalDouble.empty();
        }
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean bool)
            return bool;
        if (value == null)
            return false;
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return text.equals("true") || text.equals("yes") || text.equals("1");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> copy.put(String.valueOf(k), v));
            return copy;
        }
        return new LinkedHashMap<>();
    }

    private static Object parseScalar(String raw) {
        if (raw == null)
            return "";
        String value = raw.trim();
        if (value.isEmpty())
            return "";
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        try {
            if (value.contains("."))
                return Double.parseDouble(value);
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return value;
        }
    }

    private static Map<String, Object> parseInlineMap(String text) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (text == null)
            return map;
        String body = text.trim();
        if (body.startsWith("{"))
            body = body.substring(1);
        if (body.endsWith("}"))
            body = body.substring(0, body.length() - 1);

        int index = 0;
        while (index < body.length()) {
            index = skipWhitespace(body, index);
            int keyStart = index;
            while (index < body.length() && body.charAt(index) != ':' && body.charAt(index) != '=')
                index++;
            if (index >= body.length())
                break;
            String key = body.substring(keyStart, index).trim();
            index++;
            index = skipWhitespace(body, index);
            if (index >= body.length()) {
                map.put(stripQuotes(key), "");
                break;
            }

            char current = body.charAt(index);
            Object value;
            if (current == '{') {
                int depth = 1;
                int valueStart = index + 1;
                index++;
                while (index < body.length() && depth > 0) {
                    char c = body.charAt(index);
                    if (c == '{')
                        depth++;
                    else if (c == '}')
                        depth--;
                    index++;
                }
                int valueEnd = Math.max(valueStart, index - 1);
                String inner = body.substring(valueStart, valueEnd);
                value = parseInlineMap("{" + inner + "}");
            } else {
                boolean inString = current == '"' || current == '\'';
                char stringChar = inString ? current : 0;
                int valueStart = index;
                while (index < body.length()) {
                    char c = body.charAt(index);
                    if (!inString && c == ',')
                        break;
                    if ((c == '"' || c == '\'') && (index == valueStart || body.charAt(index - 1) != '\\')) {
                        if (inString && c == stringChar)
                            inString = false;
                        else if (!inString) {
                            inString = true;
                            stringChar = c;
                        }
                    }
                    index++;
                }
                String rawValue = body.substring(valueStart, index).trim();
                value = parseScalar(rawValue);
            }
            map.put(stripQuotes(key), value);
            while (index < body.length() && body.charAt(index) != ',')
                index++;
            if (index < body.length() && body.charAt(index) == ',')
                index++;
        }
        return map;
    }

    private static int skipWhitespace(String text, int index) {
        while (index < text.length() && Character.isWhitespace(text.charAt(index)))
            index++;
        return index;
    }

    private static String stripQuotes(String text) {
        if (text == null)
            return "";
        String trimmed = text.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String defaultTemplate() {
        return """
# Manual per-species overrides used by MANUAL_SPECIES scaling mode.
# Restart the game or server after editing this file.
#
# [minecraft:zombie]
# baseLevel = 5
# levelVariance = 2
# maxLevel = 18
# pointsPerLevel = 3
# tier = "NORMAL"
# growth = { str = 14, agi = 10, vit = 12, int = 4, dex = 8, luk = 4 }
# multipliers = { health = 1.1, damage = 1.0, defense = 1.0, speed = 1.0 }
#
# [minecraft:skeleton]
# baseLevel = 8
# levelVariance = 3
# maxLevel = 24
# growth = { str = 8, agi = 12, vit = 8, int = 6, dex = 18, luk = 6 }
#
# [othermod:desert_stalker]
# baseLevel = 14
# levelVariance = 2
# maxLevel = 32
# tier = "ELITE"
""";
    }
}
