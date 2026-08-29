package com.lootlookup;

import com.lootlookup.LootLookupConfig.RightClickMenuOption;
import net.runelite.api.Client;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.api.events.MenuOpened;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Drives {@link LootLookupPlugin#onMenuOpened} against stubbed RuneLite API objects
 * to verify the "Right click menu" config option gates the "Lookup Drops" entry.
 */
public class RightClickMenuOptionTest {

    private static final int NPC_INDEX = 7;
    private static final int NPC_ID = 415;
    private static final String NPC_NAME = "Abyssal demon";

    // ---------------------------------------------------------------- tests

    @Test
    public void alwaysShow_addsEntry() throws Exception {
        Fixture f = run(RightClickMenuOption.ALWAYS_SHOW, false);
        assertEquals("Lookup Drops", f.addedOption());
    }

    @Test
    public void alwaysShow_addsEntryEvenWhenShiftHeld() throws Exception {
        Fixture f = run(RightClickMenuOption.ALWAYS_SHOW, true);
        assertEquals("Lookup Drops", f.addedOption());
    }

    @Test
    public void holdShift_addsEntryWhenShiftHeld() throws Exception {
        Fixture f = run(RightClickMenuOption.HOLD_SHIFT, true);
        assertEquals("Lookup Drops", f.addedOption());
        assertTrue("should have queried the shift key", f.shiftQueried);
    }

    @Test
    public void holdShift_hidesEntryWhenShiftNotHeld() throws Exception {
        Fixture f = run(RightClickMenuOption.HOLD_SHIFT, false);
        assertEquals(0, f.created.size());
    }

    @Test
    public void disable_hidesEntry() throws Exception {
        Fixture f = run(RightClickMenuOption.DISABLE, false);
        assertEquals(0, f.created.size());
    }

    @Test
    public void disable_hidesEntryEvenWhenShiftHeld() throws Exception {
        Fixture f = run(RightClickMenuOption.DISABLE, true);
        assertEquals(0, f.created.size());
    }

    @Test
    public void excludedMonsterStillWins_whenShiftHeld() throws Exception {
        Fixture f = run(RightClickMenuOption.HOLD_SHIFT, true, NPC_NAME);
        assertEquals(0, f.created.size());
    }

    // ------------------------------------------------------------- fixture

    private Fixture run(RightClickMenuOption option, boolean shiftHeld) throws Exception {
        return run(option, shiftHeld, "");
    }

    private Fixture run(RightClickMenuOption option, boolean shiftHeld, String excluded) throws Exception {
        Fixture f = new Fixture(shiftHeld);

        LootLookupConfig config = new LootLookupConfig() {
            @Override
            public RightClickMenuOption rightClickMenuOption() {
                return option;
            }

            @Override
            public String excludedMonsters() {
                return excluded;
            }
        };

        LootLookupPlugin plugin = new LootLookupPlugin();
        set(plugin, "client", f.client);
        set(plugin, "config", config);

        MenuOpened event = new MenuOpened();
        event.setMenuEntries(new MenuEntry[]{f.attackEntry, f.examineEntry, f.cancelEntry});
        plugin.onMenuOpened(event);
        return f;
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static final class Fixture {
        final List<EntryStub> created = new ArrayList<>();
        final MenuEntry attackEntry;
        final MenuEntry examineEntry;
        final MenuEntry cancelEntry;
        final Client client;
        boolean shiftQueried;

        Fixture(boolean shiftHeld) {
            attackEntry = entry("Attack", MenuAction.NPC_FIRST_OPTION, NPC_INDEX);
            examineEntry = entry("Examine", MenuAction.EXAMINE_NPC, NPC_INDEX);
            cancelEntry = entry("Cancel", MenuAction.CANCEL, 0);

            NPC npc = stub(NPC.class, (p, m, a) -> {
                switch (m.getName()) {
                    case "getCombatLevel": return 124;
                    case "getId": return NPC_ID;
                    case "getName": return NPC_NAME;
                    default: return null;
                }
            });

            IndexedObjectSet<?> npcs = stub(IndexedObjectSet.class,
                    (p, m, a) -> "byIndex".equals(m.getName()) ? npc : null);
            WorldView wv = stub(WorldView.class,
                    (p, m, a) -> "npcs".equals(m.getName()) ? npcs : null);
            Menu menu = stub(Menu.class, (p, m, a) -> {
                if ("createMenuEntry".equals(m.getName())) {
                    EntryStub s = new EntryStub();
                    created.add(s);
                    return s.newProxy();
                }
                return null;
            });

            client = stub(Client.class, (p, m, a) -> {
                switch (m.getName()) {
                    case "getTopLevelWorldView": return wv;
                    case "getMenu": return menu;
                    case "isKeyPressed":
                        assertEquals(KeyCode.KC_SHIFT, ((Integer) a[0]).intValue());
                        shiftQueried = true;
                        return shiftHeld;
                    default: return null;
                }
            });
        }

        String addedOption() {
            assertEquals("expected exactly one created menu entry", 1, created.size());
            return created.get(0).option;
        }
    }

    private static MenuEntry entry(String option, MenuAction type, int identifier) {
        EntryStub s = new EntryStub();
        s.option = option;
        s.type = type;
        s.identifier = identifier;
        return s.newProxy();
    }

    /** Mutable stand-in for a {@link MenuEntry}; setters return the proxy for chaining. */
    private static final class EntryStub implements InvocationHandler {
        String option = "";
        String target = "";
        MenuAction type = MenuAction.CANCEL;
        int identifier;
        int param1;
        Consumer<MenuEntry> onClick;
        private MenuEntry proxy;

        MenuEntry newProxy() {
            proxy = (MenuEntry) Proxy.newProxyInstance(
                    MenuEntry.class.getClassLoader(), new Class<?>[]{MenuEntry.class}, this);
            return proxy;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object invoke(Object p, Method m, Object[] a) {
            switch (m.getName()) {
                case "equals": return p == a[0];
                case "hashCode": return System.identityHashCode(p);
                case "toString": return "MenuEntry[" + option + "]";
                case "getOption": return option;
                case "setOption": option = (String) a[0]; return p;
                case "getTarget": return target;
                case "setTarget": target = (String) a[0]; return p;
                case "getType": return type;
                case "setType": type = (MenuAction) a[0]; return p;
                case "getIdentifier": return identifier;
                case "setIdentifier": identifier = (Integer) a[0]; return p;
                case "getParam1": return param1;
                case "setParam1": param1 = (Integer) a[0]; return p;
                case "onClick":
                    if (a != null && a.length == 1) {
                        onClick = (Consumer<MenuEntry>) a[0];
                        return p;
                    }
                    return onClick;
                default: return defaultValue(m.getReturnType());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> iface, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, (p, m, a) -> {
            switch (m.getName()) {
                case "equals": return p == a[0];
                case "hashCode": return System.identityHashCode(p);
                case "toString": return iface.getSimpleName() + "Stub";
                default:
                    Object r = handler.invoke(p, m, a);
                    return r != null ? r : defaultValue(m.getReturnType());
            }
        });
    }

    private static Object defaultValue(Class<?> t) {
        if (!t.isPrimitive()) return null;
        if (t == boolean.class) return false;
        if (t == void.class) return null;
        if (t == long.class) return 0L;
        if (t == double.class) return 0d;
        if (t == float.class) return 0f;
        if (t == char.class) return (char) 0;
        if (t == byte.class) return (byte) 0;
        if (t == short.class) return (short) 0;
        return 0;
    }
}
