package com.lootlookup.osrswiki;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum DropTableType {
    ALWAYS("100%"),
    HUNDRED_DROPS("100% drops"),
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
    UNIQUES("Uniques"),
    TALISMANS("Talismans"),
    TALISMANS_NOTED("Talismans (noted)"),
    OTHER("Other"),
    MINOR_DROPS("Minor drops"),
    THIRD_AGE("3rd age drop table"),
    ANIMA_SEEDS("Anima seeds"),
    ALLOTMENT_SEEDS("Allotment seeds"),
    FLOWER_SEEDS("Flower seeds"),
    HOP_SEEDS("Hop seeds"),
    BUSH_SEEDS("Bush seeds"),
    HERB_SEEDS("Herb seeds"),
    TREE_SEEDS("Tree seeds"),
    FRUIT_TREE_SEEDS("Fruit tree seeds"),
    SPECIAL_SEEDS("Special seeds"),
    ANCIENT_SHARDS("Ancient shards"),
    GEMSTONES("Gemstones"),
    ARMOUR("Armour"),
    DROPS("Drops"),
    ORES_AND_BARS("Ores and bars"),
    POTIONS("Potions"),
    MUTAGENS("Mutagens"),
    DRAGONHIDE("Dragonhide"),
    FLETCHING_MATS("Fletching materials"),
    SIGILS("Sigils"),
    POUCHES("Pouches"),
    RUNECRAFTING_ITEMS("Runecrafting items"),
    SUPERIOR_SLAYER_TERTIARY("Superior Slayer tertiary"),
    FOSSILS("Fossils"),
    SEAWEED("Seaweed"),
    ORE("Ore"),
    OYSTER("Oyster"),
    NOTED_HERBS("Noted herbs"),
    BEADS("Beads"),
    EQUIPMENT("Equipment"),
    FOOD("Food"),
    TOOLS("Tools"),
    WEAPONS("Weapons"),
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
