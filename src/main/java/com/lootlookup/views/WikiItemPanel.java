package com.lootlookup.views;

import lombok.extern.slf4j.Slf4j;

import com.lootlookup.LootLookupConfig;
import com.lootlookup.osrswiki.WikiItem;
import com.lootlookup.osrswiki.WikiScraper;
import com.lootlookup.utils.Util;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import okhttp3.OkHttpClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;

@Slf4j
public class WikiItemPanel extends JPanel {
    private LootLookupConfig config;
    private final OkHttpClient okHttpClient;

    private WikiItem item;
    private String imageUrl;
    private String itemName;

    private JButton percentBtn;

    private final Color bgColor = ColorScheme.DARKER_GRAY_COLOR;
    private final Color hoverColor = bgColor.brighter();

    private final JLabel rarityLabel = new JLabel();
    private final JLabel priceLabel = new JLabel();
    private final JLabel quantityLabel = new JLabel();
    private final JPanel imageContainer = new JPanel(new BorderLayout());
    private final JPanel leftSidePanel = new JPanel(new GridLayout(2, 1));

    // Fixed layout costs: paddingContainer (4px) + imageContainer (~40px) + leftSidePanel padding (4px) + quantityLabel padding (2px)
    private static final int FIXED_HORIZONTAL_PADDING = 50;
    // Minimum gap between the name and the right-side labels so they never visually touch
    private static final int NAME_RIGHT_GAP = 5;

    private int getAvailableTextWidth() {
        return PluginPanel.PANEL_WIDTH - FIXED_HORIZONTAL_PADDING;
    }

    /**
     * Calculates the minimum pixel width that must be reserved for the right-side
     * quantity/price labels, using their shortest text variants. The right panel
     * uses a GridLayout(2,1), so its width is max(quantity, price).
     */
    private int getMinRightPanelWidth() {
        Font font = FontManager.getRunescapeSmallFont();
        int width = 0;

        if (config != null && config.showQuantity()) {
            int qShort = Util.getStringWidth(font, item.getQuantityLabelTextShort());
            int qMin = Util.getStringWidth(font, item.getQuantityValueText());
            width = Math.max(width, Math.min(qShort, qMin));
        }

        if (config != null && config.showPrice()) {
            String shortPrice = config.priceType() == PriceType.HA
                    ? item.getAlchemyPriceLabelTextShort() : item.getExchangePriceLabelTextShort();
            width = Math.max(width, Util.getStringWidth(font, shortPrice));
        }

        return width;
    }

    public WikiItemPanel(WikiItem item, LootLookupConfig config, boolean showSeparator, OkHttpClient okHttpClient,
            JButton percentButton) {
        this.item = item;
        this.config = config;

        this.imageUrl = item.getImageUrl();
        this.itemName = item.getName();
        this.okHttpClient = okHttpClient;

        this.percentBtn = percentButton;

        Font nameFont = FontManager.getRunescapeBoldFont();
        // Reserve room for the right-side labels so they don't overlap the name
        int reservedRight = getMinRightPanelWidth();
        int availableNameWidth = getAvailableTextWidth()
                - (reservedRight > 0 ? reservedRight + NAME_RIGHT_GAP : 0);

        if (Util.getStringWidth(nameFont, itemName) > availableNameWidth) {
            // Only remove known-redundant suffixes (poison variants shown on icon)
            itemName = itemName.replaceAll("\\s*\\(p\\+?\\+?\\)", "").trim();
        }
        itemName = Util.truncateToFit(nameFont, itemName, availableNameWidth);

        percentButton.addItemListener((evt) -> {
            setRarityLabelText();
        });
        ;

        setBorder(new EmptyBorder(0, 0, 5, 0));
        setLayout(new BorderLayout());
        setBackground(bgColor);

        JPanel container = new JPanel(new BorderLayout());

        JPanel paddingContainer = new JPanel(new BorderLayout());
        int padding = 2;
        paddingContainer.setBorder(new EmptyBorder(padding, padding, padding, padding));

        if (showSeparator)
            container.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ColorScheme.DARK_GRAY_COLOR));
        paddingContainer.setBackground(bgColor);

        JPanel leftPanel = buildLeftPanel();
        JPanel rightPanel = buildRightPanel();
        rightPanel.setBackground(bgColor);

        paddingContainer.add(leftPanel, BorderLayout.WEST);
        paddingContainer.add(rightPanel, BorderLayout.EAST);

        container.add(paddingContainer);

        rarityLabel.setFont(FontManager.getRunescapeSmallFont());
        rarityLabel.setForeground(config.commonColor());
        if (item.getRarity() > 0) {
            if (item.getRarity() <= 0.01) {
                rarityLabel.setForeground(config.rareColor());
            }
            if (item.getRarity() <= 0.001) {
                rarityLabel.setForeground(config.superRareColor());
            }
        }

        priceLabel.setFont(FontManager.getRunescapeSmallFont());
        priceLabel.setForeground(config.priceColor());

        if (!config.disableItemsLinks()) {
            Util.showHandCursorOnHover(container);
            container.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent evt) {
                    String wikiUrl = WikiScraper.getWikiUrl(item.getName());
                    try {
                        Desktop.getDesktop().browse(new URL(wikiUrl).toURI());
                    } catch (Exception e) {
                    }
                }

                @Override
                public void mouseEntered(MouseEvent evt) {
                    setBackground(hoverColor);
                    paddingContainer.setBackground(hoverColor);
                    leftSidePanel.setBackground(hoverColor);
                    rightPanel.setBackground(hoverColor);
                    imageContainer.setBackground(hoverColor);
                }

                @Override
                public void mouseExited(MouseEvent evt) {
                    setBackground(bgColor);
                    paddingContainer.setBackground(bgColor);
                    leftSidePanel.setBackground(bgColor);
                    rightPanel.setBackground(bgColor);
                    imageContainer.setBackground(bgColor);
                }
            });
        }
        if (itemName.endsWith("…")) {
            // If item name is truncated, show the full name on hover
            container.setToolTipText(item.getName());
        }

        add(container);
    }

    private void downloadImage(JLabel imageLabel) {
        try {
            Util.downloadImage(this.okHttpClient, this.imageUrl, (image) -> {
                BufferedImage img = item.isNoted() ? Util.getNotedImg(image) : image;
                imageLabel.setIcon(new ImageIcon(img));
                imageContainer.setBorder(new EmptyBorder(0, 5, 0, Math.max(30 - image.getWidth(), 5)));
            });
        } catch (Exception error) {
            log.error("Failed to download item image", error);
        }
    }

    private JPanel buildImagePanel() {
        imageContainer.setSize(30, imageContainer.getHeight());
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(new ImageIcon(IconTextField.class.getResource(IconTextField.Icon.LOADING_DARKER.getFile()))); // set
                                                                                                                         // loading
                                                                                                                         // icon

        new Thread(() -> downloadImage(imageLabel)).start();

        imageLabel.setSize(35, imageLabel.getWidth());

        imageContainer.add(imageLabel, BorderLayout.WEST);
        imageContainer.setSize(30, imageContainer.getHeight());
        imageContainer.setBackground(bgColor);
        return imageContainer;
    }

    private JPanel buildLeftPanel() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        JPanel itemImage = buildImagePanel();

        leftSidePanel.setBorder(new EmptyBorder(2, 2, 2, 2));
        leftSidePanel.setBackground(bgColor);

        JLabel itemNameLabel = new JLabel(itemName);
        itemNameLabel.setBorder(new EmptyBorder(0, 0, 3, 0));
        itemNameLabel.setFont(FontManager.getRunescapeBoldFont());
        itemNameLabel.setHorizontalAlignment(JLabel.LEFT);
        itemNameLabel.setVerticalAlignment(JLabel.CENTER);
        ;

        rarityLabel.setHorizontalAlignment(JLabel.LEFT);
        rarityLabel.setVerticalAlignment(JLabel.CENTER);
        setRarityLabelText();

        leftSidePanel.add(itemNameLabel);
        leftSidePanel.add(rarityLabel);

        container.add(itemImage);
        container.add(leftSidePanel);
        return container;
    }

    private JPanel buildRightPanel() {
        JPanel rightSidePanel = new JPanel(new GridLayout(2, 1));

        setQuantityLabelText();
        quantityLabel.setFont(FontManager.getRunescapeSmallFont());
        quantityLabel.setBorder(new EmptyBorder(0, 0, 3, 2));
        quantityLabel.setHorizontalAlignment(JLabel.RIGHT);
        quantityLabel.setVerticalAlignment(JLabel.CENTER);

        setPriceLabelText();
        priceLabel.setVerticalAlignment(JLabel.CENTER);
        priceLabel.setHorizontalAlignment(JLabel.RIGHT);

        rightSidePanel.add(quantityLabel);
        rightSidePanel.add(priceLabel);

        return rightSidePanel;
    }

    void setQuantityLabelText() {
        if (config != null && !config.showQuantity()) {
            quantityLabel.setText("");
            return;
        }

        Font font = FontManager.getRunescapeSmallFont();
        int nameWidth = Util.getStringWidth(FontManager.getRunescapeBoldFont(), itemName);
        int availableWidth = getAvailableTextWidth() - nameWidth - NAME_RIGHT_GAP;

        String fullText = item.getQuantityLabelText();
        String shortText = item.getQuantityLabelTextShort();
        String minText = item.getQuantityValueText();

        String text = Util.fitText(font, new String[]{fullText, shortText, minText}, availableWidth);
        quantityLabel.setText(text);

        if (!text.equals(fullText)) {
            quantityLabel.setToolTipText(fullText);
        }
    }

    void setRarityLabelText() {
        rarityLabel.setText(item.getRarityLabelText(percentBtn.isSelected()));
        if (config != null && !config.showRarity()) {
            rarityLabel.setText("");
        } else if (item.getRarity() < 0) {
            rarityLabel.setText(item.getRarityStr());
        }
    }

    void setPriceLabelText() {
        priceLabel.setText("");
        if (config != null && config.showPrice()) {
            Font font = FontManager.getRunescapeSmallFont();
            int nameWidth = Util.getStringWidth(FontManager.getRunescapeBoldFont(), itemName);
            int availableWidth = getAvailableTextWidth() - nameWidth - NAME_RIGHT_GAP;

            String scaledText = config.priceType() == PriceType.HA
                    ? item.getAlchemyPriceLabelTextScaled() : item.getExchangePriceLabelTextScaled();
            String shortText = config.priceType() == PriceType.HA
                    ? item.getAlchemyPriceLabelTextShort() : item.getExchangePriceLabelTextShort();
            String preciseText = config.priceType() == PriceType.HA
                    ? item.getAlchemyPriceLabelTextFull() : item.getExchangePriceLabelTextFull();

            String text = Util.fitText(font, new String[]{scaledText, shortText}, availableWidth);
            priceLabel.setText(text);

            if (!text.equals(preciseText)) {
                priceLabel.setToolTipText(preciseText);
            }
        }
    }
}
