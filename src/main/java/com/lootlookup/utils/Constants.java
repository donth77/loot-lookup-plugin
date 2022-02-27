package com.lootlookup.utils;


import net.runelite.client.RuneLite;
import net.runelite.client.ui.ColorScheme;

import java.awt.*;

public class Constants {
    public static final String PLUGIN_NAME = "Loot Lookup";
    public static final String CONFIG_GROUP = "Loot-Lookup";
    public static final int DEFAULT_PRIORITY = 5;
    public static final String USER_AGENT = RuneLite.USER_AGENT + " (loot-lookup)";

    public static final Color DEFAULT_COMMON_COLOR = Color.white;
    public static final Color DEFAULT_RARE_COLOR = ColorScheme.BRAND_ORANGE.brighter();
    public static final Color DEFAULT_SUPER_RARE_COLOR = new Color(200, 50, 200);
    public static final Color DEFAULT_PRICE_COLOR = ColorScheme.GRAND_EXCHANGE_ALCH;
}
