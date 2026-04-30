package com.etema.ragnarmmo.combat.contract;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RoCombatStatusBoundarySourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void provokeUsesRoStatusInsteadOfVanillaCombatAttributes() throws IOException {
        String provoke = read("skills/job/swordman/ProvokeSkillEffect.java");
        String aggroEvents = read("combat/aggro/AggroEvents.java");
        String status = read("combat/status/RoCombatStatusService.java");
        String resolver = read("combat/contract/CombatantProfileResolver.java");

        assertTrue(provoke.contains("RoCombatStatusService.applyProvoke"));
        assertTrue(!provoke.contains("Attributes.ARMOR"));
        assertTrue(!provoke.contains("Attributes.ATTACK_DAMAGE"));
        assertTrue(!aggroEvents.contains("Attributes.ARMOR"));
        assertTrue(!aggroEvents.contains("Attributes.ATTACK_DAMAGE"));
        assertTrue(status.contains("PROVOKE_DEF_REDUCTION_TAG"));
        assertTrue(status.contains("PROVOKE_ATK_BONUS_TAG"));
        assertTrue(resolver.contains("physicalAttackMultiplier"));
        assertTrue(resolver.contains("physicalDefenseMultiplier"));
    }

    @Test
    void decreaseAgiFeedsRoProfileStateInsteadOfVanillaArmorResistance() throws IOException {
        String decreaseAgi = read("skills/job/acolyte/DecreaseAgiSkillEffect.java");
        String status = read("combat/status/RoCombatStatusService.java");
        String resolver = read("combat/contract/CombatantProfileResolver.java");

        assertTrue(decreaseAgi.contains("RoCombatStatusService.applyDecreaseAgi"));
        assertTrue(decreaseAgi.contains("RoSkillStatHelper.agi"));
        assertTrue(!decreaseAgi.contains("getArmorValue"));
        assertTrue(!decreaseAgi.contains("ARMOR_TOUGHNESS"));
        assertTrue(status.contains("DECREASE_AGI_AMOUNT_TAG"));
        assertTrue(resolver.contains("agiPenalty"));
        assertTrue(resolver.contains("applyFleePenalty"));
        assertTrue(resolver.contains("applyAspdPenalty"));
    }

    @Test
    void monsterPropertyReportsRoDefenseInsteadOfVanillaArmor() throws IOException {
        String monsterProperty = read("skills/job/wizard/MonsterPropertySkillEffect.java");

        assertTrue(monsterProperty.contains("CombatantProfileResolver"));
        assertTrue(monsterProperty.contains("hardDef"));
        assertTrue(!monsterProperty.contains("Attributes.ARMOR"));
        assertTrue(!monsterProperty.contains("getAttributeValue"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
