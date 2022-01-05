package com.lootlookup.osrswiki;

import com.lootlookup.utils.Util;

import java.text.NumberFormat;


public class WikiItem {

    private String imageUrl;
    private String name;
    private int quantity;
    private  String quantityStr;
    private String rarityStr;
    private double rarity;
    private int price;

    NumberFormat nf = NumberFormat.getNumberInstance();

    public WikiItem(String imageUrl, String name, int quantity, String quantityStr, String rarityStr, double rarity, int price) {
        this.imageUrl = imageUrl;
        this.name = name;
        this.quantity = quantity;
        this.quantityStr = quantityStr;
        this.rarityStr = rarityStr;
        this.rarity = rarity;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getQuantityStr() {
        return quantityStr;
    }

    public double getRarity() {
        return rarity;
    }

    public String getRarityStr() {
        return rarityStr;
    }

    public int getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getQuantityLabelText() {
        if (quantityStr.contains("-")) {
            return "x" + quantityStr;
        }
        return quantity > 0 ? "x" + nf.format(quantity) : quantityStr;
    }

    public String getQuantityLabelTextShort() {
        return quantity > 0 ? "x" + Util.rsFormat(quantity) : "";
    }

    public String getRarityLabelText(boolean percentMode) {
        String rarityLabelStr = rarityStr.contains(";") || rarityStr.equals("Always") || rarityStr.contains(" × ") ? rarityStr : Util.convertDecimalToFraction(rarity);
        if (percentMode) {
            rarityLabelStr = Util.toPercentage(rarity, rarity <= 0.0001 ? 3 : 2);
        }
        return rarityLabelStr;
    }

    public String getPriceLabelText() {
        String priceLabelStr = price > 0 ? nf.format(price) + "gp" : "Not sold";
        if (name.equals("Nothing")) {
            priceLabelStr = "";
        }
        return priceLabelStr;
    }

    public String getPriceLabelTextShort() {
        String priceLabelStr = price > 0 ? Util.rsFormat(price) : "";
        if (name.equals("Nothing")) {
            priceLabelStr = "";
        }
        return priceLabelStr;
    }
}
