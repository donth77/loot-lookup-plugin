package com.lootlookup.views;

import com.lootlookup.LootLookupConfig;
import com.lootlookup.osrswiki.WikiItem;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class GridPanel extends JPanel {

    private static final int ITEMS_PER_ROW = 4;

    public GridPanel(WikiItem[] items, LootLookupConfig config, JButton percentButton) {
        final int rowSize = ((items.length % ITEMS_PER_ROW == 0) ? 0 : 1) + items.length / ITEMS_PER_ROW;
        setLayout(new GridLayout(rowSize, ITEMS_PER_ROW, 0, 0));
        setBorder(new EmptyBorder(0,0,0,0));


        for (int i = 0; i < rowSize * ITEMS_PER_ROW; i++) {
            final JPanel slotContainer = new JPanel();
            slotContainer.setLayout(new BoxLayout(slotContainer, BoxLayout.X_AXIS));

            if(i < items.length) {
                slotContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                slotContainer.add(new GridItem(items[i], config, percentButton));
                slotContainer.add(Box.createRigidArea(new Dimension(1,0)));
                slotContainer.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 1));
            }
            add(slotContainer);
        }
    }
}
