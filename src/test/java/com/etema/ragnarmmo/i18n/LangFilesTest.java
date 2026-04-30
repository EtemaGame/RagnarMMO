package com.etema.ragnarmmo.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class LangFilesTest {
    private static final Path LANG_DIR = Path.of("src/main/resources/assets/ragnarmmo/lang");
    private static final Pattern PLACEHOLDER = Pattern.compile("%(?:\\d+\\$)?[sdif]");
    private static final List<String> EN_US_SUSPICIOUS_TOKENS = List.of(
            "Fertig",
            "Ziehe",
            "Trascina",
            "Breite",
            "Einblendung",
            "durch Tod",
            "konfigurieren",
            "Ouvrir",
            "Verleiht",
            "Werte",
            "Kosten",
            "Maximalwert",
            "Il vous faut");
    private static final List<String> MOJIBAKE_TOKENS = List.of(
            "\u00c3",
            "\u00c2",
            "\u00e2\u20ac",
            "\u00e2\u2020",
            "\ufffd");

    @Test
    void langFilesStayInSyncWithEnglishBaseline() throws IOException {
        LangFile enUs = readLang("en_us.json");
        assertTrue(enUs.duplicateKeys().isEmpty(), () -> "Duplicate keys in en_us.json: " + enUs.duplicateKeys());
        assertNoInvalidKeys(enUs);
        assertNoDuplicateLikeKeys(enUs);
        assertNoSuspiciousEnglishBaselineText(enUs);
        assertNoMojibake(enUs);

        try (var files = Files.list(LANG_DIR)) {
            for (Path path : files
                    .filter(file -> file.getFileName().toString().endsWith(".json"))
                    .filter(file -> !file.getFileName().toString().equals("en_us.json"))
                    .sorted()
                    .toList()) {
                LangFile locale = readLang(path.getFileName().toString());
                assertTrue(locale.duplicateKeys().isEmpty(),
                        () -> "Duplicate keys in " + locale.fileName() + ": " + locale.duplicateKeys());
                assertEquals(Set.of(), diff(enUs.entries().keySet(), locale.entries().keySet()),
                        locale.fileName() + " is missing keys from canonical en_us.json");
                assertEquals(Set.of(), diff(locale.entries().keySet(), enUs.entries().keySet()),
                        locale.fileName() + " has keys that are not present in canonical en_us.json");
                assertNoInvalidKeys(locale);
                assertNoDuplicateLikeKeys(locale);
                assertPlaceholdersMatch(enUs, locale);
                assertNoMojibake(locale);
            }
        }
    }

    private static LangFile readLang(String fileName) throws IOException {
        String content = Files.readString(LANG_DIR.resolve(fileName), StandardCharsets.UTF_8);
        Map<String, String> entries = new LinkedHashMap<>();
        List<String> duplicateKeys = new ArrayList<>();

        JsonReader reader = new JsonReader(new StringReader(content));
        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            if (entries.containsKey(key)) {
                duplicateKeys.add(key);
            }

            if (reader.peek() != JsonToken.STRING) {
                throw new AssertionError(fileName + " has a non-string value for key " + key);
            }

            entries.put(key, reader.nextString());
        }
        reader.endObject();

        return new LangFile(fileName, entries, duplicateKeys);
    }

    private static Set<String> diff(Set<String> left, Set<String> right) {
        Set<String> result = new LinkedHashSet<>(left);
        result.removeAll(right);
        return result;
    }

    private static void assertNoInvalidKeys(LangFile lang) {
        List<String> invalid = lang.entries().keySet().stream()
                .filter(key -> key.contains("\\") || key.endsWith("."))
                .toList();
        assertTrue(invalid.isEmpty(), () -> lang.fileName() + " has invalid generated keys: " + invalid);
    }

    private static void assertNoDuplicateLikeKeys(LangFile lang) {
        Map<String, String> normalizedToOriginal = new LinkedHashMap<>();
        List<String> duplicateLike = new ArrayList<>();

        for (String key : lang.entries().keySet()) {
            String normalized = key.replace('\\', '.').replaceAll("\\.+", ".").toLowerCase(Locale.ROOT);
            String previous = normalizedToOriginal.putIfAbsent(normalized, key);
            if (previous != null && !previous.equals(key)) {
                duplicateLike.add(previous + " ~= " + key);
            }
        }

        assertTrue(duplicateLike.isEmpty(), () -> lang.fileName() + " has duplicate-like keys: " + duplicateLike);
    }

    private static void assertPlaceholdersMatch(LangFile enUs, LangFile esEs) {
        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, String> entry : enUs.entries().entrySet()) {
            String key = entry.getKey();
            List<String> expected = placeholders(entry.getValue());
            List<String> actual = placeholders(esEs.entries().get(key));
            if (!expected.equals(actual)) {
                mismatches.add(key + " expected " + expected + " but got " + actual);
            }
        }

        assertTrue(mismatches.isEmpty(), () -> "Placeholder mismatches between en_us and es_es: " + mismatches);
    }

    private static List<String> placeholders(String value) {
        List<String> placeholders = new ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher(value);
        while (matcher.find()) {
            placeholders.add(matcher.group());
        }
        return placeholders;
    }

    private static void assertNoSuspiciousEnglishBaselineText(LangFile enUs) {
        List<String> suspicious = new ArrayList<>();
        for (Map.Entry<String, String> entry : enUs.entries().entrySet()) {
            for (String token : EN_US_SUSPICIOUS_TOKENS) {
                if (entry.getValue().contains(token)) {
                    suspicious.add(entry.getKey() + " contains " + token);
                }
            }
        }

        assertTrue(suspicious.isEmpty(), () -> "en_us.json has suspicious non-English text: " + suspicious);
    }

    private static void assertNoMojibake(LangFile lang) {
        List<String> suspicious = new ArrayList<>();
        for (Map.Entry<String, String> entry : lang.entries().entrySet()) {
            for (String token : MOJIBAKE_TOKENS) {
                if (entry.getValue().contains(token)) {
                    suspicious.add(entry.getKey() + " contains " + token);
                }
            }
        }

        assertTrue(suspicious.isEmpty(), () -> lang.fileName() + " has likely mojibake: " + suspicious);
    }

    private record LangFile(String fileName, Map<String, String> entries, List<String> duplicateKeys) {
    }
}
