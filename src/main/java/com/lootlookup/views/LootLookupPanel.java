package com.lootlookup.views;

import com.lootlookup.LootLookupConfig;
import com.lootlookup.osrswiki.DropTableSection;
import com.lootlookup.osrswiki.WikiScraper;
import com.lootlookup.utils.Constants;
import com.lootlookup.utils.Util;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.SwingUtil;
import okhttp3.OkHttpClient;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

import static com.lootlookup.utils.Icons.*;

public class LootLookupPanel extends PluginPanel {
    private LootLookupConfig config;
    private OkHttpClient okHttpClient;
    private TableResultsPanel tablePanel;
    private DropTableSection[] dropTableSections;
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

    private int targetCombatLevel = 0;
    private int targetMonsterId = -1;

    public LootLookupPanel(LootLookupConfig config, OkHttpClient okHttpClient) {
        this.config = config;
        this.okHttpClient = okHttpClient;

        if (config != null) {
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

        buildButton(percentBtn, PERCENT_ICON_FADED, PERCENT_ICON, "Toggle Rarity Percentage", "Toggle Rarity Percentage", evt -> {
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
            String wikiUrl = WikiScraper.getWikiUrlForDrops(monsterSearchField.getText(), tablePanel.getSelectedHeader(), targetMonsterId);
            try {
                Desktop.getDesktop().browse(new URL(wikiUrl).toURI());
            } catch (Exception e) {
            }
        });
        externalLinkBtnContainer.setLayout(new BorderLayout());


        // Error Panel - Empty State
        errorPanel.setContent(Constants.PLUGIN_NAME, "Enter a monster name or select the in-game option.");


        add(monsterSearchField);
        add(mainPanel);
        add(errorPanel);

    }


    void rebuildMainPanel() {
        remove(errorPanel);
        SwingUtil.fastRemoveAll(mainPanel);

        int defaultSelectedIndex = getSelectedIndexForCombatLevel(targetCombatLevel);
        tablePanel = new TableResultsPanel(config, dropTableSections, viewOption, collapseBtn, percentBtn, tablePanel != null ? tablePanel.getSelectedIndex() : defaultSelectedIndex);

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
                    searchForMonsterName(monsterSearchField.getText(), 0, -1);
                });
        monsterSearchField.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent evt) {
                        searchForMonsterName(monsterSearchField.getText(), 0, -1);
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

    int getSelectedIndexForCombatLevel(int combatLevel) {
        if (combatLevel > 0) {
            for (int i = 0; i < dropTableSections.length; i++) {
                DropTableSection section = dropTableSections[i];
                String headerTextLower = section.getHeader().toLowerCase();
                if (headerTextLower.contains(String.valueOf(combatLevel))) {
                    return i;
                }

                try {
                    String[] headerTextTokens = headerTextLower.split("\\s+");
                    for (String token : headerTextTokens) {
                        if (token.contains("–")) {
                            String[] rangeTokens = token.split("–");
                            if (rangeTokens.length > 1 && rangeTokens[0].matches("\\d+") && rangeTokens[1].matches("\\d+")) {
                                int rangeLow = Integer.parseInt(rangeTokens[0]);
                                int rangeHigh = Integer.parseInt(rangeTokens[1]);

                                if (combatLevel >= rangeLow && combatLevel <= rangeHigh) {
                                    return i;
                                }
                            }
                        }
                    }
                } catch (Exception e) {}
            }
        }
        return 0;
    }


    void searchForMonsterName(String monsterName, int combatLevel, int monsterId) {
        if (monsterName.isEmpty()) return;

        monsterSearchField.setEditable(false);
        monsterSearchField.setIcon(IconTextField.Icon.LOADING_DARKER);

        targetCombatLevel = combatLevel;
        targetMonsterId = monsterId;

        WikiScraper.getDropsByMonster(okHttpClient, monsterName, monsterId).whenCompleteAsync((dropTableSections, ex) -> {
            this.dropTableSections = dropTableSections;
            if (tablePanel != null) {
                tablePanel.resetSelectedIndex();
                tablePanel.setSelectedIndex(getSelectedIndexForCombatLevel(combatLevel));
            }
            monsterSearchField.setIcon(dropTableSections.length == 0 ? IconTextField.Icon.ERROR : IconTextField.Icon.SEARCH);
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

    public void lookupMonsterDrops(String monsterName, int combatLevel, int monsterId) {
        targetCombatLevel = combatLevel;
        targetMonsterId = monsterId;

        SwingUtilities.invokeLater(() -> {
            monsterSearchField.setText(monsterName);
            searchForMonsterName(monsterName, combatLevel, monsterId);
        });
    }

    public void refreshMainPanel() {
        if (dropTableSections != null && dropTableSections.length > 0) {
            SwingUtilities.invokeLater(() -> {
                rebuildMainPanel();
            });
        }
    }

}
