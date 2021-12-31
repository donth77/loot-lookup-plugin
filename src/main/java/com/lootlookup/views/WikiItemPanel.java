package com.lootlookup.views;

import com.lootlookup.LootLookupConfig;
import com.lootlookup.osrswiki.WikiItem;
import com.lootlookup.osrswiki.WikiScraper;
import com.lootlookup.utils.Util;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.IconTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.text.NumberFormat;

public class WikiItemPanel extends JPanel {
    private LootLookupConfig config;

    private String imageUrl;
    private String itemName;
    private int quantity;
    private String rarityStr;
    private double rarity;
    private int price;

    private boolean percentMode = false;

    private NumberFormat nf = NumberFormat.getInstance();
    Color bgColor = ColorScheme.DARKER_GRAY_COLOR;
    Color hoverColor = bgColor.brighter();

    JLabel rarityLabel = new JLabel();
    JPanel imageContainer = new JPanel(new BorderLayout());
    JPanel leftSidePanel = new JPanel(new GridLayout(2, 1));

    public WikiItemPanel(WikiItem item, boolean showSeparator, boolean percentMode, LootLookupConfig config) {
        this.config = config;

        this.imageUrl = item.getImageUrl();
        this.itemName = item.getName();
        this.quantity = item.getQuantity();
        this.rarityStr = item.getRarityStr();
        this.rarity = item.getRarity();
        this.price = item.getPrice();

        this.percentMode = percentMode;

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
        Util.showHandCursorOnHover(container);
        container.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                String wikiUrl = WikiScraper.getWikiUrl(itemName);
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

        add(container);
    }

    private void downloadImage(JLabel imageLabel) {
        try {
            Util.downloadImage(this.imageUrl, (image) -> {
                imageLabel.setIcon(new ImageIcon(image));
                imageContainer.setBorder(new EmptyBorder(0, 5, 0, Math.max(30 - image.getWidth(), 5)));
            });
        } catch (Exception error) {
        }
    }

    private JPanel buildImagePanel() {
        imageContainer.setSize(30, imageContainer.getHeight());
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(new ImageIcon(IconTextField.class.getResource(IconTextField.Icon.LOADING_DARKER.getFile()))); // set loading icon

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

        rarityLabel.setFont(FontManager.getRunescapeSmallFont());
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

        String quantityStr = quantity > 0 ? "x" + nf.format(quantity) : "N/A";
        JLabel quantityLabel = new JLabel();
        quantityLabel.setText(config != null && config.showQuantity() ? quantityStr : "");
        quantityLabel.setFont(FontManager.getRunescapeSmallFont());
        quantityLabel.setBorder(new EmptyBorder(0, 0, 3, 2));
        quantityLabel.setHorizontalAlignment(JLabel.RIGHT);
        quantityLabel.setVerticalAlignment(JLabel.CENTER);

        String priceLabelStr = price > 0 ? nf.format(price) + "gp" : "Not sold";
        if(itemName.equals("Nothing")) {
            priceLabelStr = "";
        }
        JLabel priceLabel = new JLabel();
        priceLabel.setText(config != null && config.showPrice() ? priceLabelStr : "");
        priceLabel.setFont(FontManager.getRunescapeSmallFont());
        priceLabel.setForeground(ColorScheme.BRAND_ORANGE);
        priceLabel.setVerticalAlignment(JLabel.CENTER);

        rightSidePanel.add(quantityLabel);
        rightSidePanel.add(priceLabel);

        return rightSidePanel;
    }

    void togglePercentMode() {
        percentMode = !this.percentMode;

        setRarityLabelText();
    }

    void setRarityLabelText() {
        String rarityLabelStr = rarityStr.contains(";") || rarityStr.equals("Always") ? rarityStr : Util.convertDecimalToFraction(rarity);
        if (percentMode) {
            rarityLabelStr = Util.toPercentage(rarity, 2);
        }
        rarityLabel.setText(rarityLabelStr);
        if (config != null && !config.showRarity()) {
            rarityLabel.setText("");
        }
    }
}
