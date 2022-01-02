package com.lootlookup.utils;

import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.image.BufferedImage;

public class Icons {
    public static final BufferedImage NAV_BUTTON = ImageUtil.loadImageResource(Icons.class, "/nav_button.png");

    final static BufferedImage collapseImg = ImageUtil.loadImageResource(Icons.class, "/collapsed.png");
    final static BufferedImage expandedImg = ImageUtil.loadImageResource(Icons.class, "/expanded.png");
    final static BufferedImage percentImg = ImageUtil.loadImageResource(Icons.class, "/percent.png");
    final static BufferedImage externalLinkImg = ImageUtil.loadImageResource(Icons.class, "/external_link.png");
    final static BufferedImage externalLinkImgResize = Util.resizeImg(externalLinkImg, 20, 20);
    final static BufferedImage listImg = ImageUtil.loadImageResource(Icons.class, "/list.png");
    final static BufferedImage gridImg = ImageUtil.loadImageResource(Icons.class, "/grid.png");

    public static final ImageIcon COLLAPSE_ICON = new ImageIcon(collapseImg);
    public static final ImageIcon EXPAND_ICON = new ImageIcon(expandedImg);
    public static final ImageIcon PERCENT_ICON = new ImageIcon(percentImg);
    public static final ImageIcon PERCENT_ICON_FADED =  new ImageIcon(ImageUtil.alphaOffset(percentImg, -175));

    public static final ImageIcon EXTERNAL_LINK_ICON = new ImageIcon(externalLinkImgResize);
    public static final ImageIcon EXTERNAL_LINK_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(externalLinkImgResize, -175));

    public static final ImageIcon LIST_ICON = new ImageIcon(listImg);
    public static final ImageIcon LIST_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(listImg, -180));
    public static final ImageIcon LIST_ICON_FADED = new ImageIcon(ImageUtil.alphaOffset(listImg, -220));

    public static final ImageIcon GRID_ICON = new ImageIcon(gridImg);
    public static final ImageIcon GRID_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(gridImg, -180));
    public static final ImageIcon GRID_ICON_FADED = new ImageIcon(ImageUtil.alphaOffset(gridImg, -220));

}
