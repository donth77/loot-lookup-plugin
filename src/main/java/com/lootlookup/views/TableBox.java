package com.lootlookup.views;

import com.lootlookup.LootLookupConfig;
import com.lootlookup.osrswiki.DropTableType;
import com.lootlookup.osrswiki.WikiItem;
import com.lootlookup.utils.Util;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static com.lootlookup.utils.Icons.COLLAPSE_ICON;
import static com.lootlookup.utils.Icons.EXPAND_ICON;

public class TableBox extends JPanel {
    private LootLookupConfig config;

    private WikiItem[] items;
    private DropTableType headerStr;

    JButton collapseBtn = new JButton();
    JPanel itemsContainer = new JPanel();
    JPanel headerContainer = new JPanel();
    JPanel leftHeader = new JPanel();

    private final Color HEADER_BG_COLOR = ColorScheme.DARKER_GRAY_COLOR.darker();
    private final Color HOVER_COLOR = ColorScheme.DARKER_GRAY_HOVER_COLOR.darker();

    private final List<WikiItemPanel> itemPanels = new ArrayList<>();

    public TableBox(DropTableType headerStr, WikiItem[] items, LootLookupConfig config) {
        this.config = config;
        this.items = items;
        this.headerStr = headerStr;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        buildHeader();
        buildItemsContainer();
    }

    void buildHeader() {
        buildLeftHeader();
        buildHeaderContainer();
    }

    void buildLeftHeader() {
        // Label
        JLabel headerLabel = new JLabel(String.valueOf(headerStr));
        headerLabel.setFont(FontManager.getRunescapeBoldFont());
        headerLabel.setForeground(ColorScheme.BRAND_ORANGE);
        headerLabel.setHorizontalAlignment(JLabel.CENTER);


        leftHeader.setLayout(new BoxLayout(leftHeader, BoxLayout.X_AXIS));
        leftHeader.setBackground(HEADER_BG_COLOR);

        buildCollapseBtn();

        leftHeader.add(Box.createRigidArea(new Dimension(5, 0)));
        leftHeader.add(collapseBtn);
        leftHeader.add(Box.createRigidArea(new Dimension(10, 0)));
        leftHeader.add(headerLabel);
    }

    void buildCollapseBtn() {

        SwingUtil.removeButtonDecorations(collapseBtn);
        collapseBtn.setIcon(EXPAND_ICON);
        collapseBtn.setSelectedIcon(COLLAPSE_ICON);
        SwingUtil.addModalTooltip(collapseBtn, "Expand Table", "Collapse Table");
        collapseBtn.setBackground(HEADER_BG_COLOR);
        collapseBtn.setUI(new BasicButtonUI()); // substance breaks the layout
        collapseBtn.addActionListener(evt -> toggleCollapse());
        Util.showHandCursorOnHover(collapseBtn);
    }

    void buildHeaderContainer() {
        headerContainer.setLayout(new BorderLayout());
        headerContainer.setBackground(HEADER_BG_COLOR);
        headerContainer.setPreferredSize(new Dimension(0, 40));

        Util.showHandCursorOnHover(headerContainer);
        headerContainer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                toggleCollapse();
            }

            @Override
            public void mouseEntered(MouseEvent evt) {
                headerContainer.setBackground(HOVER_COLOR);
                leftHeader.setBackground(HOVER_COLOR);
                collapseBtn.setBackground(HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                headerContainer.setBackground(HEADER_BG_COLOR);
                leftHeader.setBackground(HEADER_BG_COLOR);
                collapseBtn.setBackground(HEADER_BG_COLOR);
            }
        });

        headerContainer.add(leftHeader, BorderLayout.WEST);
        add(headerContainer);

    }

    void buildItemsContainer() {
        int i = 0;
        for (WikiItem item : items) {
            WikiItemPanel itemPanel = new WikiItemPanel(item, i > 0, false, config);
            itemPanels.add(itemPanel);
            itemsContainer.add(itemPanel);
            i++;
        }

        itemsContainer.setLayout(new BoxLayout(itemsContainer, BoxLayout.Y_AXIS));
        add(itemsContainer);
    }


    void collapse() {
        if (!isCollapsed()) {
            collapseBtn.setSelected(true);
            itemsContainer.setVisible(false);
        }
    }

    void expand() {
        if (isCollapsed()) {
            collapseBtn.setSelected(false);
            itemsContainer.setVisible(true);
        }
    }

    void toggleCollapse() {
        if (isCollapsed()) {
            expand();
        } else {
            collapse();
        }
    }

    boolean isCollapsed() {
        return collapseBtn.isSelected();
    }

    void togglePercentMode() {
        for (WikiItemPanel item : itemPanels) {
            item.togglePercentMode();
        }
    }
}
