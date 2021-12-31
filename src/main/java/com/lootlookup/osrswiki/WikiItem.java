package com.lootlookup.osrswiki;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class WikiItem {

    private String imageUrl;
    private String name;
    private int quantity;
    private String rarityStr;
    private double rarity;
    private int price;

    public WikiItem(String imageUrl, String name, int quantity, String rarityStr, double rarity, int price) {
        this.imageUrl = imageUrl;
        this.name = name;
        this.quantity = quantity;
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


    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
