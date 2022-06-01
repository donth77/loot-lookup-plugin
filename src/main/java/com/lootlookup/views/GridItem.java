package com.lootlookup.views;

import com.lootlookup.LootLookupConfig;
import com.lootlookup.osrswiki.WikiItem;
import com.lootlookup.utils.Util;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.IconTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

import static com.lootlookup.utils.Icons.noteImg;

public class GridItem extends JPanel {

    private WikiItem item;
    private LootLookupConfig config;
    private JButton percentBtn;

    private String rarityColorStr;
    private String priceColorStr;

    private final JPanel container = new JPanel();
    private final JLabel imageLabel = new JLabel();

    private final Color bgColor = ColorScheme.DARKER_GRAY_COLOR;

    private static int maxQuantityLength = 6;

    public GridItem(WikiItem item, LootLookupConfig config, JButton percentButton) {
        this.item = item;
        this.config = config;
        this.percentBtn = percentButton;

        priceColorStr = Util.colorToHex(config.priceColor());

        rarityColorStr = Util.colorToHex(config.commonColor());
        if (item.getRarity() > 0) {
            if (item.getRarity() <= 0.01) {
                rarityColorStr = Util.colorToHex(config.rareColor());
            }
            if (item.getRarity() <= 0.001) {
                rarityColorStr = Util.colorToHex(config.superRareColor());
            }
        }

        imageLabel.setIcon(new ImageIcon(IconTextField.class.getResource(IconTextField.Icon.LOADING_DARKER.getFile())));
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        container.setBackground(bgColor);

        setBorder(new EmptyBorder(1, 0, 0, 0));
        setLayout(new BorderLayout());

        new Thread(() -> {
            Util.downloadImage(item.getImageUrl(), (image) -> {
                BufferedImage img = item.getQuantityStr().endsWith(" (noted)") ? Util.getNotedImg(image) : image;
                imageLabel.setIcon(new ImageIcon(img));
            });
        }).start();

        JPanel bottomText = new JPanel();
        bottomText.setBackground(new Color(0, 0, 0, 0));

        bottomText.setLayout(new BorderLayout());
        String quantityLabelTextShort = item.getQuantityLabelTextShort();
        JLabel quantityLabel = new JLabel(quantityLabelTextShort.length() > maxQuantityLength ? item.getQuantityValueText() : quantityLabelTextShort);

        quantityLabel.setBackground(bgColor);
        quantityLabel.setFont(FontManager.getRunescapeSmallFont());

        setBackground(bgColor);

        setTooltipText();
        percentButton.addItemListener((evt) -> {
            setTooltipText();
        });

        container.add(imageLabel);
        add(container, BorderLayout.CENTER);
        if (config.showQuantity()) {
            bottomText.add(quantityLabel, BorderLayout.EAST);
        }
        add(bottomText, BorderLayout.SOUTH);
    }


    void setTooltipText() {
		if (config.priceType().getValue() == 0)
		{
			setToolTipText("<html>" + item.getName() +
				(config.showQuantity() ? "<br>" + item.getQuantityLabelText() : "") +
				(config.showRarity() ? "<br><font color=\"" + rarityColorStr + "\">" + (item.getRarity() < 0 ? item.getRarityStr() : item.getRarityLabelText(percentBtn.isSelected())) + "</font>" : "") +
				(config.priceType().getValue() == 0 ? "<br><font color=\"" + priceColorStr + "\">" + item.getExchangePriceLabelText() + "</font>" : "") + "</html>");
		}
		else if (config.priceType().getValue() == 1)
		{
			setToolTipText("<html>" + item.getName() +
				(config.showQuantity() ? "<br>" + item.getQuantityLabelText() : "") +
				(config.showRarity() ? "<br><font color=\"" + rarityColorStr + "\">" + (item.getRarity() < 0 ? item.getRarityStr() : item.getRarityLabelText(percentBtn.isSelected())) + "</font>" : "") +
				(config.priceType().getValue() == 1 ? "<br><font color=\"" + priceColorStr + "\">" + item.getAlchemyPriceLabelText() + "</font>" : "") + "</html>");
		}
		else
		{
			setToolTipText("<html>" + item.getName() +
				(config.showQuantity() ? "<br>" + item.getQuantityLabelText() : "") +
				(config.showRarity() ? "<br><font color=\"" + rarityColorStr + "\">" + (item.getRarity() < 0 ? item.getRarityStr() : item.getRarityLabelText(percentBtn.isSelected())) + "</font>" : ""));
		}
    }
}
