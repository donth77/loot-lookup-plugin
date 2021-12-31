package com.lootlookup.utils;

import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.image.BufferedImage;

public class Icons {
    public static final BufferedImage NAV_BUTTON = ImageUtil.loadImageResource(Icons.class, "/nav_button.png");

    final static BufferedImage collapseImg = ImageUtil.loadImageResource(LootTrackerPlugin.class, "collapsed.png");
    final static BufferedImage expandedImg = ImageUtil.loadImageResource(LootTrackerPlugin.class, "expanded.png");

    final static BufferedImage percentImg = ImageUtil.loadImageResource(Icons.class, "/percent.png");
    final static BufferedImage externalLinkImg = ImageUtil.loadImageResource(Icons.class, "/external_link.png");

    public static final ImageIcon COLLAPSE_ICON = new ImageIcon(collapseImg);
    public static final ImageIcon EXPAND_ICON = new ImageIcon(expandedImg);

    public static final ImageIcon PERCENT_ICON = new ImageIcon(percentImg);
    public static final ImageIcon EXTERNAL_LINK_ICON = new ImageIcon(externalLinkImg);

    public static final ImageIcon PERCENT_ICON_DIM =  new ImageIcon(ImageUtil.alphaOffset(percentImg, -220));
    public static final ImageIcon EXTERNAL_LINK_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(externalLinkImg, -175));

}
