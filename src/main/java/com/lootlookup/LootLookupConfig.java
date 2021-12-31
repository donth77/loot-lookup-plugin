package com.lootlookup;

import com.lootlookup.utils.Constants;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(Constants.PLUGIN_NAME)
public interface LootLookupConfig extends Config {
    @ConfigItem(
            keyName = "showRarity",
            name = "Rarity",
            description = ""
    )
    default boolean showRarity() {
        return true;
    }

    @ConfigItem(
            keyName = "showQuantity",
            name = "Quantity",
            description = ""
    )
    default boolean showQuantity() {
        return true;
    }

    @ConfigItem(
            keyName = "showPrice",
            name = "Price",
            description = ""
    )
    default boolean showPrice() {
        return true;
    }

    @ConfigItem(
            keyName = "disableMenuOption",
            name = "Disable Right Click Menu Option",
            description = "Disables the right click menu option for monsters"
    )
    default boolean disableMenuOption() {
        return false;
    }


}
