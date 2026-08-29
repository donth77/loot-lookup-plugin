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
import okhttp3.OkHttpClient;

@PluginDescriptor(name = Constants.PLUGIN_NAME)
public class LootLookupPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private LootLookupConfig config;
    @Inject
    private ConfigManager configManager;
    @Inject
    public OkHttpClient okHttpClient;

    private LootLookupPanel panel;
    private NavigationButton navButton;

    @Override
    protected void startUp() {
        migrateDisableMenuOption();

        panel = new LootLookupPanel(config, okHttpClient);

        navButton = NavigationButton.builder()
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
     * Insert option adjacent to "Examine" when target is an attackable or
     * pickpocketable NPC.
     *
     * @param event
     */
    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        // Bail before scanning the menu: under HOLD_SHIFT this is false for most
        // right-clicks, and the scan below does an NPC lookup per menu entry.
        if (!shouldShowRightClickMenuOption()) {
            return;
        }

        final var npcs = client.getTopLevelWorldView().npcs();
        MenuEntry[] menuEntries = event.getMenuEntries();

        boolean shouldShowLookup = false;
        String targetMonsterName = "";
        int combatLevel = 0;
        int monsterId = -1;

        for (MenuEntry menuEntry : menuEntries) {
            MenuAction menuType = menuEntry.getType();

            if (menuType == MenuAction.EXAMINE_NPC
                    || menuType == MenuAction.NPC_FIRST_OPTION
                    || menuType == MenuAction.NPC_SECOND_OPTION
                    || menuType == MenuAction.NPC_THIRD_OPTION
                    || menuType == MenuAction.NPC_FOURTH_OPTION
                    || menuType == MenuAction.NPC_FIFTH_OPTION) {
                String optionText = menuEntry.getOption();
                int id = menuEntry.getIdentifier();

                NPC target;
                try {
                    target = npcs.byIndex(id);
                } catch (ArrayIndexOutOfBoundsException ignored) {
                    continue;
                }

                if (target != null) {
                    combatLevel = target.getCombatLevel();
                    monsterId = target.getId();

                    boolean isAttack = optionText.equals("Attack") && combatLevel > 0;
                    boolean isPickpocket = optionText.equals("Pickpocket");
                    if (isAttack || isPickpocket) {
                        shouldShowLookup = true;
                        targetMonsterName = target.getName();
                    }
                }
            }
        }

        if (shouldShowLookup && !isMonsterExcluded(targetMonsterName, monsterId)) {
            MenuEntry entryToAppendOn = menuEntries[menuEntries.length - 1];

            int idx = Arrays.asList(menuEntries).indexOf(entryToAppendOn);

            String finalTargetMonsterName = targetMonsterName;
            int finalCombatLevel = combatLevel;
            int finalMonsterId = monsterId;

            client
                    .getMenu()
                    .createMenuEntry(idx - 1)
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

    /**
     * "disableMenuOption" (boolean) was replaced by "rightClickMenuOption" (enum) in 1.2.3 (#51).
     * Carry the old value over so users who had hidden the menu option keep it hidden.
     */
    private void migrateDisableMenuOption() {
        String legacy = configManager.getConfiguration(Constants.CONFIG_GROUP, "disableMenuOption");
        if (legacy == null) {
            return;
        }

        // RuneLite writes config defaults before startUp() runs, so "rightClickMenuOption"
        // is already set to ALWAYS_SHOW by now -- don't guard on it being absent. The legacy
        // key only survives a single upgrade, so its value wins here.
        configManager.setConfiguration(Constants.CONFIG_GROUP, "rightClickMenuOption",
                Boolean.parseBoolean(legacy)
                        ? LootLookupConfig.RightClickMenuOption.DISABLE
                        : LootLookupConfig.RightClickMenuOption.ALWAYS_SHOW);

        configManager.unsetConfiguration(Constants.CONFIG_GROUP, "disableMenuOption");
    }

    private boolean shouldShowRightClickMenuOption() {
        switch (config.rightClickMenuOption()) {
            case ALWAYS_SHOW:
                return true;
            case HOLD_SHIFT:
                return client.isKeyPressed(KeyCode.KC_SHIFT);
            case DISABLE:
            default:
                return false;
        }
    }

    private boolean isMonsterExcluded(String name, int id) {
        String excluded = config.excludedMonsters();
        if (excluded == null || excluded.trim().isEmpty()) {
            return false;
        }
        String idStr = String.valueOf(id);
        for (String entry : excluded.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.equalsIgnoreCase(name) || trimmed.equals(idStr)) {
                return true;
            }
        }
        return false;
    }

    @Provides
    LootLookupConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(LootLookupConfig.class);
    }

    public void selectNavButton() {
        SwingUtilities.invokeLater(
                () -> {
                    clientToolbar.openPanel(navButton);
                });
    }
}
