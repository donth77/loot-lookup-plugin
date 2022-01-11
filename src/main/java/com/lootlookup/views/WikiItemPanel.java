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

    private static int maxNameLength = 22;
    private static int labelsMaxLength = 25;

    public WikiItemPanel(WikiItem item, LootLookupConfig config, boolean showSeparator, JButton percentButton) {
        this.item = item;
        this.config = config;

        this.imageUrl = item.getImageUrl();
        this.itemName = item.getName();

        this.percentBtn = percentButton;

        if (itemName.length() > maxNameLength) {
            itemName = itemName.replaceAll("\\(.*\\)", "").trim(); // Don't display any text in parentheses if name is too long
        }

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

        if (itemName.length() > maxNameLength) {
            // Adjust layout to accomodate long item names
            JPanel itemImage = buildImagePanel();
            JPanel topPanel = buildTopPanel();
            JPanel bottomPanel = buildBottomPanel();
            paddingContainer.add(itemImage, BorderLayout.WEST);
            JPanel rightCol = new JPanel();
            rightCol.setBackground(new Color(0, 0, 0, 0));
            rightCol.setLayout(new BorderLayout());
            rightCol.add(topPanel, BorderLayout.NORTH);
            rightCol.add(bottomPanel, BorderLayout.SOUTH);
            paddingContainer.add(rightCol, BorderLayout.EAST);
            setPreferredSize(new Dimension(0, 50));
        } else {
            paddingContainer.add(leftPanel, BorderLayout.WEST);
            paddingContainer.add(rightPanel, BorderLayout.EAST);
        }

        container.add(paddingContainer);

        rarityLabel.setFont(FontManager.getRunescapeSmallFont());
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
        }

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


        rightSidePanel.add(quantityLabel);
        rightSidePanel.add(priceLabel);

        return rightSidePanel;
    }

    private JPanel buildTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel itemNameLabel = new JLabel(itemName);
        itemNameLabel.setBorder(new EmptyBorder(0, 0, 0, 0));
        itemNameLabel.setFont(FontManager.getRunescapeBoldFont());
        itemNameLabel.setHorizontalAlignment(JLabel.LEFT);
        itemNameLabel.setVerticalAlignment(JLabel.TOP);

        setQuantityLabelText();
        quantityLabel.setFont(FontManager.getRunescapeSmallFont());
        quantityLabel.setBorder(new EmptyBorder(0, 0, 0, 0));
        quantityLabel.setHorizontalAlignment(JLabel.RIGHT);
        quantityLabel.setVerticalAlignment(JLabel.TOP);

        topPanel.add(itemNameLabel, BorderLayout.WEST);
        topPanel.add(quantityLabel, BorderLayout.EAST);
        topPanel.setBackground(new Color(0, 0, 0, 0));
        return topPanel;
    }


    private JPanel buildBottomPanel() {
        JPanel botPanel = new JPanel(new BorderLayout());

        rarityLabel.setHorizontalAlignment(JLabel.LEFT);
        rarityLabel.setVerticalAlignment(JLabel.CENTER);
        setRarityLabelText();

        setPriceLabelText();
        priceLabel.setFont(FontManager.getRunescapeSmallFont());
        priceLabel.setHorizontalAlignment(JLabel.RIGHT);
        priceLabel.setVerticalAlignment(JLabel.CENTER);
        botPanel.add(rarityLabel, BorderLayout.WEST);
        botPanel.add(priceLabel, BorderLayout.EAST);
        botPanel.setBackground(new Color(0, 0, 0, 0));
        return botPanel;
    }

    void setQuantityLabelText() {
        if (config != null && !config.showQuantity()) {
            quantityLabel.setText("");
        } else {
            quantityLabel.setText((itemName + item.getQuantityLabelText()).length() > labelsMaxLength ? item.getQuantityLabelTextShort() : item.getQuantityLabelText());
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
            priceLabel.setText((itemName + item.getPriceLabelText()).length() > labelsMaxLength ? item.getPriceLabelTextShort() : item.getPriceLabelText());
        }
    }
}
