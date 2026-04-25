package com.etema.ragnarmmo.items.cards;

import java.util.Locale;

public enum CardEquipType {
    ANY,
    WEAPON,
    SHIELD,
    ARMOR,
    HEADGEAR,
    SHOES,
    GARMENT,
    ACCESSORY;

    public static CardEquipType fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return ANY;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ANY;
        }
    }

    public String displayName() {
        return switch (this) {
            case ANY -> "Any";
            case WEAPON -> "Weapon";
            case SHIELD -> "Shield";
            case ARMOR -> "Armor";
            case HEADGEAR -> "Headgear";
            case SHOES -> "Shoes";
            case GARMENT -> "Garment";
            case ACCESSORY -> "Accessory";
        };
    }
}
