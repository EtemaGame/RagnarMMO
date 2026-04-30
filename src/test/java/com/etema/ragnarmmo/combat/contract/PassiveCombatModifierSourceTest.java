package com.etema.ragnarmmo.combat.contract;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class PassiveCombatModifierSourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");

    @Test
    void targetSpecificPassivesLiveInsideCombatContract() throws IOException {
        String contract = read("combat/contract/CombatContract.java");
        String service = read("combat/contract/PassiveCombatModifierService.java");

        assertTrue(contract.contains("PassiveCombatModifierService.applyOutgoingPhysicalDamage"));
        assertTrue(contract.contains("PassiveCombatModifierService.applyIncomingPhysicalDamage"));
        assertTrue(service.contains("\"demon_bane\""));
        assertTrue(service.contains("\"divine_protection\""));
        assertTrue(service.contains("\"beast_bane\""));
        assertTrue(service.contains("\"iron_tempering\""));
        assertTrue(service.contains("\"steel_tempering\""));
        assertTrue(service.contains("\"unfair_trick\""));
        assertTrue(service.contains("isBeastOrInsect"));
        assertTrue(service.contains("PlayerSkillsProvider.get"));
    }

    @Test
    void basicAttackProcsLiveInsideCombatContractInsteadOfHurtEventDamage() throws IOException {
        String contract = read("combat/contract/CombatContract.java");
        String procService = read("combat/contract/CombatProcService.java");
        String doubleAttack = read("skills/job/thief/DoubleAttackSkillEffect.java");

        assertTrue(contract.contains("CombatProcService.applyBasicAttackDamageMultiplier"));
        assertTrue(procService.contains("\"double_attack\""));
        assertTrue(procService.contains("DAGGER_TAG"));
        assertTrue(procService.contains("SkillRegistry.get(DOUBLE_ATTACK)"));
        assertTrue(procService.contains("PlayerSkillsProvider.get"));
        assertTrue(!doubleAttack.contains("LivingHurtEvent"));
        assertTrue(!doubleAttack.contains("player.getRandom().nextFloat() < chance"));
    }

    @Test
    void weaponMasteryPassivesFeedHandAttackProfileInsteadOfHurtEventDamage() throws IOException {
        String resolver = read("combat/profile/HandAttackProfileResolver.java");

        assertTrue(resolver.contains("TWO_HAND_MASTERY"));
        assertTrue(resolver.contains("TWO_HANDED_TAG"));
        assertTrue(resolver.contains("ctx.twoHand() * 4.0D"));
        assertTrue(resolver.contains("ctx.sword() * 4.0D"));
        assertTrue(resolver.contains("ctx.spear() * 4.0D"));
        assertTrue(resolver.contains("ctx.mace() * 4.0D"));
        assertTrue(resolver.contains("ctx.katar() * 4.0D"));
        assertTrue(resolver.contains("ctx.researchWeaponry()"));
    }

    @Test
    void legacyPassiveDamageHooksDoNotMutateLivingHurtDamage() throws IOException {
        String assassinEvents = read("skills/job/assassin/AssassinSkillEvents.java");
        String blacksmithEvents = read("skills/job/blacksmith/BlacksmithSkillEvents.java");
        String priestEvents = read("skills/job/priest/PriestSkillEvents.java");
        String restrictionHook = read("items/hook/RoCombatRestrictionHook.java");
        String handResolver = read("combat/profile/HandAttackProfileResolver.java");

        assertTrue(!assassinEvents.contains("LivingHurtEvent"));
        assertTrue(!blacksmithEvents.contains("LivingHurtEvent"));
        assertTrue(!priestEvents.contains("LivingHurtEvent"));
        assertTrue(!restrictionHook.contains("event.setAmount"));
        assertTrue(handResolver.contains("applyRequirementPenalty"));

        String skillEffect = read("skills/api/ISkillEffect.java");
        String handler = read("skills/runtime/SkillEffectHandler.java");
        assertTrue(!skillEffect.contains("OFFENSIVE_HURT"));
        assertTrue(!skillEffect.contains("DEFENSIVE_HURT"));
        assertTrue(!handler.contains("onOffensiveHurt"));
        assertTrue(!handler.contains("onDefensiveHurt"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }
}
