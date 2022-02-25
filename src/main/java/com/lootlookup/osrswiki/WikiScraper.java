package com.lootlookup.osrswiki;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import com.lootlookup.utils.Constants;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WikiScraper {
    private final static String baseUrl = "https://oldschool.runescape.wiki";
    private final static String baseWikiUrl = baseUrl + "/w/";

    public static OkHttpClient client = RuneLiteAPI.CLIENT;
    private static Document doc;

    public static CompletableFuture<DropTableSection[]> getDropsByMonsterName(String monsterName) {
        CompletableFuture<DropTableSection[]> future = new CompletableFuture<>();

        String url = getWikiUrl(monsterName);

        requestAsync(url).whenCompleteAsync((responseHTML, ex) -> {
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
            int tableIndex = 0;

            for (Element tableHeader : tableHeaders) {
                Boolean isDropsTableHeader = tableHeader.text().toLowerCase().contains("drops");

                Elements parentH2 = tableHeader.parent().select("h2");
                Boolean isParentH2 = !parentH2.isEmpty();

                Elements parentH3 = tableHeader.parent().select("h3");
                Boolean isParentH3 = !parentH3.isEmpty();

                if(isParentH2) {
                    if (!currDropTable.isEmpty()) {
                        // reset section
                        currDropTableSection.setTable(currDropTable);
                        dropTableSections.add(currDropTableSection);

                        currDropTable = new LinkedHashMap<>();
                        currDropTableSection = new DropTableSection();
                    }

                    if(isDropsTableHeader) {
                        // new section
                        parseDropTableSection = true;
                        currDropTableSection.setHeader(tableHeader.text());
                    } else {
                        parseDropTableSection = false;
                    }
                } else if(parseDropTableSection && isParentH3) {
                        // parse table
                        WikiItem[] tableRows = getTableItems(tableIndex, "h3 ~ table.item-drops");

                        if (tableRows.length > 0 && !currDropTable.containsKey(tableHeader.text())) {
                            currDropTable.put(tableHeader.text(), tableRows);
                            tableIndex++;
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
                String[] lootRow = new String[5];
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

                    if (cellContent != null && !cellContent.isEmpty() && index < 5) {
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
        int price = -1;

        if (row.length > 4) {
            imageUrl = row[0];
            name = row[1];
            if (name.endsWith("(m)")) {
                // (m) indicates members only, remove because it's not part of actual item name
                name = name.substring(0, name.length() - 3);
            }

            NumberFormat nf = NumberFormat.getNumberInstance();

            quantityStr = row[2];
            quantityStr = quantityStr.replaceAll("–", "-").replaceAll("\\(.*\\)", "").trim();
            try {
                String[] quantityStrs = quantityStr.replaceAll("\\s+", "").split("-");
                String firstQuantityStr = quantityStrs.length > 0 ? quantityStrs[0] : null;
                quantity = nf.parse(firstQuantityStr).intValue();
            } catch (ParseException e) {
            }

            rarityStr = row[3];
            if(rarityStr.startsWith("2 × ") || rarityStr.startsWith("3 × ")) {
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
                price = nf.parse(row[4]).intValue();
            } catch (ParseException ex) {
            }
        }
        return new WikiItem(imageUrl, name, quantity, quantityStr, rarityStr, rarity, price);
    }


    public static String filterTableContent(String cellContent) {
        return cellContent.replaceAll("\\[.*\\]", "");
    }

    public static String getWikiUrl(String itemOrMonsterName) {
        String sanitizedName = sanitizeName(itemOrMonsterName);
        return baseWikiUrl + sanitizedName;
    }

    public static String getWikiUrlForDrops(String monsterName, String anchorText) {
        String sanitizedMonsterName = sanitizeName(monsterName);
        String anchorStr = "Drops";
        if (anchorText != null) {
            anchorStr = anchorText.replaceAll("\\s+", "_");
        }
        return baseWikiUrl + sanitizedMonsterName + "#" + anchorStr;
    }

    public static String sanitizeName(String name) {
        name = name.trim().toLowerCase().replaceAll("\\s+", "_");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private static CompletableFuture<String> requestAsync(String url) {
        CompletableFuture<String> future = new CompletableFuture<>();

        Request request = new Request.Builder().url(url).header("User-Agent", Constants.USER_AGENT).build();

        client
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