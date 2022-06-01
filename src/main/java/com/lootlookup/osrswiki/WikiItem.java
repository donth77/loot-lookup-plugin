package com.lootlookup.osrswiki;

import com.lootlookup.utils.Util;

import java.text.NumberFormat;


public class WikiItem {

    private String imageUrl;
    private String name;
    private int quantity;
    private String quantityStr;
    private String rarityStr;
    private double rarity;
    private int exchangePrice;
    private int alchemyPrice;

    NumberFormat nf = NumberFormat.getNumberInstance();

    public WikiItem(String imageUrl, String name, int quantity, String quantityStr, String rarityStr, double rarity, int exchangePrice, int alchemyPrice) {
        this.imageUrl = imageUrl;
        this.name = name;
        this.quantity = quantity;
        this.quantityStr = quantityStr;
        this.rarityStr = rarityStr;
        this.rarity = rarity;
        this.exchangePrice = exchangePrice;
        this.alchemyPrice = alchemyPrice;
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
    public int getExchangePrice() {
        return exchangePrice;
    }
	public int getAlchemyPrice() {
		return alchemyPrice;
	}

    public String getImageUrl() {
        return imageUrl;
    }

    public String getQuantityLabelText() {
        if (quantityStr.contains("-") || quantityStr.endsWith(" (noted)")) {
            return "x" + quantityStr;
        }
        return quantity > 0 ? "x" + nf.format(quantity) : quantityStr;
    }

    public String getQuantityLabelTextShort() {
        if (quantityStr.endsWith(" (noted)")) {
            return "x" + quantityStr.replaceAll("\\(.*\\)", "").trim();
        }
        return getQuantityValueText();
    }

    public String getQuantityValueText() {
        return quantity > 0 ? "x" + Util.rsFormat(quantity) : "";
    }

    public String getRarityLabelText(boolean percentMode) {
        String rarityLabelStr = rarityStr.contains(";") || rarityStr.equals("Always") || rarityStr.contains(" × ") ? rarityStr : Util.convertDecimalToFraction(rarity);
        if (percentMode) {
            rarityLabelStr = Util.toPercentage(rarity, rarity <= 0.0001 ? 3 : 2);
        }
        return rarityLabelStr;
    }

	public String getExchangePriceLabelText() {
		String priceLabelStr = exchangePrice > 0 ? nf.format(exchangePrice) + "gp" : "Not sold";
		if (name.equals("Nothing")) {
			priceLabelStr = "";
		}
		return priceLabelStr;
	}

	public String getExchangePriceLabelTextShort() {
		String priceLabelStr = exchangePrice > 0 ? Util.rsFormat(exchangePrice) : "";
		if (name.equals("Nothing")) {
			priceLabelStr = "";
		}
		return priceLabelStr;
	}

	public String getAlchemyPriceLabelText() {
		String priceLabelStr = nf.format(alchemyPrice) + "gp";
		if (name.equals("Nothing")) {
			priceLabelStr = "";
		}
		return priceLabelStr;
	}

	public String getAlchemyPriceLabelTextShort() {
		String priceLabelStr = alchemyPrice > 0 ? nf.format(alchemyPrice) + "gp" : "";
		if (name.equals("Nothing")) {
			priceLabelStr = "";
		}
		return priceLabelStr;
	}
}
