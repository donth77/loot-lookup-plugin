package com.lootlookup.views;

import com.lootlookup.LootLookupConfig;
import com.lootlookup.osrswiki.DropTableType;
import com.lootlookup.osrswiki.WikiItem;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TableResultsPanel extends JPanel {
    private LootLookupConfig config;
    private JButton collapseBtn;

    private final List<TableBox> boxes = new ArrayList<>();

    public TableResultsPanel(Map<DropTableType, WikiItem[]> dropTables, JButton collapseButton, LootLookupConfig config) {
        this.config = config;
        this.collapseBtn = collapseButton;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(this);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);

        for (Map.Entry<DropTableType, WikiItem[]> entry : dropTables.entrySet()) {
            DropTableType tableHeader = entry.getKey();
            TableBox tableBox = new TableBox(tableHeader, entry.getValue(), config);
            boxes.add(tableBox);

            add(tableBox);
            add(Box.createRigidArea(new Dimension(0, 5)));
        }
    }

    boolean isAllCollapsed() {
        return boxes.stream()
                .filter(TableBox::isCollapsed)
                .count() == boxes.size();
    }

    boolean isAllExpanded() {
        return boxes.stream()
                .filter(TableBox::isCollapsed)
                .count() == 0;
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

    void togglePercentMode() {
        for (TableBox box : boxes) {
            box.togglePercentMode();
        }
    }

}
