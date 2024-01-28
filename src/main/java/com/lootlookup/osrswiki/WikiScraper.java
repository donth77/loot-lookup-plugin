package com.lootlookup.osrswiki;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import com.lootlookup.utils.Constants;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WikiScraper {
    private final static String baseUrl = "https://oldschool.runescape.wiki";
    private final static String baseWikiUrl = baseUrl + "/w/";
    private final static String baseWikiLookupUrl = baseWikiUrl + "Special:Lookup";

    private static Document doc;

    public static CompletableFuture<DropTableSection[]> getDropsByMonster(OkHttpClient okHttpClient, String monsterName, int monsterId) {
        CompletableFuture<DropTableSection[]> future = new CompletableFuture<>();

        String url;
        if (monsterId > -1) {
            url = getWikiUrlWithId(monsterName, monsterId);
        } else {
            url = getWikiUrl(monsterName);
        }

        requestAsync(okHttpClient, url).whenCompleteAsync((responseHTML, ex) -> {
            List<DropTableSection> dropTableSections = new ArrayList<>();

            if (ex != null) {
                DropTableSection[] result = new DropTableSection[0];
                future.complete(result);
            }

            doc = Jsoup.parse(responseHTML);
            Elements tableHeaders = doc.select("h2 span.mw-headline, h3 span.mw-headline, h4 span.mw-headline");

            Boolean parseDropTableSection = false;
            DropTableSection currDropTableSection = new DropTableSection();
            Map<String, WikiItem[]> currDropTable = new LinkedHashMap<>();
            int tableIndexH3 = 0;
            int tableIndexH4 = 0;

            boolean incrementH3Index = false;

            for (Element tableHeader : tableHeaders) {
                String tableHeaderText = tableHeader.text();
                String monsterNameLC = monsterName.toLowerCase();

                // --- Handle edge cases for specific pages ---
                if (monsterNameLC.equals("hespori") && tableHeaderText.equals("Main table")) continue;
                if (monsterNameLC.equals("chaos elemental") && tableHeaderText.equals("Major drops")) continue;
                if (monsterNameLC.equals("cyclops") && tableHeaderText.equals("Drops")) continue;
                if (monsterNameLC.equals("gorak") && tableHeaderText.equals("Drops")) continue;
                if (monsterNameLC.equals("undead druid") && tableHeaderText.equals("Seeds")) {
                    incrementH3Index = true;
                    continue;
                }
                ;
                // ---

                String tableHeaderTextLower = tableHeaderText.toLowerCase();
                Boolean isDropsTableHeader = tableHeaderTextLower.contains("drop") || tableHeaderTextLower.contains("levels") || isDropsHeaderForEdgeCases(monsterName, tableHeaderText);
                Boolean isPickpocketLootHeader = tableHeaderTextLower.contains("loot");
                Boolean parseH3Primary = isPickpocketLootHeader || parseH3PrimaryForEdgeCases(monsterName);

                Elements parentH2 = tableHeader.parent().select("h2");
                Boolean isParentH2 = !parentH2.isEmpty();

                Elements parentH3 = tableHeader.parent().select("h3");
                Boolean isParentH3 = !parentH3.isEmpty();

                Elements parentH4 = tableHeader.parent().select("h4");
                Boolean isParentH4 = !parentH4.isEmpty();


                // --- Handle edge cases for specific pages ---
                if (isParentH3 && tableHeaderText.equals("Regular drops")) {
                    incrementH3Index = true;
                    continue;
                }
                ;
                // ---

                if (isParentH2 || (parseH3Primary && isParentH3)) {
                    if (!currDropTable.isEmpty()) {
                        // reset section
                        currDropTableSection.setTable(currDropTable);
                        dropTableSections.add(currDropTableSection);

                        currDropTable = new LinkedHashMap<>();
                        currDropTableSection = new DropTableSection();
                    }

                    if (isDropsTableHeader || isPickpocketLootHeader) {
                        // new section
                        parseDropTableSection = true;
                        currDropTableSection.setHeader(tableHeaderText);
                    } else {
                        parseDropTableSection = false;
                    }
                } else if (parseDropTableSection && (isParentH3 || isParentH4)) {
                    String element = isParentH4 ? "h4" : "h3";
                    int tableIndex = isParentH4 ? tableIndexH4 : tableIndexH3;
                    // parse table
                    WikiItem[] tableRows = getTableItems(tableIndex, element + " ~ table.item-drops");

                    if (tableRows.length > 0 && !currDropTable.containsKey(tableHeaderText)) {
                        currDropTable.put(tableHeaderText, tableRows);
                        if (isParentH4) {
                            tableIndexH4++;
                            if (incrementH3Index) {
                                tableIndexH3++;
                            }
                        } else {
                            tableIndexH3++;
                        }
                    }
                }
            }

            if (!currDropTable.isEmpty()) {
                currDropTableSection.setTable(currDropTable);
                dropTableSections.add(currDropTableSection);
            }

            if (dropTableSections.isEmpty()) {
                tableHeaders = doc.select("h2 span.mw-headline");

                if (!tableHeaders.isEmpty()) {
                    WikiItem[] tableRows = getTableItems(0, "h2 ~ table.item-drops");
                    if (tableRows.length > 0) {
                        currDropTable = new LinkedHashMap<>();
                        currDropTable.put("Drops", tableRows);
                        dropTableSections.add(new DropTableSection("Drops", currDropTable));
                    }
                }
            }

            DropTableSection[] result = dropTableSections.toArray(new DropTableSection[dropTableSections.size()]);
            future.complete(result);
        });

        return future;
    }

    private static WikiItem[] getTableItems(int tableIndex, String selector) {
        List<WikiItem> wikiItems = new ArrayList<>();
        Elements dropTables = doc.select(selector);

        if (dropTables.size() > tableIndex) {
            Elements dropTableRows = dropTables.get(tableIndex).select("tbody tr");
            for (Element dropTableRow : dropTableRows) {
                String[] lootRow = new String[6];
                Elements dropTableCells = dropTableRow.select("td");
                int index = 1;

                for (Element dropTableCell : dropTableCells) {
                    String cellContent = dropTableCell.text();
                    Elements images = dropTableCell.select("img");

                    if (images.size() != 0) {
                        String imageSource = images.first().attr("src");
                        if (!imageSource.isEmpty()) {
                            lootRow[0] = baseUrl + imageSource;
                        }
                    }

                    if (cellContent != null && !cellContent.isEmpty() && index < 6) {
                        cellContent = filterTableContent(cellContent);
                        lootRow[index] = cellContent;
                        index++;
                    }
                }

                if (lootRow[0] != null) {
                    WikiItem wikiItem = parseRow(lootRow);
                    wikiItems.add(wikiItem);
                }
            }
        }


        WikiItem[] result = new WikiItem[wikiItems.size()];
        return wikiItems.toArray(result);
    }

    public static WikiItem parseRow(String[] row) {
        String imageUrl = "";
        String name = "";

        double rarity = -1;
        String rarityStr = "";

        int quantity = 0;
        String quantityStr = "";
        int exchangePrice = -1;
        int alchemyPrice = -1;

        if (row.length > 4) {
            imageUrl = row[0];
            name = row[1];
            if (name.endsWith("(m)")) {
                // (m) indicates members only, remove because it's not part of actual item name
                name = name.substring(0, name.length() - 3);
            }

            NumberFormat nf = NumberFormat.getNumberInstance();

            quantityStr = row[2];
            quantityStr = quantityStr.replaceAll("–", "-").trim();
            try {
                String[] quantityStrs = quantityStr.replaceAll("\\s+", "").split("-");
                String firstQuantityStr = quantityStrs.length > 0 ? quantityStrs[0] : null;
                quantity = nf.parse(firstQuantityStr).intValue();
            } catch (ParseException e) {
            }

            rarityStr = row[3];
            if (rarityStr.startsWith("~")) {
                rarityStr = rarityStr.substring(1);
            } else if (rarityStr.startsWith("2 × ") || rarityStr.startsWith("3 × ")) {
                rarityStr = rarityStr.substring(4);
            }

            try {
                String[] rarityStrs = rarityStr.replaceAll("\\s+", "").split(";");
                String firstRarityStr = rarityStrs.length > 0 ? rarityStrs[0] : null;

                if (firstRarityStr != null) {
                    if (firstRarityStr.equals("Always")) {
                        rarity = 1.0;
                    } else {
                        String[] fraction = firstRarityStr.split("/");
                        if (fraction.length > 1) {
                            double numer = nf.parse(fraction[0]).doubleValue();
                            double denom = nf.parse(fraction[1]).doubleValue();
                            rarity = numer / denom;
                        }

                    }
                }
            } catch (ParseException ex) {
            }


            try {
                exchangePrice = nf.parse(row[4]).intValue();
            } catch (ParseException ex) {
            }
            try {
                alchemyPrice = nf.parse(row[5]).intValue();
            } catch (ParseException ex) {
            }
        }
        return new WikiItem(imageUrl, name, quantity, quantityStr, rarityStr, rarity, exchangePrice, alchemyPrice);
    }


    public static String filterTableContent(String cellContent) {
        return cellContent.replaceAll("\\[.*\\]", "");
    }

    public static String getWikiUrl(String itemOrMonsterName) {
        String sanitizedName = sanitizeName(itemOrMonsterName);
        return baseWikiUrl + sanitizedName;
    }

    public static String getWikiUrlWithId(String monsterName, int id) {
        String sanitizedName = sanitizeName(monsterName);
        // --- Handle edge cases for specific pages ---
        if(id == 7851 || id == 7852) {
            // Redirect Dusk and Dawn to Grotesque Guardians page
            id = -1;
            sanitizedName = "Grotesque_Guardians";
        }
        // ---
        return baseWikiLookupUrl + "?type=npc&id=" + String.valueOf(id) + "&name=" + sanitizedName;
    }

    public static String getWikiUrlForDrops(String monsterName, String anchorText, int monsterId) {
        if (monsterId > -1) {
            return getWikiUrlWithId(monsterName, monsterId);
        }
        String sanitizedMonsterName = sanitizeName(monsterName);
        String anchorStr = "Drops";
        if (anchorText != null) {
            anchorStr = anchorText.replaceAll("\\s+", "_");
        }
        return baseWikiUrl + sanitizedMonsterName + "#" + anchorStr;
    }

    public static String sanitizeName(String name) {
        // --- Handle edge cases for specific pages ---
        if (name.equalsIgnoreCase("tzhaar-mej")) {
            name = "tzhaar-mej (monster)";
        }
        if(name.equalsIgnoreCase("dusk") || name.equalsIgnoreCase("dawn")) {
            name = "grotesque guardians";
        }
        // ---
        name = name.trim().toLowerCase().replaceAll("\\s+", "_");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public static Boolean isDropsHeaderForEdgeCases(String monsterName, String tableHeaderText) {
        String monsterNameLC = monsterName.toLowerCase();
        String tableHeaderTextLower = tableHeaderText.toLowerCase();
        return (monsterNameLC.equals("cyclops") && (
                tableHeaderTextLower.contains("warriors' guild") ||
                        tableHeaderText.equals("Ardougne Zoo")))
                || (monsterNameLC.equals("vampyre juvinate") &&
                tableHeaderTextLower.equals("returning a juvinate to human"));
    }

    public static Boolean parseH3PrimaryForEdgeCases(String monsterName) {
        String monsterNameLC = monsterName.toLowerCase();
        return monsterNameLC.equals("cyclops");
    }

    private static CompletableFuture<String> requestAsync(OkHttpClient okHttpClient, String url) {
        CompletableFuture<String> future = new CompletableFuture<>();

        Request request = new Request.Builder().url(url).header("User-Agent", Constants.USER_AGENT).build();

        okHttpClient
                .newCall(request)
                .enqueue(
                        new Callback() {
                            @Override
                            public void onFailure(Call call, IOException ex) {
                                future.completeExceptionally(ex);
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                try (ResponseBody responseBody = response.body()) {
                                    if (!response.isSuccessful()) future.complete("");

                                    future.complete(responseBody.string());
                                } finally {
                                    response.close();
                                }
                            }
                        });

        return future;
    }

}