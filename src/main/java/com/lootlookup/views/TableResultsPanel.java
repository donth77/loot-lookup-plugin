package com.lootlookup.views;

import com.lootlookup.LootLookupConfig;
import com.lootlookup.osrswiki.DropTableSection;
import com.lootlookup.osrswiki.WikiItem;
import com.lootlookup.utils.Util;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.SwingUtil;
import okhttp3.OkHttpClient;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TableResultsPanel extends JPanel {
    private LootLookupConfig config;
    private final OkHttpClient okHttpClient;
    private DropTableSection[] dropTableSections;
    private ViewOption viewOption;
    private JButton collapseBtn;
    private JButton percentButton;

    private final List<TableBox> boxes = new ArrayList<>();
    private int selectedTabIndex;

    private final JPanel dropTableContent = new JPanel();

    // Sub-table keys the user has hidden via the filter popover. Reset
    // whenever a new TableResultsPanel is constructed (on a new search),
    // preserved across tab switches within the same lookup.
    private final Set<String> hiddenSubTableKeys = new HashSet<>();

    // Timestamp of the last filter popup close, used so a button click that
    // dismisses the popup doesn't immediately re-open it in the same tick.
    private long filterMenuClosedAt = 0L;

    public TableResultsPanel(LootLookupConfig config, OkHttpClient okHttpClient, DropTableSection[] dropTableSections, ViewOption viewOption, JButton collapseButton, JButton percentButton, int selectedTabIndex) {
        this.config = config;
        this.okHttpClient = okHttpClient;
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
        Map<String, WikiItem[]> dropTables = selectedSection != null
                ? reorderPinnedToTop(selectedSection.getTable(), config != null ? config.pinnedSubTables() : "")
                : Collections.emptyMap();

        if (dropTableSections.length > 1) {
            dropTableContent.add(Box.createRigidArea(new Dimension(0, 5)));

            JPanel labelContainer = new JPanel(new BorderLayout());

            Font headerFont = FontManager.getRunescapeBoldFont();
            String dropsHeaderText = Util.truncateToFit(headerFont, selectedSection.getHeader(), PluginPanel.PANEL_WIDTH);

            JLabel sectionHeaderLabel = new JLabel(dropsHeaderText);
            sectionHeaderLabel.setFont(headerFont);
            sectionHeaderLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            if (!dropsHeaderText.equals(selectedSection.getHeader())) {
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

        String pinnedRaw = config != null ? config.pinnedSubTables() : "";
        for (Map.Entry<String, WikiItem[]> entry : dropTables.entrySet()) {
            String tableHeader = entry.getKey();
            if (hiddenSubTableKeys.contains(tableHeader)) {
                continue;
            }
            boolean isPinned = matchesPinKeyword(tableHeader, pinnedRaw);
            TableBox tableBox = new TableBox(config, okHttpClient, entry.getValue(), viewOption, tableHeader, percentButton, isPinned);
            boxes.add(tableBox);

            dropTableContent.add(tableBox);
            dropTableContent.add(Box.createRigidArea(new Dimension(0, 5)));
        }
    }

    /**
     * Pins sections whose keys contain any of the configured pinning
     * keywords (case-insensitive substring match) to the top of the drop
     * panel, preserving both the original ordering of non-pinned entries
     * and the relative order of pinned entries as they appear in the map.
     */
    static Map<String, WikiItem[]> reorderPinnedToTop(Map<String, WikiItem[]> tables, String pinnedRaw) {
        if (tables == null || tables.isEmpty()) {
            return tables;
        }
        if (pinnedRaw == null || pinnedRaw.trim().isEmpty()) {
            return tables;
        }

        LinkedHashMap<String, WikiItem[]> pinned = new LinkedHashMap<>();
        LinkedHashMap<String, WikiItem[]> rest = new LinkedHashMap<>(tables);

        for (String keyword : pinnedRaw.split(",")) {
            String keywordLower = keyword.trim().toLowerCase();
            if (keywordLower.isEmpty()) {
                continue;
            }
            Iterator<Map.Entry<String, WikiItem[]>> it = rest.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, WikiItem[]> entry = it.next();
                if (entry.getKey().toLowerCase().contains(keywordLower)) {
                    pinned.put(entry.getKey(), entry.getValue());
                    it.remove();
                }
            }
        }

        if (pinned.isEmpty()) {
            return tables;
        }

        LinkedHashMap<String, WikiItem[]> result = new LinkedHashMap<>(pinned);
        result.putAll(rest);
        return result;
    }

    /**
     * Returns true if the given section key matches any of the comma-separated
     * pinning keywords (case-insensitive substring match).
     */
    static boolean matchesPinKeyword(String key, String pinnedRaw) {
        if (key == null || pinnedRaw == null || pinnedRaw.trim().isEmpty()) {
            return false;
        }
        String keyLower = key.toLowerCase();
        for (String keyword : pinnedRaw.split(",")) {
            String kw = keyword.trim().toLowerCase();
            if (!kw.isEmpty() && keyLower.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the user has hidden at least one section via the
     * filter popover. Used by the toolbar filter button to swap between its
     * active and inactive icons.
     */
    boolean hasActiveFilter() {
        return !hiddenSubTableKeys.isEmpty();
    }

    /**
     * Opens the filter popover if it isn't currently active, or skips
     * opening when the most recent close happened within the same click
     * that just closed it. This makes the filter toolbar button behave
     * as a toggle: clicking once shows the menu, clicking again hides it.
     */
    void toggleFilterMenu(Component anchor, Runnable onFilterChanged) {
        // Swing dismisses the popup before firing the button's action
        // listener. If the close timestamp is fresh enough to be from that
        // same click, treat this as "user wanted to close the menu" and
        // leave it closed.
        if (System.currentTimeMillis() - filterMenuClosedAt < 250) {
            return;
        }
        showFilterMenu(anchor, onFilterChanged);
    }

    /**
     * Opens a popup menu anchored under the given component with Check all
     * and Uncheck all shortcuts followed by a JCheckBoxMenuItem for every
     * section in the currently selected drop table. Toggling an item
     * updates the hidden set and re-renders the drop table content in
     * place. The {@code onFilterChanged} callback is invoked after each
     * toggle so the owner of the anchor button can refresh its icon state.
     */
    void showFilterMenu(Component anchor, Runnable onFilterChanged) {
        DropTableSection selectedSection = selectedTabIndex < dropTableSections.length
                ? dropTableSections[selectedTabIndex] : null;
        if (selectedSection == null || selectedSection.getTable() == null
                || selectedSection.getTable().isEmpty()) {
            return;
        }
        Set<String> allKeys = selectedSection.getTable().keySet();

        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                filterMenuClosedAt = System.currentTimeMillis();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        JMenuItem checkAll = new JMenuItem("Check all");
        checkAll.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        checkAll.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        checkAll.addActionListener(evt -> {
            hiddenSubTableKeys.clear();
            rebuildAndNotify(onFilterChanged);
        });
        menu.add(checkAll);

        JMenuItem uncheckAll = new JMenuItem("Uncheck all");
        uncheckAll.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        uncheckAll.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        uncheckAll.addActionListener(evt -> {
            hiddenSubTableKeys.addAll(allKeys);
            rebuildAndNotify(onFilterChanged);
        });
        menu.add(uncheckAll);

        menu.addSeparator();

        for (String key : allKeys) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem("  " + key, !hiddenSubTableKeys.contains(key));
            item.setIconTextGap(8);
            item.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            item.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            item.addActionListener(evt -> {
                if (item.isSelected()) {
                    hiddenSubTableKeys.remove(key);
                } else {
                    hiddenSubTableKeys.add(key);
                }
                rebuildAndNotify(onFilterChanged);
            });
            menu.add(item);
        }

        menu.show(anchor, 0, anchor.getHeight());
    }

    private void rebuildAndNotify(Runnable onFilterChanged) {
        SwingUtilities.invokeLater(() -> {
            remove(dropTableContent);
            SwingUtil.fastRemoveAll(dropTableContent);
            buildDropTableContent();
            add(dropTableContent);
            revalidate();
            repaint();
            if (onFilterChanged != null) {
                onFilterChanged.run();
            }
        });
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

    void setSelectedIndex(int index) {
        selectedTabIndex = index;
    }

}