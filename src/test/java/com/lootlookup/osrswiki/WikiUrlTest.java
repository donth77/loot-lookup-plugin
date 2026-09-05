package com.lootlookup.osrswiki;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

/**
 * The wiki URLs are handed to RuneLite's LinkBrowser.browse, which validates them with
 * URI.create and throws IllegalArgumentException on anything malformed. These tests pin
 * down that ordinary names are emitted unchanged and that characters which are illegal
 * in a URI (quotes, angle brackets, percent signs, ...) are percent-encoded rather than
 * passed through.
 */
public class WikiUrlTest {

    /** Every distinct drop-table section header observed across the parser-compare monster set. */
    private static final String[] KNOWN_SECTION_HEADERS = {
            "\"Full\" Gorak drop table",
            "Armed hobgoblin drops",
            "Drop table 1",
            "Drop table 2",
            "Drops",
            "Drops (Ancient Prison)",
            "Drops (Armed)",
            "Drops (level 13)",
            "Drops (level 18)",
            "Drops (level 24)",
            "Drops (MVP/Solo)",
            "Drops (non-MVP)",
            "Drops (Plain)",
            "Drops (Regular)",
            "Drops (Stronghold of Security)",
            "Drops (Unarmed)",
            "Free-to-play worlds drops",
            "Gorak gem drop table",
            "Level 172, 178, and 184 drops",
            "Level 26 drops",
            "Level 3 and 6 drops",
            "Level 79 drops",
            "Level 82, 87, and 94 drops",
            "Level 92, 100, 101, and 113 drops",
            "Loot",
            "Members' worlds drops",
            "Pickpocketing",
            "Rewards",
            "Standard loot",
            "Unarmed hobgoblin drops",
            "Warriors' Guild Basement",
            "Warriors' Guild Top Floor",
            "Wilderness Slayer Cave drops",
    };

    @Test
    public void ordinaryUrlsAreEmittedUnchanged() {
        assertEquals("https://oldschool.runescape.wiki/w/Zulrah's_scales",
                WikiScraper.getWikiUrl("Zulrah's scales"));
        assertEquals("https://oldschool.runescape.wiki/w/Clue_scroll_(elite)",
                WikiScraper.getWikiUrl("Clue scroll (elite)"));
        assertEquals("https://oldschool.runescape.wiki/w/Special:Lookup?type=npc&id=2042&name=Zulrah",
                WikiScraper.getWikiUrlWithId("Zulrah", 2042));
        assertEquals("https://oldschool.runescape.wiki/w/Special:Lookup?type=npc&id=2042&name=Zulrah",
                WikiScraper.getWikiUrlForDrops("Zulrah", "Drops", 2042));
        assertEquals("https://oldschool.runescape.wiki/w/Special:Lookup?type=npc&id=-1&name=Zulrah#Drops",
                WikiScraper.getWikiUrlForDrops("Zulrah", "Drops", -1));
        assertEquals("https://oldschool.runescape.wiki/w/Special:Lookup?type=npc&id=-1&name=Zulrah#Drops",
                WikiScraper.getWikiUrlForDrops("Zulrah", null, -1));
        assertEquals("https://oldschool.runescape.wiki/w/Special:Lookup?type=npc&id=-1&name=Warriors'_guild_top_floor#Warriors'_Guild_Top_Floor",
                WikiScraper.getWikiUrlForDrops("Warriors' Guild Top Floor", "Warriors' Guild Top Floor", -1));
    }

    @Test
    public void quotedSectionHeaderIsPercentEncoded() {
        String url = WikiScraper.getWikiUrlForDrops("Gorak", "\"Full\" Gorak drop table", -1);
        assertEquals("https://oldschool.runescape.wiki/w/Special:Lookup?type=npc&id=-1&name=Gorak#%22Full%22_Gorak_drop_table", url);
        assertEquals("\"Full\"_Gorak_drop_table", URI.create(url).getFragment());
    }

    @Test
    public void everyKnownSectionHeaderYieldsAValidUri() {
        for (String header : KNOWN_SECTION_HEADERS) {
            String url = WikiScraper.getWikiUrlForDrops("Gorak", header, -1);
            URI uri = URI.create(url); // must not throw
            assertEquals(header, header.replaceAll("\\s+", "_"), uri.getFragment());
        }
    }

    @Test
    public void typedSearchTextWithIllegalCharactersYieldsAValidUri() {
        String[] typed = {"zulrah\"", "<zulrah>", "100% zulrah", "zul{rah}", "zul|rah", "zul#rah", "zul\\rah", "zul^rah", "zul`rah"};
        for (String name : typed) {
            String url = WikiScraper.getWikiUrlForDrops(name, "Drops", -1);
            URI uri = URI.create(url); // must not throw
            assertEquals(name, "Drops", uri.getFragment());
            assertEquals(name, "/w/Special:Lookup", uri.getPath());
        }
        URI.create(WikiScraper.getWikiUrl("weird \"item\" <name>"));
    }
}
