package com.etema.ragnarmmo.skills.api;

/**
 * Reasons for gaining XP - used for tracking and modifiers.
 */
public enum XPGainReason {
    COMBAT_PVE("Killed a mob"),
    COMBAT_PVP("Combat with player"),
    BLOCK_BREAK("Broke a block"),
    BLOCK_PLACE("Placed a block"),
    CRAFTING("Crafted an item"),
    FISHING_CATCH("Caught something fishing"),
    HARVEST("Harvested crops"),
    BREEDING("Bred animals"),
    SMELTING("Smelted items"),
    ENCHANTING("Enchanted an item"),
    BREWING("Brewed a potion"),
    FALL_DAMAGE("Survived fall damage"),
    TAME("Tamed an animal"),
    PARTY_SHARE("Shared from party member"),
    QUEST("Quest completion"),
    COMMAND("Admin command"),
    SKILL_USE("Skill usage"),
    UNKNOWN("Unknown source");

    private final String description;

    XPGainReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
