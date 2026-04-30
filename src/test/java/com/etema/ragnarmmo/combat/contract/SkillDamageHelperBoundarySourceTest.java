package com.etema.ragnarmmo.combat.contract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SkillDamageHelperBoundarySourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void skillDamageHelperIsOnlyFinalDamageAdapter() throws IOException {
        String helper = Files.readString(ROOT.resolve("combat/damage/SkillDamageHelper.java"));

        assertTrue(helper.contains("dealSkillDamage"));
        assertFalse(helper.contains("getPhysicalATK"));
        assertFalse(helper.contains("scaleByATK"));
        assertFalse(helper.contains("getMATK"));
        assertFalse(helper.contains("scaleByMATK"));
        assertFalse(helper.contains("Attributes.ATTACK_DAMAGE"));
        assertFalse(helper.contains("getWeaponBaseDamage"));
    }

    @Test
    void nonDamageSkillStatsUseRoSkillStatHelper() throws IOException {
        String steal = Files.readString(ROOT.resolve("skills/job/thief/StealSkillEffect.java"));
        String heal = Files.readString(ROOT.resolve("skills/job/acolyte/HealSkillEffect.java"));
        String signum = Files.readString(ROOT.resolve("skills/job/acolyte/SignumCrucisSkillEffect.java"));
        String decreaseAgi = Files.readString(ROOT.resolve("skills/job/acolyte/DecreaseAgiSkillEffect.java"));

        assertTrue(steal.contains("RoSkillStatHelper.dex"));
        assertTrue(heal.contains("RoSkillStatHelper.baseLevel"));
        assertTrue(heal.contains("RoSkillStatHelper.intel"));
        assertTrue(signum.contains("RoSkillStatHelper.baseLevel"));
        assertTrue(decreaseAgi.contains("RoSkillStatHelper.intel"));
    }
}
