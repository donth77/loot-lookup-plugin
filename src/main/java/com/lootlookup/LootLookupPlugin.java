package com.lootlookup;

import com.google.inject.Provides;

import javax.inject.Inject;
import javax.swing.*;
import java.util.Arrays;

import com.lootlookup.utils.Constants;
import com.lootlookup.utils.Icons;
import com.lootlookup.views.LootLookupPanel;
import net.runelite.api.*;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
        name = Constants.PLUGIN_NAME
)
public class LootLookupPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private LootLookupConfig config;

    private LootLookupPanel panel;
    private NavigationButton navButton;

    @Override
    protected void startUp() {
        panel = new LootLookupPanel(config);

        navButton =
                NavigationButton.builder()
                        .tooltip(Constants.PLUGIN_NAME)
                        .icon(Icons.NAV_BUTTON)
                        .priority(Constants.DEFAULT_PRIORITY)
                        .panel(panel)
                        .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() {
        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (event.getGroup().equals(Constants.CONFIG_GROUP)) {
            switch (event.getKey()) {
                case "showRarity":
                case "showQuantity":
                case "showPrice":
                case "priceType":
                case "disableItemLinks":
                case "commonColor":
                case "rareColor":
                case "superRareColor":
                case "priceColor":
                case "gridRowOption":
                    if (panel != null) {
                        panel.refreshMainPanel();
                    }
            }
        }
    }

    /**
     * Insert option adjacent to "Examine" when target is attackable NPC
     *
     * @param event
     */
    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        final NPC[] cachedNPCs = client.getCachedNPCs();
        MenuEntry[] menuEntries = event.getMenuEntries();

        boolean isTargetAttackableNPC = false;
        String targetMonsterName = "";
        int combatLevel = 0;
        int monsterId = -1;

        for (MenuEntry menuEntry : menuEntries) {
            MenuAction menuType = menuEntry.getType();

            if (menuType == MenuAction.EXAMINE_NPC || menuType == MenuAction.NPC_SECOND_OPTION || menuType == MenuAction.NPC_FIFTH_OPTION) {
                String optionText = menuEntry.getOption();
                int id = menuEntry.getIdentifier();

                if (id < cachedNPCs.length) {
                    NPC target = cachedNPCs[id];

                    if (target != null) {
                        combatLevel = target.getCombatLevel();
                        monsterId = target.getId();

                        if (optionText.equals("Attack") && combatLevel > 0) {
                            isTargetAttackableNPC = true;
                            targetMonsterName = target.getName();
                        }
                    }
                }
            }
        }

        if (isTargetAttackableNPC && !config.disableMenuOption()) {
            MenuEntry entryToAppendOn = menuEntries[menuEntries.length - 1];

            int idx = Arrays.asList(menuEntries).indexOf(entryToAppendOn);

            String finalTargetMonsterName = targetMonsterName;
            int finalCombatLevel = combatLevel;
            int finalMonsterId = monsterId;

            client
                    .createMenuEntry(idx + 1)
                    .setOption("Lookup Drops")
                    .setTarget(entryToAppendOn.getTarget())
                    .setIdentifier(entryToAppendOn.getIdentifier())
                    .setParam1(entryToAppendOn.getParam1())
                    .setType(MenuAction.of(MenuAction.RUNELITE.getId()))
                    .onClick(
                            evt -> {
                                selectNavButton();
                                panel.lookupMonsterDrops(finalTargetMonsterName, finalCombatLevel, finalMonsterId);
                            });
        }
    }


    @Provides
    LootLookupConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(LootLookupConfig.class);
    }

    public void selectNavButton() {
        SwingUtilities.invokeLater(
                () -> {
                    if (!navButton.isSelected()) {
                        navButton.getOnSelect().run();
                    }
                });
    }
}