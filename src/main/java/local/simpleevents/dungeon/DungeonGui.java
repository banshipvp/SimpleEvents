package local.simpleevents.dungeon;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds and opens the Simple Dungeons browser GUI.
 *
 * Layout (54 slots, 6 rows):
 *   Row 0  [0-8]:   Purple glass border + Tips book at slot 4
 *   Col 0  [9,18,27,36]: Purple glass border
 *   Col 8  [17,26,35,44]: Purple glass border
 *   Dungeon icons at slots 10, 19, 28, 37 (one per dungeon, one per row)
 *   Slot 48: Heroic dungeons info
 *   Row 5  [45-53]: Purple glass border + Heroic at 48 + General info at 50
 */
public class DungeonGui {

    /** Used by the listener to identify this GUI by title. */
    public static final String MAIN_TITLE = "§5§lSimple Dungeons";

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final DungeonManager manager;

    // Dungeon icon slots (one per tier, left column)
    static final int[] DUNGEON_SLOTS = {10, 19, 28, 37};

    public DungeonGui(DungeonManager manager) {
        this.manager = manager;
    }

    // ── Main browser ──────────────────────────────────────────────────────────

    public void openBrowser(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, LEGACY.deserialize(MAIN_TITLE));

        // Borders
        ItemStack border = buildBorder();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int r = 1; r <= 4; r++) {
            inv.setItem(r * 9, border);
            inv.setItem(r * 9 + 8, border);
        }

        // Tips book (top-center)
        inv.setItem(4, buildTipsBook());

        // Dungeon icons
        DungeonType[] types = DungeonType.values();
        for (int i = 0; i < types.length && i < DUNGEON_SLOTS.length; i++) {
            inv.setItem(DUNGEON_SLOTS[i], buildDungeonIcon(types[i]));
        }

        // General info (bottom row slot 47)
        inv.setItem(47, buildGeneralInfo());

        // Heroic info (bottom row slot 51)
        inv.setItem(51, buildHeroicInfo());

        player.openInventory(inv);
    }

    // ── Normal / Heroic loot table sub-menus ─────────────────────────────────

    public void openNormalLoot(Player player, DungeonType type) {
        String title = "§e" + stripColor(type.getDisplayName()) + " — Normal Loot";
        Inventory inv = Bukkit.createInventory(null, 27, LEGACY.deserialize(title));
        fillLootTable(inv, type, false);
        player.openInventory(inv);
    }

    public void openHeroicLoot(Player player, DungeonType type) {
        String title = "§c" + stripColor(type.getDisplayName()) + " — Heroic Loot";
        Inventory inv = Bukkit.createInventory(null, 27, LEGACY.deserialize(title));
        fillLootTable(inv, type, true);
        player.openInventory(inv);
    }

    private void fillLootTable(Inventory inv, DungeonType type, boolean heroic) {
        ItemStack border = buildBorder();
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // Lootbag showcase
        inv.setItem(11, buildLootbagShowcase(type, heroic));
        // Money note showcase
        inv.setItem(13, buildMoneyNoteShowcase(type, heroic));
        // Next-unlock reward
        if (type.getUnlockRewardDescription() != null) {
            inv.setItem(15, buildUnlockRewardShowcase(type));
        }
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private ItemStack buildDungeonIcon(DungeonType type) {
        ItemStack item = new ItemStack(type.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(type.getDisplayName()));

        List<Component> lore = new ArrayList<>();

        // Status
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§eSTATUS"));
        ActiveDungeon active = manager.getActiveDungeon(type);
        if (active == null || active.getParticipantCount() == 0) {
            lore.add(LEGACY.deserialize("§7Empty —"));
            lore.add(LEGACY.deserialize("§7no one is in the dungeon."));
        } else {
            String mode = active.isHeroic() ? " §c[HEROIC]" : "";
            lore.add(LEGACY.deserialize("§a" + active.getParticipantCount() + " player(s) inside" + mode + "§a."));
        }

        // Lore
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§eLORE"));
        for (String line : type.getLore().split("\n")) {
            lore.add(LEGACY.deserialize("§7" + line));
        }

        // Rewards
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§eREWARDS"));
        lore.add(LEGACY.deserialize("§8» §7Dungeon Lootbag"));
        lore.add(LEGACY.deserialize("§8» §7Money Note"));
        lore.add(LEGACY.deserialize("§8  §7(Normal: §a$" + fmt(type.getNormalRewardMin())
                + " §7— §a$" + fmt(type.getNormalRewardMax()) + "§7)"));
        lore.add(LEGACY.deserialize("§8  §7(Heroic: §6$" + fmt(type.getHeroicRewardMin())
                + " §7— §6$" + fmt(type.getHeroicRewardMax()) + "§7)"));
        if (type.getUnlockRewardDescription() != null) {
            lore.add(LEGACY.deserialize("§8» §7" + type.getUnlockRewardDescription()));
        }

        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§eLeft-Click §7to view normal loot table."));
        lore.add(LEGACY.deserialize("§eRight-Click §7to view heroic loot table."));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildTipsBook() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize("§6§lDungeon Tips"));
        List<Component> lore = new ArrayList<>();
        lore.add(LEGACY.deserialize("§7Simple Dungeons operate differently than"));
        lore.add(LEGACY.deserialize("§7the normal SimpleEvents gameplay."));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§8✗ §7You do §nNOT§7 lose your inventory."));
        lore.add(LEGACY.deserialize("§8✗ §7Every boss has custom mechanics."));
        lore.add(LEGACY.deserialize("§8✗ §7If you do not strategize, you die."));
        lore.add(LEGACY.deserialize("§8✗ §7Anyone can enter your dungeon portal."));
        lore.add(LEGACY.deserialize("§8✗ §7Dungeons are PvP-enabled zones."));
        lore.add(LEGACY.deserialize("§8✗ §7Simple Dungeons are end-game"));
        lore.add(LEGACY.deserialize("§8  §7PvE content — they are §c§lHARD§7."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildGeneralInfo() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize("§6§lSimple Dungeons"));
        List<Component> lore = new ArrayList<>();
        lore.add(LEGACY.deserialize("§7Dungeons are top-tier PvE instances"));
        lore.add(LEGACY.deserialize("§7for the most powerful players to conquer."));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§7Filled with §b§ladvanced custom AI"));
        lore.add(LEGACY.deserialize("§7and unique challenges, these dungeons"));
        lore.add(LEGACY.deserialize("§7will push any warrior to their limit."));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§7In order to start a dungeon you"));
        lore.add(LEGACY.deserialize("§7need to unlock its §6dungeon key§7."));
        lore.add(LEGACY.deserialize("§7Keys can be obtained from admins"));
        lore.add(LEGACY.deserialize("§7or through special dungeon events."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildHeroicInfo() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize("§c§lHeroic Dungeons"));
        List<Component> lore = new ArrayList<>();
        lore.add(LEGACY.deserialize("§7Taking dungeons to a whole new level,"));
        lore.add(LEGACY.deserialize("§7Heroic Dungeons are a higher risk,"));
        lore.add(LEGACY.deserialize("§7higher reward event."));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§7When a Heroic Dungeon begins, three"));
        lore.add(LEGACY.deserialize("§7guaranteed debuffs and two random debuffs"));
        lore.add(LEGACY.deserialize("§7will be selected to create a unique"));
        lore.add(LEGACY.deserialize("§7experience. If you have what it takes,"));
        lore.add(LEGACY.deserialize("§7all participants will receive §6OP rewards§7."));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§eGuaranteed Debuffs"));
        lore.add(LEGACY.deserialize("§8✗ §7−50% Objective Time §8(Sunken Citadel, Corrupted Keep)"));
        lore.add(LEGACY.deserialize("§8✗ §7−33% Objective Time §8(Void Sanctum, Temporal Rift)"));
        lore.add(LEGACY.deserialize("§8✗ §7Bosses take §c75% less §7incoming damage"));
        lore.add(LEGACY.deserialize("§8✗ §7Bosses deal §c2.5× §7outgoing damage"));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§eRandom Debuffs §7(2 chosen):"));
        for (DungeonDebuff debuff : DungeonDebuff.values()) {
            lore.add(LEGACY.deserialize("§8✗ §7" + debuff.getLabel()));
        }
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§7Obtain Heroic Dungeon Keys by"));
        lore.add(LEGACY.deserialize("§7applying §6Heroic Scrolls §7onto Dungeon Keys."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildLootbagShowcase(DungeonType type, boolean heroic) {
        ItemStack item = new ItemStack(Material.BUNDLE);
        ItemMeta meta = item.getItemMeta();
        String prefix = heroic ? "§c§l" : "§e§l";
        meta.displayName(LEGACY.deserialize(prefix + "Dungeon Lootbag"));
        List<Component> lore = new ArrayList<>();
        lore.add(LEGACY.deserialize("§7Awarded upon completing"));
        lore.add(LEGACY.deserialize("§7the dungeon run."));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§7Contents vary by dungeon tier."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildMoneyNoteShowcase(DungeonType type, boolean heroic) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize("§6Money Note"));
        List<Component> lore = new ArrayList<>();
        lore.add(LEGACY.deserialize("§7Reward on completion:"));
        lore.add(Component.empty());
        if (!heroic) {
            lore.add(LEGACY.deserialize("§7Normal: §a$" + fmt(type.getNormalRewardMin())
                    + " §7— §a$" + fmt(type.getNormalRewardMax())));
        }
        lore.add(LEGACY.deserialize("§7Heroic: §6$" + fmt(type.getHeroicRewardMin())
                + " §7— §6$" + fmt(type.getHeroicRewardMax())));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildUnlockRewardShowcase(DungeonType type) {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();
        DungeonType next = DungeonType.values()[type.ordinal() + 1];
        meta.displayName(LEGACY.deserialize("§b" + stripColor(next.getDisplayName()) + " Access"));
        List<Component> lore = new ArrayList<>();
        lore.add(LEGACY.deserialize("§7Completing this dungeon rewards"));
        lore.add(LEGACY.deserialize("§7access to the next tier:"));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("  §r" + next.getDisplayName()));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBorder() {
        ItemStack pane = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.empty());
        pane.setItemMeta(meta);
        return pane;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static String fmt(long value) {
        return String.format("%,d", value);
    }

    private static String stripColor(String s) {
        return s.replaceAll("§.", "");
    }
}
