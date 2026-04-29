package com.etema.ragnarmmo.combat.contract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class SkillCombatMigrationSourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void migratedMageCombatSkillsDoNotApplyFinalDamageDirectly() throws IOException {
        String sources = read("skills/execution/projectile/AbstractBoltSkillEffect.java")
                + read("skills/job/acolyte/HolyLightSkillEffect.java")
                + read("skills/job/mage/FrostDiverSkillEffect.java")
                + read("skills/job/mage/NapalmBeatSkillEffect.java")
                + read("skills/job/mage/FireBallSkillEffect.java")
                + read("skills/job/mage/SoulStrikeSkillEffect.java");

        assertFalse(sources.contains("SkillDamageHelper"),
                "Migrated combat skills must not use SkillDamageHelper for final damage");
        assertFalse(sources.contains(".hurt("),
                "Migrated combat skills must not call target.hurt() directly");
    }

    @Test
    void migratedSkillsResolveDamageThroughCombatContract() throws IOException {
        String resolver = read("combat/engine/RagnarSkillResolver.java");
        String contract = read("combat/contract/CombatContract.java");
        String specResolver = read("combat/contract/SkillCombatSpecResolver.java");

        assertTrue(resolver.contains("SkillCombatSpecResolver.resolve"),
                "Combat skills must declare a SkillCombatSpec before damage resolution");
        assertTrue(resolver.contains("combatContract.resolveSkill"),
                "Combat skills must pass through CombatContract");
        assertTrue(resolver.contains("shouldExecuteLegacyEffectAfterContract"),
                "Combat skills must not execute unsafe legacy effects after contract damage");
        assertTrue(contract.contains("resolveSkill("),
                "CombatContract must expose skill damage resolution");
        assertTrue(contract.contains("applyMagicDefense"),
                "Magical skills must resolve against MDEF inside the contract");
        assertTrue(specResolver.contains("hasLevelKey(definition, level, \"damage_percent\")"),
                "Authored damage_percent skills must become SkillCombatSpec skills");
        assertTrue(specResolver.contains("defaultDamagePercent"),
                "Legacy hardcoded combat skills must have temporary contract specs until authored data is complete");
    }

    @Test
    void legacySkillPacketIsAWrapperIntoCombatEngine() throws IOException {
        String packet = read("skills/net/PacketUseSkill.java");
        String handler = read("skills/runtime/SkillEffectHandler.java");

        assertTrue(packet.contains("RagnarCombatEngine.get().handleSkillUseRequest"),
                "Legacy skill packet must forward into the server-authoritative combat engine");
        assertFalse(packet.contains("SkillEffectHandler.tryUseSkill"),
                "Legacy skill packet must not execute skills through the old direct-effect path");
        assertTrue(handler.contains("SkillCombatSpecResolver.resolve(def, level).isPresent()"),
                "Legacy skill handler must detect combat specs");
        assertTrue(handler.contains("forwardCombatSkillToEngine"),
                "Legacy skill handler must forward combat specs into the combat engine");
    }

    @Test
    void legacySkillAndVisualEntitiesDoNotContainCombatDamagePaths() throws IOException {
        String sources = readTree("skills")
                + readTree("entity/aoe")
                + readTree("entity/projectile");

        assertFalse(sources.contains("SkillDamageHelper.dealSkillDamage"),
                "Legacy skill/visual entities must not apply final combat damage");
        assertFalse(sources.contains(".hurt("),
                "Legacy skill/visual entities must not call hurt() directly");
        assertFalse(sources.contains("SkillDamageHelper.scaleByATK"),
                "Legacy skill code must not keep old ATK damage formulas");
        assertFalse(sources.contains("SkillDamageHelper.scaleByMATK"),
                "Legacy skill code must not keep old MATK damage formulas");
        assertFalse(sources.contains("event.setAmount("),
                "Legacy skill hooks must not mutate vanilla damage amounts");
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    private static String readTree(String relative) throws IOException {
        Path root = ROOT.resolve(relative);
        try (Stream<Path> paths = Files.walk(root)) {
            StringBuilder out = new StringBuilder();
            for (Path path : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                out.append(Files.readString(path)).append('\n');
            }
            return out.toString();
        }
    }
}
