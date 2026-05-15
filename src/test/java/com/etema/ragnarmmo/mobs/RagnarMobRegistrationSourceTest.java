package com.etema.ragnarmmo.mobs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RagnarMobRegistrationSourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void mobsModuleRegistersAttributesFromCentralRagnarMobCollection() throws IOException {
        String entities = Files.readString(ROOT.resolve("common/init/RagnarEntities.java"));
        String module = Files.readString(ROOT.resolve("common/init/modules/MobsModule.java"));

        assertFalse(entities.contains("RAGNAR_MOBS"));
        assertTrue(module.contains("RagnarEntities.PORING.get()"));
        assertTrue(module.contains("RagnarEntities.POPORING.get()"));
        assertTrue(module.contains("RagnarEntities.LUNATIC.get()"));
        assertTrue(module.contains("RagnarEntities.CREAMY.get()"));
        assertTrue(module.contains("RagnarEntities.CREAMY_FEAR.get()"));
    }

    @Test
    void registeredRagnarMobsHaveEnglishLangEntries() throws IOException {
        String entities = Files.readString(ROOT.resolve("common/init/RagnarEntities.java"));
        String lang = Files.readString(Path.of("src/main/resources/assets/ragnarmmo/lang/en_us.json"));

        assertMobLang(entities, lang, "poring");
        assertMobLang(entities, lang, "poporing");
        assertMobLang(entities, lang, "drop");
        assertMobLang(entities, lang, "marin");
        assertMobLang(entities, lang, "lunatic");
        assertMobLang(entities, lang, "fabre");
        assertMobLang(entities, lang, "pupa");
        assertMobLang(entities, lang, "muka");
        assertMobLang(entities, lang, "creamy");
        assertMobLang(entities, lang, "creamy_fear");
    }

    @Test
    void registeredRagnarMobsHaveRendererAndVisualAssets() throws IOException {
        String clientEvents = Files.readString(ROOT.resolve("client/ClientModEvents.java"));

        assertMobVisuals(clientEvents, "PORING", "poring", "PoringRenderer", "poring");
        assertMobVisuals(clientEvents, "POPORING", "poporing", "PoporingRenderer", "poring");
        assertMobVisuals(clientEvents, "DROP", "drop", "DropRenderer", "poring");
        assertMobVisuals(clientEvents, "MARIN", "marin", "MarinRenderer", "poring");
        assertMobVisuals(clientEvents, "LUNATIC", "lunatic", "LunaticRenderer", "lunatic");
        assertMobVisuals(clientEvents, "FABRE", "fabre", "FabreRenderer", "fabre");
        assertMobVisuals(clientEvents, "PUPA", "pupa", "PupaRenderer", "pupa");
        assertMobVisuals(clientEvents, "MUKA", "muka", "MukaRenderer", "muka");
        assertMobVisuals(clientEvents, "CREAMY", "creamy", "CreamyRenderer", "creamy");
        assertMobVisuals(clientEvents, "CREAMY_FEAR", "creamy_fear", "CreamyFearRenderer", "creamy");
    }

    @Test
    void registeredRagnarMobsHaveSpawnEggItemsModelsAndLang() throws IOException {
        String items = Files.readString(ROOT.resolve("items/RagnarMobItems.java"));
        String lang = Files.readString(Path.of("src/main/resources/assets/ragnarmmo/lang/en_us.json"));

        assertSpawnEgg(items, lang, "PORING", "poring");
        assertSpawnEgg(items, lang, "POPORING", "poporing");
        assertSpawnEgg(items, lang, "DROP", "drop");
        assertSpawnEgg(items, lang, "MARIN", "marin");
        assertSpawnEgg(items, lang, "LUNATIC", "lunatic");
        assertSpawnEgg(items, lang, "FABRE", "fabre");
        assertSpawnEgg(items, lang, "PUPA", "pupa");
        assertSpawnEgg(items, lang, "MUKA", "muka");
        assertSpawnEgg(items, lang, "CREAMY", "creamy");
        assertSpawnEgg(items, lang, "CREAMY_FEAR", "creamy_fear");
    }

    private static void assertMobLang(String entities, String lang, String id) {
        assertTrue(entities.contains("registerCreature(\"" + id + "\""), () -> id + " is not registered");
        assertTrue(lang.contains("\"entity.ragnarmmo." + id + "\""), () -> id + " has no English lang entry");
    }

    private static void assertMobVisuals(String clientEvents, String constant, String id, String renderer, String geoBase) {
        assertTrue(clientEvents.contains("RagnarEntities." + constant + ".get()"), () -> id + " renderer is not registered");
        assertTrue(clientEvents.contains("render.entity." + renderer), () -> id + " renderer class is not wired");
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/ragnarmmo/textures/entity/" + id + ".png")),
                () -> id + " texture is missing");
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/ragnarmmo/geo/entity/" + geoBase + ".geo.json")),
                () -> id + " geo model is missing");
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/ragnarmmo/animations/entity/" + geoBase + ".animation.json")),
                () -> id + " animation is missing");
    }

    private static void assertSpawnEgg(String items, String lang, String constant, String id) {
        assertTrue(items.contains(constant + "_SPAWN_EGG"), () -> id + " spawn egg item is not registered");
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/ragnarmmo/models/item/others/eggs/" + id + "_spawn_egg.json")),
                () -> id + " spawn egg model is missing");
        assertTrue(lang.contains("\"item.ragnarmmo.others.eggs." + id + "_spawn_egg\""),
                () -> id + " spawn egg has no English lang entry");
    }
}
