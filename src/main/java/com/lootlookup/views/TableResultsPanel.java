package com.lootlookup.views;

import com.lootlookup.LootLookupConfig;
import com.lootlookup.osrswiki.DropTableSection;
import com.lootlookup.osrswiki.WikiItem;
import com.lootlookup.utils.Util;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TableResultsPanel extends JPanel {
    private LootLookupConfig config;
    private DropTableSection[] dropTableSections;
    private ViewOption viewOption;
    private JButton collapseBtn;
    private JButton percentButton;

    private final List<TableBox> boxes = new ArrayList<>();
    private int selectedTabIndex = 0;

    private final JPanel dropTableContent = new JPanel();
    private final int maxHeaderLength = 31;

    public TableResultsPanel(LootLookupConfig config, DropTableSection[] dropTableSections, ViewOption viewOption, JButton collapseButton, JButton percentButton, int selectedTabIndex) {
        this.config = config;
        this.dropTableSections = dropTableSections;
        this.viewOption = viewOption;
        this.collapseBtn = collapseButton;
        this.percentButton = percentButton;
        this.selectedTabIndex = selectedTabIndex;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        dropTableContent.setLayout(new BoxLayout(dropTableContent, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(this);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);

        if (dropTableSections.length > 1) {
            MaterialTabGroup tabGroup = new MaterialTabGroup();
            tabGroup.setLayout(new GridLayout(1, dropTableSections.length, 7, 7));

            for (int i = 0; i < dropTableSections.length; i++) {
                MaterialTab tab = new MaterialTab(String.valueOf(i + 1), tabGroup, null);
                buildMaterialTab(tab, i);
                if (i == selectedTabIndex) {
                    tab.select();
                }
                tabGroup.addTab(tab);
            }
            add(tabGroup);
        }
        buildDropTableContent();
        add(dropTableContent);
    }

    void buildMaterialTab(MaterialTab tab, int index) {
        if (index < dropTableSections.length) {
            // Tooltip text
            tab.setToolTipText(dropTableSections[index].getHeader());
            // Switch Tab handler
            tab.setOnSelectEvent(() -> {
                selectedTabIndex = index;

                // Switch tab - update with new drop table content
                SwingUtilities.invokeLater(() -> {
                    remove(dropTableContent);
                    SwingUtil.fastRemoveAll(dropTableContent);
                    buildDropTableContent();
                    add(dropTableContent);
                });
                return true;
            });
            // Tab Decorations
            tab.setOpaque(true);
            tab.setVerticalAlignment(SwingConstants.CENTER);
            tab.setHorizontalAlignment(SwingConstants.CENTER);
            tab.setBackground(ColorScheme.DARKER_GRAY_COLOR);

            tab.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseEntered(MouseEvent e)
                {
                    MaterialTab tab = (MaterialTab) e.getSource();
                    tab.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
                }

                @Override
                public void mouseExited(MouseEvent e)
                {
                    MaterialTab tab = (MaterialTab) e.getSource();
                    tab.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                }
            });
            Util.showHandCursorOnHover(tab);
        }
    }

    void buildDropTableContent() {
        DropTableSection selectedSection = selectedTabIndex < dropTableSections.length ? dropTableSections[selectedTabIndex] : null;
        Map<String, WikiItem[]> dropTables = selectedSection != null ? selectedSection.getTable() : Collections.emptyMap();

        if (dropTableSections.length > 1) {
            dropTableContent.add(Box.createRigidArea(new Dimension(0, 5)));

            JPanel labelContainer = new JPanel(new BorderLayout());
            
            String dropsHeaderText = selectedSection.getHeader();
            if (dropsHeaderText.length() > maxHeaderLength) {
                dropsHeaderText = dropsHeaderText.substring(0, maxHeaderLength) + "…";
            }

            JLabel sectionHeaderLabel = new JLabel(dropsHeaderText);
            sectionHeaderLabel.setFont(FontManager.getRunescapeBoldFont());
            sectionHeaderLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            if (dropsHeaderText.endsWith("…")) {
                sectionHeaderLabel.setToolTipText(selectedSection.getHeader());
            }

            labelContainer.add(sectionHeaderLabel, BorderLayout.WEST);
            dropTableContent.add(labelContainer);

            dropTableContent.add(Box.createRigidArea(new Dimension(0, 5)));
            JPanel separator = new JPanel();
            separator.setPreferredSize(new Dimension(0, 6));
            separator.setBorder(new MatteBorder(1,0,0,0, ColorScheme.DARKER_GRAY_COLOR));
            dropTableContent.add(separator);
        }

        for (Map.Entry<String, WikiItem[]> entry : dropTables.entrySet()) {
            String tableHeader = entry.getKey();
            TableBox tableBox = new TableBox(config, entry.getValue(), viewOption, tableHeader, percentButton);
            boxes.add(tableBox);

            dropTableContent.add(tableBox);
            dropTableContent.add(Box.createRigidArea(new Dimension(0, 5)));
        }
    }

    void toggleCollapse() {
        for (TableBox box : boxes) {
            if (!collapseBtn.isSelected()) {
               box.expand();
            } else if (!box.isCollapsed()) {
                box.collapse();
            }
        }
    }

    String getSelectedHeader() {
        DropTableSection selectedSection = selectedTabIndex < dropTableSections.length ? dropTableSections[selectedTabIndex] : null;
        if (selectedSection != null) {
            return selectedSection.getHeader();
        }
        return null;
    }

    int getSelectedIndex() {
        return selectedTabIndex;
    }

    void resetSelectedIndex() {
        selectedTabIndex = 0;
    }

}
