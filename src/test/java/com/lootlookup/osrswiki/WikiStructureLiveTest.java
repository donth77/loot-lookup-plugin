package com.lootlookup.osrswiki;

import com.lootlookup.utils.Util;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Live "canary" integration test that guards against breaking changes to the OSRS
 * Wiki's HTML structure and image delivery.
 *
 * <p>The plugin scrapes drop tables out of the wiki's <em>rendered HTML</em>
 * ({@link WikiScraper}) and then downloads each item's icon ({@link Util#downloadImage}).
 * Both depend on the wiki not changing out from under the plugin:
 * <ul>
 *   <li><b>Parsing:</b> if the wiki renames the {@code table.item-drops} class,
 *   restructures the {@code mw-heading} wrappers, or moves the rarity/quantity
 *   columns, the parser silently returns empty or malformed results.</li>
 *   <li><b>Icons:</b> the wiki content-negotiates on the {@code Accept} header and
 *   will serve WebP for {@code .png} URLs — which Java's {@code ImageIO} cannot
 *   decode — so icons spin forever (see
 *   <a href="https://github.com/donth77/loot-lookup-plugin/issues/50">issue #50</a>).</li>
 * </ul>
 * This test exercises the real production scraper <em>and</em> the real production
 * image-download path against a curated spread of live wiki pages, and fails when
 * either collapses, so the daily CI run surfaces the breakage.
 *
 * <p><b>This test hits the live network and is opt-in.</b> It is skipped during a
 * normal {@code ./gradlew build}/{@code test} (via {@link Assume}) and only runs
 * when explicitly enabled:
 * <pre>
 *   ./gradlew test --tests "com.lootlookup.osrswiki.WikiStructureLiveTest" -Dlootlookup.livetest=true
 * </pre>
 * or by setting the {@code LOOTLOOKUP_LIVETEST=true} environment variable.
 *
 * <p>Thresholds are deliberately conservative lower bounds — well below the actual
 * current drop counts — so ordinary wiki content edits (a new item added, a rarity
 * tweaked) never trip the alarm, while a structural break (which drives counts to
 * ~0) always does. When the wiki legitimately reorganises a page, bump that page's
 * floor rather than deleting the check.
 */
public class WikiStructureLiveTest {

    private static final String WIKI_BASE = "https://oldschool.runescape.wiki";

    private static final int REQUEST_TIMEOUT_SEC = 30;
    private static final int MAX_ATTEMPTS = 3;       // retry only guards transient network blips
    private static final long POLITE_DELAY_MS = 250; // be a good citizen to the community wiki

    private static final int MAX_ICON_SAMPLES = 15;  // one representative icon per page, capped
    private static final int IMAGE_TIMEOUT_SEC = 20;

    /** A wiki page to check, with conservative floors for its parsed output. */
    private static final class Page {
        final String name;
        final int minSections;
        final int minItems;

        Page(String name, int minSections, int minItems) {
            this.name = name;
            this.minSections = minSections;
            this.minItems = minItems;
        }
    }

    /**
     * Curated corpus spanning the variety of page shapes the parser handles:
     * plain low-level mobs, standard/GWD/wilderness bosses, slayer monsters,
     * pickpocket ("loot") tables, and pages with baked-in parser edge cases
     * (Hespori, Cyclops, Grotesque Guardians redirect, etc.).
     *
     * <p>Note: raid reward-chest pages (Chambers of Xeric, Theatre of Blood,
     * Tombs of Amascut) are intentionally excluded — their loot is not rendered
     * as {@code table.item-drops}, so the current scraper does not parse them and
     * a canary must stay green for pages the plugin actually supports today.
     */
    private static final Page[] CORPUS = {
            // --- plain / low-level mobs (single flat drop table) ---
            new Page("Goblin", 1, 8),
            new Page("Cow", 1, 3),
            new Page("Guard", 1, 8),
            new Page("Hill Giant", 1, 10),
            new Page("Moss giant", 1, 10),

            // --- slayer monsters ---
            new Page("Abyssal demon", 1, 8),
            new Page("Gargoyle", 1, 8),
            new Page("Bloodveld", 1, 6),
            new Page("Kraken", 1, 5),

            // --- standard / GWD / high-value bosses ---
            new Page("General Graardor", 1, 12),
            new Page("K'ril Tsutsaroth", 1, 12),
            new Page("Zulrah", 1, 15),
            new Page("Vorkath", 1, 12),
            new Page("Cerberus", 1, 10),
            new Page("Vardorvis", 1, 6),

            // --- pickpocket / "loot" tables (different header keyword path) ---
            new Page("Master Farmer", 1, 5),
            new Page("H.A.M. Member", 1, 5),

            // --- wilderness / other classic bosses ---
            new Page("King Black Dragon", 1, 10),
            new Page("Callisto", 1, 10),
            new Page("Alchemical Hydra", 1, 10),

            // --- baked-in parser edge cases (must not regress) ---
            new Page("Hespori", 1, 4),
            new Page("Cyclops", 1, 3),
            new Page("Grotesque Guardians", 1, 8),
            new Page("Undead druid", 1, 4),
            new Page("Jelly", 1, 6),
    };

    @Test(timeout = 20 * 60 * 1000L)
    public void wikiStructureAndIconsStillWork() throws Exception {
        Assume.assumeTrue(
                "Live wiki-structure check is opt-in and was skipped. "
                        + "Enable with -Dlootlookup.livetest=true (or LOOTLOOKUP_LIVETEST=true).",
                liveEnabled());

        OkHttpClient client = new OkHttpClient();

        List<String> failures = new ArrayList<>();
        List<String> report = new ArrayList<>();

        int totalItems = 0;
        int rarityLike = 0;
        int badImage = 0;
        List<String> blankNamePages = new ArrayList<>();
        Set<String> iconUrls = new LinkedHashSet<>(); // one representative icon per page

        try {
            // ---- Phase 1: parse every page through the real scraper ----
            for (Page page : CORPUS) {
                DropTableSection[] sections = fetchWithRetry(client, page.name);
                int items = countItems(sections);
                int secs = sections.length;

                List<String> problems = new ArrayList<>();
                if (secs < page.minSections) {
                    problems.add("expected >= " + page.minSections + " section(s), got " + secs);
                }
                if (items < page.minItems) {
                    problems.add("expected >= " + page.minItems + " item(s), got " + items);
                }

                // Accumulate global sanity signals over every returned item.
                String firstIcon = null;
                for (DropTableSection section : sections) {
                    if (section.getTable() == null) {
                        continue;
                    }
                    for (WikiItem[] arr : section.getTable().values()) {
                        for (WikiItem item : arr) {
                            totalItems++;
                            String name = item.getName();
                            if (name == null || name.trim().isEmpty()) {
                                if (!blankNamePages.contains(page.name)) {
                                    blankNamePages.add(page.name);
                                }
                            }
                            String img = item.getImageUrl();
                            if (img == null || !img.startsWith(WIKI_BASE)) {
                                badImage++;
                            } else if (firstIcon == null) {
                                firstIcon = img;
                            }
                            if (looksLikeRarity(item.getRarityStr())) {
                                rarityLike++;
                            }
                        }
                    }
                }
                if (firstIcon != null) {
                    iconUrls.add(firstIcon);
                }

                report.add(String.format("%-4s %-22s sections=%-2d items=%-3d",
                        problems.isEmpty() ? "OK" : "FAIL", page.name, secs, items));
                if (!problems.isEmpty()) {
                    failures.add(page.name + ": " + String.join("; ", problems));
                }
            }

            // ---- Phase 2: download a sample of real icons through the plugin's own
            // image path and confirm they actually decode (guards issue #50) ----
            List<String> iconSample = new ArrayList<>(iconUrls);
            if (iconSample.size() > MAX_ICON_SAMPLES) {
                iconSample = iconSample.subList(0, MAX_ICON_SAMPLES);
            }
            int iconOk = 0;
            List<String> iconFailures = new ArrayList<>();
            for (String url : iconSample) {
                boolean decoded = decodeIcon(client, url);
                if (decoded) {
                    iconOk++;
                } else {
                    iconFailures.add(url);
                }
            }
            report.add(String.format("---- icons: %d/%d decoded ----", iconOk, iconSample.size()));

            if (iconSample.size() >= 5) {
                double okFrac = (double) iconOk / iconSample.size();
                if (okFrac < 0.8) {
                    failures.add(String.format(
                            "item icons not decoding: only %d/%d sampled icons decoded to an image "
                                    + "(>= 80%% expected). The wiki may again be serving WebP for .png icon URLs "
                                    + "(issue #50), or the download Accept/headers regressed. Failing URLs: %s",
                            iconOk, iconSample.size(), String.join(", ", iconFailures)));
                }
            }
            // If fewer than 5 icons were collected, parsing itself is broken and the
            // per-page failures above already report it — no separate icon verdict.
        } finally {
            shutdown(client);
        }

        // Corpus-wide structural health signals. These catch subtler breaks that
        // per-page counts miss (e.g. a shifted rarity column, or icon <img>
        // extraction that changed), and only fire when pervasive to avoid flakiness.
        if (totalItems == 0) {
            failures.add("no items parsed for ANY page in the corpus — the wiki HTML structure very likely changed");
        } else {
            double rarityFrac = (double) rarityLike / totalItems;
            if (rarityFrac < 0.5) {
                failures.add(String.format(
                        "rarity-column health low: only %.0f%% of %d items had a rarity-like value "
                                + "(>= 50%% expected) — the rarity column may have moved",
                        rarityFrac * 100, totalItems));
            }
            double badImgFrac = (double) badImage / totalItems;
            if (badImgFrac > 0.10) {
                failures.add(String.format(
                        "icon URL health low: %.0f%% of %d items had an unexpected image URL prefix "
                                + "— the item icon <img> extraction may have changed",
                        badImgFrac * 100, totalItems));
            }
            if (!blankNamePages.isEmpty()) {
                failures.add("blank item name(s) parsed on: " + String.join(", ", blankNamePages));
            }
        }

        StringBuilder out = new StringBuilder();
        out.append("\n=== OSRS wiki structure check: ")
                .append(CORPUS.length).append(" pages, ")
                .append(totalItems).append(" items parsed ===\n");
        for (String line : report) {
            out.append(line).append('\n');
        }
        System.out.println(out);

        if (!failures.isEmpty()) {
            StringBuilder msg = new StringBuilder();
            msg.append(failures.size())
                    .append(" wiki-dependency problem(s) detected — the plugin is likely broken by a change "
                            + "to the OSRS Wiki (page HTML and/or icon delivery):\n");
            for (String failure : failures) {
                msg.append("  - ").append(failure).append('\n');
            }
            msg.append("\nFull per-page results:").append(out);
            Assert.fail(msg.toString());
        }
    }

    /**
     * Fetch through the real production scraper. Retries only on a fully empty
     * result (the shape a transient network blip produces) so a genuine structural
     * break — which yields empty/partial output every time — still fails fast.
     */
    private DropTableSection[] fetchWithRetry(OkHttpClient client, String name) throws Exception {
        DropTableSection[] best = new DropTableSection[0];
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            DropTableSection[] result;
            try {
                result = WikiScraper.getDropsByMonster(client, name, -1)
                        .get(REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.out.printf("  [%s] attempt %d/%d errored: %s%n",
                        name, attempt, MAX_ATTEMPTS, e.getMessage());
                Thread.sleep(POLITE_DELAY_MS * attempt);
                continue;
            }
            if (countItems(result) > countItems(best)) {
                best = result;
            }
            if (countItems(result) > 0) {
                Thread.sleep(POLITE_DELAY_MS);
                return best;
            }
            Thread.sleep(POLITE_DELAY_MS * attempt); // back off before retrying an empty result
        }
        return best;
    }

    /**
     * Download an icon through the plugin's real {@link Util#downloadImage} path
     * (same Accept/Referer/User-Agent headers) and confirm the bytes decode to a
     * non-null image. {@code downloadImage} only invokes its callback on a
     * successful decode, so a decode failure (e.g. an unexpected WebP response)
     * surfaces here as a timeout — exactly the issue #50 symptom.
     */
    private boolean decodeIcon(OkHttpClient client, String url) throws InterruptedException {
        for (int attempt = 1; attempt <= 2; attempt++) {
            CompletableFuture<BufferedImage> future = new CompletableFuture<>();
            Util.downloadImage(client, url, future::complete);
            try {
                BufferedImage img = future.get(IMAGE_TIMEOUT_SEC, TimeUnit.SECONDS);
                if (img != null && img.getWidth() > 0 && img.getHeight() > 0) {
                    Thread.sleep(POLITE_DELAY_MS);
                    return true;
                }
            } catch (Exception e) {
                // Timeout (callback never fired => decode failed) or other error.
                System.out.printf("  [icon] attempt %d/2 failed for %s: %s%n",
                        attempt, url, e.getClass().getSimpleName());
            }
            Thread.sleep(POLITE_DELAY_MS * attempt);
        }
        return false;
    }

    private static int countItems(DropTableSection[] sections) {
        int n = 0;
        for (DropTableSection section : sections) {
            if (section.getTable() == null) {
                continue;
            }
            for (WikiItem[] arr : section.getTable().values()) {
                n += arr.length;
            }
        }
        return n;
    }

    /** Whether a rarity cell's text looks like a real rarity value (fraction, "Always", a word, %, ...). */
    private static boolean looksLikeRarity(String rarityStr) {
        if (rarityStr == null) {
            return false;
        }
        String s = rarityStr.trim();
        if (s.isEmpty()) {
            return false;
        }
        if (s.contains("/") || s.contains("×") || s.endsWith("%")) {
            return true;
        }
        String low = s.toLowerCase();
        return low.equals("always")
                || low.contains("common")   // covers Common / Uncommon
                || low.contains("rare")      // covers Rare / Very rare
                || low.equals("varies")
                || low.equals("random")
                || low.contains("chance");
    }

    private static boolean liveEnabled() {
        String flag = System.getProperty("lootlookup.livetest");
        if (flag == null) {
            flag = System.getenv("LOOTLOOKUP_LIVETEST");
        }
        return flag != null
                && (flag.equalsIgnoreCase("true") || flag.equals("1") || flag.equalsIgnoreCase("yes"));
    }

    private static void shutdown(OkHttpClient client) {
        try {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
            client.dispatcher().executorService().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
