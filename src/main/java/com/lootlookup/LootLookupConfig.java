package com.lootlookup;

import com.lootlookup.utils.Constants;
import com.lootlookup.views.GridRowOption;
import com.lootlookup.views.PriceType;
import com.lootlookup.views.ViewOption;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

import static com.lootlookup.utils.Constants.*;

@ConfigGroup(Constants.CONFIG_GROUP)
public interface LootLookupConfig extends Config {
    @ConfigItem(
            position = 0,
            keyName = "defaultViewOption",
            name = "Default view option",
            description = "Select default view option"
    )
    default ViewOption viewOption() {
        return ViewOption.LIST;
    }

    @ConfigItem(
            position = 1,
            keyName = "showRarity",
            name = "Rarity",
            description = "Show/hide rarity for item"
    )
    default boolean showRarity() {
        return true;
    }

    @ConfigItem(
            position = 2,
            keyName = "showQuantity",
            name = "Quantity",
            description = "Show/hide quantity for item"
    )
    default boolean showQuantity() {
        return true;
    }

    @ConfigItem(
            position = 3,
            keyName = "showPrice",
            name = "Show Price",
            description = "Show/hide price for item"
    )
    default boolean showPrice() {
        return false;
    }

    @ConfigItem(
            position = 4,
            keyName = "priceType",
            name = "Price Type",
            description = "Select price type for item"
    )
    default PriceType priceType() {
        return PriceType.GE;
    }

    @ConfigItem(
            position = 5,
            keyName = "disableMenuOption",
            name = "Disable Right Click Menu option",
            description = "Disable the right click menu option for monsters"
    )
    default boolean disableMenuOption() {
        return false;
    }

    @ConfigItem(
            position = 6,
            keyName = "excludedMonsters",
            name = "Excluded monsters",
            description = "Comma-separated list of NPC names or IDs to hide the right-click menu option for. Names are matched case insensitive and must be singular (e.g. \"Dust devil\", not \"Dust devils\")."
    )
    default String excludedMonsters() {
        return "";
    }

    @ConfigItem(
            position = 7,
            keyName = "pinnedSubTables",
            name = "Pinned sections",
            description = "Comma-separated keywords to pin matching sections to the top of the drops panel (e.g. \"Tertiary\" pins Tertiary, Catacombs tertiary, etc.). Matched case-insensitively as substrings. Clear to keep sections in wiki order."
    )
    default String pinnedSubTables() {
        return "Unique, Tertiary";
    }

    @ConfigItem(
            position = 8,
            keyName = "disableItemLinks",
            name = "Disable Item links (List only)",
            description = "Disable links to OSRS Wiki page for item"
    )
    default boolean disableItemsLinks() {
        return false;
    }

    @ConfigItem(
            position = 9,
            keyName = "gridRowOption",
            name = "Items per row (Grid only)",
            description = "Number of items displayed in a grid row"
    )
    default GridRowOption gridRowOption() {
        return GridRowOption.FOUR;
    }

    @ConfigItem(
            position = 10,
            keyName = "commonColor",
            name = "Common Color",
            description = "Color to highlight the rarity of items with a value greater than 1/100"
    )
    default Color commonColor() {
        return DEFAULT_COMMON_COLOR;
    }

    @ConfigItem(
            position = 11,
            keyName = "rareColor",
            name = "Rare Color",
            description = "Color to highlight the rarity of items with a value of 1/100 - 1/1000"
    )
    default Color rareColor() {
        return DEFAULT_RARE_COLOR;
    }

    @ConfigItem(
            position = 12,
            keyName = "superRareColor",
            name = "Super Rare Color",
            description = "Color to highlight the rarity of items with a value of 1/1000 or less"
    )
    default Color superRareColor() {
        return DEFAULT_SUPER_RARE_COLOR;
    }

    @ConfigItem(
            position = 13,
            keyName = "priceColor",
            name = "Price Color",
            description = "Color to highlight item prices"
    )
    default Color priceColor() {
        return DEFAULT_PRICE_COLOR;
    }


}
