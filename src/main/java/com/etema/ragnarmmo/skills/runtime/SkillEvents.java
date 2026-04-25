package com.etema.ragnarmmo.skills.runtime;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.lifeskills.PlacedBlockTracker;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles events to award Skill XP.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class SkillEvents {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer && event.getLevel() instanceof ServerLevel serverLevel) {
            PlacedBlockTracker.get(serverLevel).markPlaced(event.getPos());
        }
    }

    // Life/Gathering Skills
    private static final net.minecraft.resources.ResourceLocation MINING = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:mining");
    private static final net.minecraft.resources.ResourceLocation WOODCUTTING = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:woodcutting");
    private static final net.minecraft.resources.ResourceLocation EXCAVATION = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:excavation");
    private static final net.minecraft.resources.ResourceLocation FARMING = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:farming");

    // Combat Skills
    private static final net.minecraft.resources.ResourceLocation SWORD_MASTERY = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:sword_mastery");
    private static final net.minecraft.resources.ResourceLocation ONE_HAND_MASTERY = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:one_hand_mastery");
    private static final net.minecraft.resources.ResourceLocation TWO_HAND_MASTERY = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:two_hand_mastery");
    private static final net.minecraft.resources.ResourceLocation BASH = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:bash");
    private static final net.minecraft.resources.ResourceLocation DAGGER_MASTERY = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:dagger_mastery");
    private static final net.minecraft.resources.ResourceLocation BACKSTAB_TRAINING = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:backstab_training");
    private static final net.minecraft.resources.ResourceLocation BOW_MASTERY = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:bow_mastery");
    private static final net.minecraft.resources.ResourceLocation ACCURACY_TRAINING = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:accuracy_training");
    private static final net.minecraft.resources.ResourceLocation MACE_MASTERY = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:mace_mastery");
    private static final net.minecraft.resources.ResourceLocation STAFF_MASTERY = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:staff_mastery");
    private static final net.minecraft.resources.ResourceLocation WEAPON_TRAINER = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:weapon_trainer");
    private static final net.minecraft.resources.ResourceLocation SPEAR_MASTERY = new net.minecraft.resources.ResourceLocation(
            "ragnarmmo:spear_mastery");

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) ||
                !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Check Anti-Abuse
        PlacedBlockTracker tracker = PlacedBlockTracker.get(serverLevel);
        if (tracker.isPlayerPlaced(event.getPos())) {
            // Priority is LOWEST in PlacedBlockTracker, so cleanup happens after this.
            return; // No XP for placed blocks
        }

        // Determine Skill based on Tool
        ItemStack held = player.getMainHandItem();
        Item item = held.getItem();
        net.minecraft.resources.ResourceLocation targetSkill = null;

        if (item instanceof PickaxeItem)
            targetSkill = MINING;
        else if (item instanceof AxeItem)
            targetSkill = WOODCUTTING;
        else if (item instanceof ShovelItem)
            targetSkill = EXCAVATION;
        else if (item instanceof HoeItem)
            targetSkill = FARMING;

        // Skip life skills if they are managed by LifeSkillManager (which uses point
        // system)
        // Checking via SkillRegistry to confirm category
        if (targetSkill != null && !isLifeSkill(targetSkill)) {
            final net.minecraft.resources.ResourceLocation skill = targetSkill; // for lambda
            BlockState state = event.getState();
            PlayerSkillsProvider.get(player).ifPresent(skills -> {
                int xp = SourceConfig.getInstance().getXp(state, skill);
                if (xp > 0) {
                    skills.addXP(skill, xp, com.etema.ragnarmmo.skills.api.XPGainReason.BLOCK_BREAK);
                }
            });
        }
    }

    // Fishing now uses the new point-based life skills system
    // See LifeSkillEventHandler for the new implementation
    // @SubscribeEvent
    // public static void
    // onItemFished(net.minecraftforge.event.entity.player.ItemFishedEvent event) {
    // // Disabled - life skills use point-based system
    // }

    // Exploration now uses the new point-based life skills system with region
    // tracking
    // See ExplorationEventHandler for the new implementation
    // Old distance-based XP is disabled to prevent exploits (circle-walking)

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
            .create(new net.minecraft.resources.ResourceLocation(RagnarMMO.MODID, "daggers"));
    private static final net.minecraft.tags.TagKey<Item> MACE_TAG = net.minecraft.tags.ItemTags
            .create(new net.minecraft.resources.ResourceLocation(RagnarMMO.MODID, "maces"));
    private static final net.minecraft.tags.TagKey<Item> STAFF_TAG = net.minecraft.tags.ItemTags
            .create(new net.minecraft.resources.ResourceLocation(RagnarMMO.MODID, "staves"));
    private static final net.minecraft.tags.TagKey<Item> WAND_TAG = net.minecraft.tags.ItemTags
            .create(new net.minecraft.resources.ResourceLocation(RagnarMMO.MODID, "wands"));
    private static final net.minecraft.tags.TagKey<Item> TWO_HANDED_TAG = net.minecraft.tags.ItemTags
            .create(new net.minecraft.resources.ResourceLocation(RagnarMMO.MODID, "two_handed"));

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
}
