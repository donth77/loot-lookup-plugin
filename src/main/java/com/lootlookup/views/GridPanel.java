package com.lootlookup.views;

import com.lootlookup.LootLookupConfig;
import com.lootlookup.osrswiki.WikiItem;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class GridPanel extends JPanel {

    public GridPanel(WikiItem[] items, LootLookupConfig config, JButton percentButton) {
        int itemsPerRow = config.gridRowOption().getValue();
        final int rowSize = ((items.length % itemsPerRow == 0) ? 0 : 1) + items.length / itemsPerRow;
        setLayout(new GridLayout(rowSize, itemsPerRow, 0, 0));
        setBorder(new EmptyBorder(0,0,0,0));


        for (int i = 0; i < rowSize * itemsPerRow; i++) {
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
