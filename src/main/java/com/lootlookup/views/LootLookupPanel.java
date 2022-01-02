package com.lootlookup.views;

import com.lootlookup.LootLookupConfig;
import com.lootlookup.osrswiki.DropTableType;
import com.lootlookup.osrswiki.WikiItem;
import com.lootlookup.osrswiki.WikiScraper;
import com.lootlookup.utils.Util;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.Map;

import static com.lootlookup.utils.Icons.*;

public class LootLookupPanel extends PluginPanel {
    private LootLookupConfig config;
    private TableResultsPanel tablePanel;
    private Map<DropTableType, WikiItem[]> dropTables;
    private ViewOption viewOption = ViewOption.LIST;

    private IconTextField monsterSearchField = new IconTextField();

    private final JPanel mainPanel = new JPanel();

    private final JPanel actionsContainer = new JPanel();

    private final JPanel actionsLeft = new JPanel();
    private final JButton collapseBtn = new JButton();

    private final JPanel actionsRight = new JPanel();
    private final JButton percentBtn = new JButton();

    JRadioButton listBtn = new JRadioButton();
    JRadioButton gridBtn = new JRadioButton();
    JToggleButton externalLinkBtn = new JToggleButton();
    private final JPanel externalLinkBtnContainer = new JPanel();
    private final JPanel listBtnContainer = new JPanel();
    private final JPanel gridBtnContainer = new JPanel();
    ButtonGroup viewOptionGroup = new ButtonGroup();


    private final PluginErrorPanel errorPanel = new PluginErrorPanel();

    public LootLookupPanel(LootLookupConfig config) {
        this.config = config;

        if(config != null) {
            viewOption = config.viewOption();
        }

        // Layout
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Main Panel

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Search Field

        buildSearchField();

        // Actions Container

        actionsContainer.setLayout(new BorderLayout());
        actionsContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        actionsContainer.setPreferredSize(new Dimension(0, 29));

        // View Controls

        actionsLeft.setLayout(new BoxLayout(actionsLeft, BoxLayout.X_AXIS));
        actionsLeft.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        actionsRight.setLayout(new BoxLayout(actionsRight, BoxLayout.X_AXIS));
        actionsRight.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Collapse Button

        buildButton(collapseBtn, EXPAND_ICON, COLLAPSE_ICON, "Expand All", "Collapse All", evt -> {
            collapseBtn.setSelected(!collapseBtn.isSelected());
            tablePanel.toggleCollapse();
        });

        // List Button

        SwingUtil.removeButtonDecorations(listBtn);
        listBtn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        listBtn.setIcon(LIST_ICON_FADED);
        listBtn.setRolloverIcon(LIST_ICON_HOVER);
        listBtn.setSelectedIcon(LIST_ICON);
        Util.showHandCursorOnHover(listBtn);
        listBtn.setToolTipText("List");
        listBtn.addActionListener(evt -> {
            this.viewOption = ViewOption.LIST;
            refreshMainPanel();
        });
        listBtn.setSelected(viewOption == ViewOption.LIST);
        listBtnContainer.setLayout(new BorderLayout());
        listBtnContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);



        // Grid Button

        SwingUtil.removeButtonDecorations(gridBtn);
        gridBtn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        gridBtn.setIcon(GRID_ICON_FADED);
        gridBtn.setRolloverIcon(GRID_ICON_HOVER);
        gridBtn.setSelectedIcon(GRID_ICON);
        Util.showHandCursorOnHover(gridBtn);
        gridBtn.setToolTipText("Grid");
        gridBtn.addActionListener(evt -> {
            this.viewOption = ViewOption.GRID;
            refreshMainPanel();
        });
        gridBtn.setSelected(viewOption == ViewOption.GRID);
        gridBtnContainer.setLayout(new BorderLayout());
        gridBtnContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);


        viewOptionGroup.add(listBtn);
        viewOptionGroup.add(gridBtn);

        // Percent Button

        buildButton(percentBtn, PERCENT_ICON_FADED, PERCENT_ICON, "Rarity Percentage Off" , "Rarity Percentage On", evt -> {
            percentBtn.setSelected(!percentBtn.isSelected());
        });

        // External Link Button

        SwingUtil.removeButtonDecorations(externalLinkBtn);
        externalLinkBtn.setIcon(EXTERNAL_LINK_ICON);
        externalLinkBtn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        externalLinkBtn.setUI(new BasicButtonUI());
        externalLinkBtn.setRolloverIcon(EXTERNAL_LINK_ICON_HOVER);
        externalLinkBtn.setToolTipText("Wiki");
        Util.showHandCursorOnHover(externalLinkBtn);
        externalLinkBtn.addActionListener((evt) -> {
            String wikiUrl = WikiScraper.getWikiUrlForDrops(monsterSearchField.getText());
            try {
                Desktop.getDesktop().browse(new URL(wikiUrl).toURI());
            } catch (Exception e) {
            }
        });
        externalLinkBtnContainer.setLayout(new BorderLayout());


        // Error Panel - Empty State
        errorPanel.setContent("Loot Lookup", "Enter a monster name or select the in-game option.");


        add(monsterSearchField);
        add(mainPanel);
        add(errorPanel);

    }


    void rebuildMainPanel(Map<DropTableType, WikiItem[]> dropTables, ViewOption viewOption) {
        remove(errorPanel);
        SwingUtil.fastRemoveAll(mainPanel);

        tablePanel = new TableResultsPanel(config, dropTables, viewOption, collapseBtn, percentBtn);

        actionsLeft.add(Box.createRigidArea(new Dimension(5, 0)));
        actionsLeft.add(collapseBtn);

        listBtnContainer.add(listBtn, BorderLayout.CENTER);
        actionsRight.add(listBtnContainer);
        gridBtnContainer.add(gridBtn, BorderLayout.CENTER);
        actionsRight.add(gridBtnContainer);
        if (config != null & config.showRarity()) actionsRight.add(percentBtn);
        actionsRight.add(Box.createRigidArea(new Dimension(5, 0)));
        externalLinkBtnContainer.add(externalLinkBtn, BorderLayout.CENTER);
        actionsRight.add(externalLinkBtnContainer);

        actionsContainer.add(actionsLeft, BorderLayout.WEST);
        actionsContainer.add(actionsRight, BorderLayout.EAST);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(actionsContainer);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(tablePanel);

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    void resetMainPanel() {
        SwingUtil.fastRemoveAll(mainPanel);
        mainPanel.revalidate();
        mainPanel.repaint();
        add(errorPanel);
    }

    void buildSearchField() {
        monsterSearchField.setIcon(IconTextField.Icon.SEARCH);
        monsterSearchField.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
        monsterSearchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        monsterSearchField.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        monsterSearchField.setMinimumSize(new Dimension(0, 30));

        monsterSearchField.addActionListener(
                evt -> {
                    searchForMonsterName(monsterSearchField.getText());
                });
        monsterSearchField.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent evt) {
                        searchForMonsterName(monsterSearchField.getText());
                    }
                });
        monsterSearchField.addClearListener(
                () -> {
                    reset();
                });
    }

    void buildButton(JButton btn, ImageIcon icon, ImageIcon selectedIcon, String on, String off, ActionListener listener) {
        SwingUtil.removeButtonDecorations(btn);
        btn.setIcon(icon);
        btn.setSelectedIcon(selectedIcon);
        btn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        btn.setUI(new BasicButtonUI());

        SwingUtil.addModalTooltip(btn, on, off);
        Util.showHandCursorOnHover(btn);
        btn.addActionListener(listener);
    }


    void searchForMonsterName(String monsterName) {
        if (monsterName.isEmpty()) return;

        monsterSearchField.setEditable(false);
        monsterSearchField.setIcon(IconTextField.Icon.LOADING_DARKER);


        WikiScraper.getDropsByMonsterName(monsterName).whenCompleteAsync((dropTables, ex) -> {
            this.dropTables = dropTables;
            monsterSearchField.setIcon(dropTables.isEmpty() ? IconTextField.Icon.ERROR : IconTextField.Icon.SEARCH);
            monsterSearchField.setEditable(true);
            refreshMainPanel();
        });

    }

    void resetSearchField() {
        monsterSearchField.setIcon(IconTextField.Icon.SEARCH);
        monsterSearchField.setText("");
        monsterSearchField.setEditable(true);
    }

    public void reset() {
        SwingUtilities.invokeLater(() -> {
            resetSearchField();
            resetMainPanel();
        });
    }

    public void lookupMonsterDrops(String monsterName) {
        SwingUtilities.invokeLater(() -> {
            monsterSearchField.setText(monsterName);
            searchForMonsterName(monsterName);
        });
    }

    public void refreshMainPanel() {
        if (dropTables != null && !dropTables.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                rebuildMainPanel(dropTables, viewOption);
            });
        }
    }

}
