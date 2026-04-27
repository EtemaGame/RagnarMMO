package com.etema.ragnarmmo.skills.runtime;

import com.etema.ragnarmmo.RagnarMMO;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles events to award Skill XP.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class SkillEvents {

    // Combat Skills
    private static final net.minecraft.resources.ResourceLocation SWORD_MASTERY = skillId("sword_mastery");
    private static final net.minecraft.resources.ResourceLocation ONE_HAND_MASTERY = skillId("one_hand_mastery");
    private static final net.minecraft.resources.ResourceLocation TWO_HAND_MASTERY = skillId("two_hand_mastery");
    private static final net.minecraft.resources.ResourceLocation BASH = skillId("bash");
    private static final net.minecraft.resources.ResourceLocation DAGGER_MASTERY = skillId("dagger_mastery");
    private static final net.minecraft.resources.ResourceLocation BACKSTAB_TRAINING = skillId("backstab_training");
    private static final net.minecraft.resources.ResourceLocation BOW_MASTERY = skillId("bow_mastery");
    private static final net.minecraft.resources.ResourceLocation ACCURACY_TRAINING = skillId("accuracy_training");
    private static final net.minecraft.resources.ResourceLocation MACE_MASTERY = skillId("mace_mastery");
    private static final net.minecraft.resources.ResourceLocation STAFF_MASTERY = skillId("staff_mastery");
    private static final net.minecraft.resources.ResourceLocation WEAPON_TRAINER = skillId("weapon_trainer");
    private static final net.minecraft.resources.ResourceLocation SPEAR_MASTERY = skillId("spear_mastery");

    @SubscribeEvent
    public static void onMobKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            LivingEntity target = event.getEntity();

            PlayerSkillsProvider.get(player).ifPresent(skills -> {
                // Get applicable skills for the weapon used
                java.util.List<net.minecraft.resources.ResourceLocation> applicableSkills = getApplicableSkills(player);

                for (net.minecraft.resources.ResourceLocation skill : applicableSkills) {
                    // Double check it's not a LIFE skill (though getApplicableSkills shouldn't
                    // return them)
                    if (isLifeSkill(skill))
                        continue;

                    int xp = SourceConfig.getInstance().getXp(target, skill);
                    // Use a neutral default if config entries are not present yet.
                    if (xp == 0)
                        xp = 10;

                    if (xp > 0) {
                        skills.addXP(skill, xp, com.etema.ragnarmmo.skills.api.XPGainReason.COMBAT_PVE);
                    }
                }
            });
        }
    }

    private static boolean isLifeSkill(net.minecraft.resources.ResourceLocation skillId) {
        return com.etema.ragnarmmo.skills.data.SkillRegistry.get(skillId)
                .map(def -> def.getCategory() == com.etema.ragnarmmo.skills.api.SkillCategory.LIFE)
                .orElse(false);
    }

    private static final net.minecraft.tags.TagKey<Item> DAGGER_TAG = net.minecraft.tags.ItemTags
            .create(skillId("daggers"));
    private static final net.minecraft.tags.TagKey<Item> MACE_TAG = net.minecraft.tags.ItemTags
            .create(skillId("maces"));
    private static final net.minecraft.tags.TagKey<Item> STAFF_TAG = net.minecraft.tags.ItemTags
            .create(skillId("staves"));
    private static final net.minecraft.tags.TagKey<Item> WAND_TAG = net.minecraft.tags.ItemTags
            .create(skillId("wands"));
    private static final net.minecraft.tags.TagKey<Item> TWO_HANDED_TAG = net.minecraft.tags.ItemTags
            .create(skillId("two_handed"));

    private static java.util.List<net.minecraft.resources.ResourceLocation> getApplicableSkills(Player player) {
        ItemStack held = player.getMainHandItem();
        Item item = held.getItem();
        java.util.List<net.minecraft.resources.ResourceLocation> list = new java.util.ArrayList<>();

        // Novice
        list.add(WEAPON_TRAINER);

        boolean isDagger = held.is(DAGGER_TAG);
        boolean isMace = held.is(MACE_TAG);
        boolean isStaff = held.is(STAFF_TAG);
        boolean isWand = held.is(WAND_TAG);
        boolean isTwoHanded = held.is(TWO_HANDED_TAG);

        // Swordman
        if (item instanceof SwordItem && !isDagger) {
            list.add(SWORD_MASTERY);
            if (isTwoHanded) {
                list.add(TWO_HAND_MASTERY);
            } else {
                list.add(ONE_HAND_MASTERY);
            }
            list.add(BASH);
        }
        if (item instanceof AxeItem) {
            if (!isTwoHanded) {
                list.add(ONE_HAND_MASTERY);
            }
        }

        // Thief
        if (isDagger) {
            list.add(DAGGER_MASTERY);
            list.add(BACKSTAB_TRAINING);
        }

        // Archer
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            list.add(BOW_MASTERY);
            list.add(ACCURACY_TRAINING);
        }

        // Acolyte
        if (isMace || item instanceof ShovelItem || item.toString().toLowerCase().contains("mace")) {
            list.add(MACE_MASTERY);
        }

        // Mage
        if (isStaff || isWand) {
            list.add(STAFF_MASTERY);
        }

        // Knight
        if (item instanceof net.minecraft.world.item.TridentItem) {
            list.add(SPEAR_MASTERY);
        }

        return list;
    }

    private static net.minecraft.resources.ResourceLocation skillId(String path) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(RagnarMMO.MODID, path);
    }
}
