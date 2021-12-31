package com.lootlookup.osrswiki;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum DropTableType {
    ALWAYS("100%"),
    WEAPONS_ARMOUR("Weapons and armour"),
    RUNES("Runes"),
    RUNES_AND_AMMO("Runes and ammunition"),
    HERBS("Herbs"),
    MATERIALS("Materials"),
    COINS("Coins"),
    RARES("Rare drop table"),
    GEMS("Gem drop table"),
    RARE_AND_GEM("Rare and Gem drop table"),
    TERTIARY("Tertiary"),
    CATACOMBS_TERTIARY("Catacombs tertiary"),
    WILDY_TERTIARY("Wilderness Slayer tertiary"),
    PRE_ROLL("Pre-roll"),
    SEEDS("Seeds"),
    CONSUMABLES("Consumables"),
    RESOURCES("Resources"),
    DEADMAN_MODE("Deadman mode"),
    BOLT_TIPS("Bolt tips"),
    FOOD_AND_POTIONS("Food and potions"),
    UNIQUE("Unique"),
    TALISMANS("Talismans"),
    OTHER("Other"),
    ;

    private final String label;
    DropTableType(final String s) { label = s; }
    public String toString() { return label; }

    public static boolean isValidSlotType(String str) {
        Set<String> labelSet = new HashSet<>(Arrays.asList(DropTableType.getLabels()));
        return labelSet.contains(str);
    }

    public static String[] getLabels() {
        return Arrays.toString(DropTableType.values()).replaceAll("^.|.$", "").split(", ");
    }
}
