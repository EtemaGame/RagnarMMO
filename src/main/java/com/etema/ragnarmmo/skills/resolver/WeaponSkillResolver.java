package com.etema.ragnarmmo.skills.resolver;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;

import java.util.ArrayList;
import java.util.List;

public final class WeaponSkillResolver {
    private static final ResourceLocation SWORD_MASTERY = skillId("sword_mastery");
    private static final ResourceLocation ONE_HAND_MASTERY = skillId("one_hand_mastery");
    private static final ResourceLocation TWO_HAND_MASTERY = skillId("two_hand_mastery");
    private static final ResourceLocation BASH = skillId("bash");
    private static final ResourceLocation DAGGER_MASTERY = skillId("dagger_mastery");
    private static final ResourceLocation BACKSTAB_TRAINING = skillId("backstab_training");
    private static final ResourceLocation BOW_MASTERY = skillId("bow_mastery");
    private static final ResourceLocation ACCURACY_TRAINING = skillId("accuracy_training");
    private static final ResourceLocation MACE_MASTERY = skillId("mace_mastery");
    private static final ResourceLocation STAFF_MASTERY = skillId("staff_mastery");
    private static final ResourceLocation WEAPON_TRAINER = skillId("weapon_trainer");
    private static final ResourceLocation SPEAR_MASTERY = skillId("spear_mastery");

    private static final TagKey<Item> DAGGER_TAG = ItemTags.create(skillId("daggers"));
    private static final TagKey<Item> MACE_TAG = ItemTags.create(skillId("maces"));
    private static final TagKey<Item> STAFF_TAG = ItemTags.create(skillId("staves"));
    private static final TagKey<Item> WAND_TAG = ItemTags.create(skillId("wands"));
    private static final TagKey<Item> TWO_HANDED_TAG = ItemTags.create(skillId("two_handed"));

    private WeaponSkillResolver() {
    }

    public static List<ResourceLocation> applicableCombatSkills(Player player) {
        ItemStack held = player.getMainHandItem();
        Item item = held.getItem();
        List<ResourceLocation> skills = new ArrayList<>();

        skills.add(WEAPON_TRAINER);

        boolean isDagger = held.is(DAGGER_TAG);
        boolean isMace = held.is(MACE_TAG);
        boolean isStaff = held.is(STAFF_TAG);
        boolean isWand = held.is(WAND_TAG);
        boolean isTwoHanded = held.is(TWO_HANDED_TAG);

        if (item instanceof SwordItem && !isDagger) {
            skills.add(SWORD_MASTERY);
            skills.add(isTwoHanded ? TWO_HAND_MASTERY : ONE_HAND_MASTERY);
            skills.add(BASH);
        }
        if (item instanceof AxeItem && !isTwoHanded) {
            skills.add(ONE_HAND_MASTERY);
        }
        if (isDagger) {
            skills.add(DAGGER_MASTERY);
            skills.add(BACKSTAB_TRAINING);
        }
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            skills.add(BOW_MASTERY);
            skills.add(ACCURACY_TRAINING);
        }
        if (isMace || item instanceof ShovelItem || item.toString().toLowerCase().contains("mace")) {
            skills.add(MACE_MASTERY);
        }
        if (isStaff || isWand) {
            skills.add(STAFF_MASTERY);
        }
        if (item instanceof net.minecraft.world.item.TridentItem) {
            skills.add(SPEAR_MASTERY);
        }

        return skills;
    }

    private static ResourceLocation skillId(String path) {
        return ResourceLocation.fromNamespaceAndPath(RagnarMMO.MODID, path);
    }
}
