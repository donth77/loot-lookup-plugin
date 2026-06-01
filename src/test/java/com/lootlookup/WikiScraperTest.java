package com.lootlookup;

import com.lootlookup.osrswiki.DropTableSection;
import com.lootlookup.osrswiki.WikiItem;
import com.lootlookup.osrswiki.WikiScraper;
import okhttp3.OkHttpClient;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Standalone entry point for exercising {@link WikiScraper} against real wiki
 * pages without booting the RuneLite client. Run this class as a regular Java
 * application to get a quick printout of the parsed drop table sections for a
 * handful of monsters. Useful for verifying scraper changes before doing a
 * full in-game integration test.
 *
 * <p>Pass names as program arguments to override the defaults. Append
 * {@code #<npcId>} to a name (e.g. {@code "Eldric the Ice King#14149"}) to
 * exercise the {@code Special:Lookup} path used in-game, which resolves the
 * correct wiki page even when the page title's casing differs from the NPC
 * name.
 */
public class WikiScraperTest {

    private static final String[] TARGETS = {
            "Master Farmer",
            "H.A.M. Member",
            "Elf (Thieving)",
            "Vyrewatch Sentinel",
            "General Graardor",
    };

    public static void main(String[] args) throws Exception {
        OkHttpClient client = new OkHttpClient();
        try {
            String[] targets = args.length > 0 ? args : TARGETS;
            for (String name : targets) {
                printDropsFor(client, name);
            }
        } finally {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
            client.dispatcher().executorService().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static void printDropsFor(OkHttpClient client, String name) throws Exception {
        System.out.println();
        System.out.println("==== " + name + " ====");

        int id = -1;
        int hashIdx = name.indexOf('#');
        if (hashIdx > -1) {
            id = Integer.parseInt(name.substring(hashIdx + 1));
            name = name.substring(0, hashIdx);
        }

        DropTableSection[] sections = WikiScraper
                .getDropsByMonster(client, name, id)
                .get(30, TimeUnit.SECONDS);

        if (sections.length == 0) {
            System.out.println("  (no sections returned)");
            return;
        }

        for (DropTableSection section : sections) {
            System.out.println();
            System.out.println("  Section: " + section.getHeader());
            Map<String, WikiItem[]> table = section.getTable();
            if (table == null || table.isEmpty()) {
                System.out.println("    (empty)");
                continue;
            }
            for (Map.Entry<String, WikiItem[]> entry : table.entrySet()) {
                WikiItem[] items = entry.getValue();
                System.out.println("    [" + entry.getKey() + "] " + items.length + " items");
                for (WikiItem item : items) {
                    System.out.println("      - " + item.getName()
                            + " | qty=" + item.getQuantityLabelText()
                            + " | rarity=" + item.getRarityStr());
                }
            }
        }
    }
}
