package local.simpleevents.minigame.bedwars;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Bed Wars item shop with five tab categories:
 *   Blocks | Gear | Tools | Misc | Potions
 *
 * Currency: Iron Ingots (§f), Gold Ingots (§6), Diamonds (§b), Emeralds (§a)
 */
public class BedWarsShopGui {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    public static final String TITLE_BLOCKS  = "§8BW Shop §7» §fBlocks";
    public static final String TITLE_GEAR    = "§8BW Shop §7» §aGear";
    public static final String TITLE_TOOLS   = "§8BW Shop §7» §6Tools";
    public static final String TITLE_MISC    = "§8BW Shop §7» §cMisc";
    public static final String TITLE_POTIONS = "§8BW Shop §7» §dPotions";

    // Tab slots (bottom row)
    static final int TAB_BLOCKS  = 45;
    static final int TAB_GEAR    = 46;
    static final int TAB_TOOLS   = 47;
    static final int TAB_MISC    = 48;
    static final int TAB_POTIONS = 49;

    // ── Open tab menus ────────────────────────────────────────────────────────

    public static void openBlocks(Player player, BedWarsGame game) {
        Inventory inv = base(player, TITLE_BLOCKS);
        addTabs(inv, 0);

        // slot → {material, qty, currency, cost}
        // Row 1
        slot(inv, 10, Material.WHITE_WOOL,      8, Material.IRON_INGOT,    4, "§fWool (8x)");
        slot(inv, 11, Material.OAK_PLANKS,      6, Material.GOLD_INGOT,    4, "§fWood Planks (6x)");
        slot(inv, 12, Material.END_STONE,        6, Material.GOLD_INGOT,    8, "§fEnd Stone (6x)");
        slot(inv, 13, Material.GLASS,           16, Material.IRON_INGOT,   32, "§fGlass (16x)");
        slot(inv, 14, Material.OBSIDIAN,         4, Material.EMERALD,       8, "§fObsidian (4x)");

        player.openInventory(inv);
    }

    public static void openGear(Player player, BedWarsGame game) {
        Inventory inv = base(player, TITLE_GEAR);
        addTabs(inv, 1);

        BedWarsIsland island = game.getIslandFor(player);
        int protBonus = island != null ? island.getProtectionLevel() : 0;

        // Armour sets – sold as full set per tier
        slot(inv, 10, Material.CHAINMAIL_CHESTPLATE, 1, Material.IRON_INGOT,  24, "§7Chainmail Armour",
                "§7Grants full chainmail armour set.", "§7Cost: §f24 Iron");
        slot(inv, 11, Material.IRON_CHESTPLATE,      1, Material.GOLD_INGOT,  12, "§6Iron Armour",
                "§7Grants full iron armour set.", "§7Cost: §612 Gold");
        slot(inv, 12, Material.DIAMOND_CHESTPLATE,   1, Material.EMERALD,      6, "§bDiamond Armour",
                "§7Grants full diamond armour set.", "§7Cost: §b6 Emeralds");

        // Bow & arrows
        slot(inv, 19, Material.BOW,    1, Material.GOLD_INGOT,  12, "§fBow",
                "§7A standard bow.", "§7Cost: §612 Gold");
        slot(inv, 20, Material.ARROW,  8, Material.GOLD_INGOT,   2, "§fArrows (8x)",
                "§7Cost: §62 Gold");

        player.openInventory(inv);
    }

    public static void openTools(Player player, BedWarsGame game) {
        Inventory inv = base(player, TITLE_TOOLS);
        addTabs(inv, 2);

        BedWarsIsland island = game.getIslandFor(player);
        int effBonus = island != null ? island.getEfficiencyLevel() : 0;

        // Stone tools (upgrades from wood)
        slot(inv, 10, Material.STONE_SWORD,   1, Material.IRON_INGOT,  10, "§7Stone Sword",
                "§7Persists until death.", "§7Cost: §f10 Iron");
        slot(inv, 11, Material.STONE_PICKAXE, 1, Material.IRON_INGOT,  10, "§7Stone Pickaxe",
                "§7Persists until downgraded on death.", "§7Cost: §f10 Iron");
        slot(inv, 12, Material.STONE_AXE,     1, Material.IRON_INGOT,  10, "§7Stone Axe",
                "§7Persists until downgraded on death.", "§7Cost: §f10 Iron");
        slot(inv, 13, Material.SHEARS,        1, Material.IRON_INGOT,  40, "§fShears",
                "§7Fast wool breaking.", "§7Cost: §f40 Iron");

        // Iron tools
        slot(inv, 19, Material.IRON_SWORD,    1, Material.GOLD_INGOT,   7, "§6Iron Sword",
                "§7Persists until death.", "§7Cost: §67 Gold");
        slot(inv, 20, Material.IRON_PICKAXE,  1, Material.GOLD_INGOT,   5, "§6Iron Pickaxe",
                "§7Cost: §65 Gold");
        slot(inv, 21, Material.IRON_AXE,      1, Material.GOLD_INGOT,   5, "§6Iron Axe",
                "§7Cost: §65 Gold");

        // Diamond tools
        slot(inv, 28, Material.DIAMOND_SWORD,    1, Material.EMERALD,    3, "§bDiamond Sword",
                "§7Persists until death.", "§7Cost: §b3 Emeralds");
        slot(inv, 29, Material.DIAMOND_PICKAXE,  1, Material.EMERALD,    3, "§bDiamond Pickaxe",
                "§7Cost: §b3 Emeralds");
        slot(inv, 30, Material.DIAMOND_AXE,      1, Material.EMERALD,    3, "§bDiamond Axe",
                "§7Cost: §b3 Emeralds");

        player.openInventory(inv);
    }

    public static void openMisc(Player player, BedWarsGame game) {
        Inventory inv = base(player, TITLE_MISC);
        addTabs(inv, 3);

        slot(inv, 10, Material.TNT,             1, Material.GOLD_INGOT,  8,  "§cTNT",
                "§7Auto-ignites on placement.", "§7Cost: §68 Gold");
        slot(inv, 11, Material.ENDER_PEARL,     1, Material.EMERALD,     4,  "§dEnder Pearl",
                "§7Cost: §a4 Emeralds");
        slot(inv, 12, Material.GOLDEN_APPLE,    1, Material.GOLD_INGOT,  5,  "§6Golden Apple",
                "§7Cost: §65 Gold");
        slot(inv, 13, Material.FIRE_CHARGE,     1, Material.GOLD_INGOT,  40, "§cFireball",
                "§7Launch a fireball.", "§7Cost: §640 Gold");
        slot(inv, 14, Material.WATER_BUCKET,    1, Material.GOLD_INGOT,  6,  "§9Water Bucket",
                "§7Cost: §66 Gold");

        player.openInventory(inv);
    }

    public static void openPotions(Player player, BedWarsGame game) {
        Inventory inv = base(player, TITLE_POTIONS);
        addTabs(inv, 4);

        slot(inv, 10, Material.POTION, 1, Material.GOLD_INGOT,  4,  "§aPotion of Healing",
                "§7Instant health II.", "§7Cost: §64 Gold");
        slot(inv, 11, Material.POTION, 1, Material.GOLD_INGOT,  4,  "§dPotion of Speed",
                "§7Speed II, 45s.", "§7Cost: §64 Gold");
        slot(inv, 12, Material.POTION, 1, Material.GOLD_INGOT,  4,  "§aPotion of Regeneration",
                "§7Regen II, 5s.", "§7Cost: §64 Gold");
        slot(inv, 13, Material.POTION, 1, Material.GOLD_INGOT,  8,  "§7Potion of Invisibility",
                "§7Invisibility 30s.", "§7Cost: §68 Gold");
        slot(inv, 14, Material.POTION, 1, Material.GOLD_INGOT,  4,  "§ePotion of Jump Boost",
                "§7Jump Boost II, 45s.", "§7Cost: §64 Gold");

        player.openInventory(inv);
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private static Inventory base(Player player, String title) {
        Inventory inv = Bukkit.createInventory(null, 54, LEGACY.deserialize(title));
        ItemStack border = glass();
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        return inv;
    }

    private static void addTabs(Inventory inv, int active) {
        inv.setItem(TAB_BLOCKS,  tabItem(Material.BRICKS,              "§fBlocks",  active == 0));
        inv.setItem(TAB_GEAR,    tabItem(Material.CHAINMAIL_CHESTPLATE,"§aGear",    active == 1));
        inv.setItem(TAB_TOOLS,   tabItem(Material.IRON_PICKAXE,        "§6Tools",   active == 2));
        inv.setItem(TAB_MISC,    tabItem(Material.TNT,                 "§cMisc",    active == 3));
        inv.setItem(TAB_POTIONS, tabItem(Material.POTION,              "§dPotions", active == 4));
    }

    private static ItemStack tabItem(Material mat, String name, boolean active) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize((active ? "§e§l" : "§7") + name));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack glass() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    // ── Slot builder ──────────────────────────────────────────────────────────

    /** Build a shop item icon with name, currency, and cost in its lore. */
    private static void slot(Inventory inv, int slot, Material mat, int qty,
                              Material currency, int cost, String displayName, String... extraLore) {
        ItemStack item = new ItemStack(mat, qty);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(displayName));

        String currencyName = switch (currency) {
            case IRON_INGOT -> "§fIron";
            case GOLD_INGOT -> "§6Gold";
            case DIAMOND    -> "§bDiamond";
            case EMERALD    -> "§aEmerald";
            default         -> currency.name();
        };

        List<Component> lore = new java.util.ArrayList<>();
        for (String l : extraLore) lore.add(LEGACY.deserialize(l));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§eCost: " + cost + " " + currencyName));
        lore.add(LEGACY.deserialize("§7Click to buy"));
        meta.lore(lore);

        // Store purchase metadata in item name tag via PDC alternative: we encode
        // the purchase code in a custom name tag key pattern so the listener can read it.
        // Pattern: §r§0<mat>§1<qty>§2<currency>§3<cost>  (hidden via black colour codes)
        // Actually we use ItemMeta display name trick — we'll use a simpler approach:
        // the listener matches by slot position relative to the open GUI title.

        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    // ── Purchase record: maps slot → ShopItem ─────────────────────────────────

    public record ShopItem(Material give, int qty, Material currency, int cost,
                           String category, String tag) {}

    /** Returns what happens when a player clicks a given slot in a shop category GUI. */
    public static ShopItem getShopItem(String guiTitle, int slot) {
        return switch (guiTitle) {
            case TITLE_BLOCKS  -> blocksItem(slot);
            case TITLE_GEAR    -> gearItem(slot);
            case TITLE_TOOLS   -> toolsItem(slot);
            case TITLE_MISC    -> miscItem(slot);
            case TITLE_POTIONS -> potionsItem(slot);
            default            -> null;
        };
    }

    private static ShopItem blocksItem(int slot) {
        return switch (slot) {
            case 10 -> new ShopItem(Material.WHITE_WOOL,     8, Material.IRON_INGOT,    4, "blocks", "wool");
            case 11 -> new ShopItem(Material.OAK_PLANKS,     6, Material.GOLD_INGOT,    4, "blocks", "planks");
            case 12 -> new ShopItem(Material.END_STONE,       6, Material.GOLD_INGOT,    8, "blocks", "endstone");
            case 13 -> new ShopItem(Material.GLASS,          16, Material.IRON_INGOT,   32, "blocks", "glass");
            case 14 -> new ShopItem(Material.OBSIDIAN,        4, Material.EMERALD,       8, "blocks", "obsidian");
            default -> null;
        };
    }

    private static ShopItem gearItem(int slot) {
        return switch (slot) {
            case 10 -> new ShopItem(Material.CHAINMAIL_CHESTPLATE, 1, Material.IRON_INGOT, 24, "gear", "chain_armour");
            case 11 -> new ShopItem(Material.IRON_CHESTPLATE,       1, Material.GOLD_INGOT, 12, "gear", "iron_armour");
            case 12 -> new ShopItem(Material.DIAMOND_CHESTPLATE,    1, Material.EMERALD,     6, "gear", "diamond_armour");
            case 19 -> new ShopItem(Material.BOW,    1, Material.GOLD_INGOT, 12, "gear", "bow");
            case 20 -> new ShopItem(Material.ARROW,  8, Material.GOLD_INGOT,  2, "gear", "arrows");
            default -> null;
        };
    }

    private static ShopItem toolsItem(int slot) {
        return switch (slot) {
            case 10 -> new ShopItem(Material.STONE_SWORD,    1, Material.IRON_INGOT, 10, "tools", "stone_sword");
            case 11 -> new ShopItem(Material.STONE_PICKAXE,  1, Material.IRON_INGOT, 10, "tools", "stone_pickaxe");
            case 12 -> new ShopItem(Material.STONE_AXE,      1, Material.IRON_INGOT, 10, "tools", "stone_axe");
            case 13 -> new ShopItem(Material.SHEARS,          1, Material.IRON_INGOT, 40, "tools", "shears");
            case 19 -> new ShopItem(Material.IRON_SWORD,     1, Material.GOLD_INGOT,  7, "tools", "iron_sword");
            case 20 -> new ShopItem(Material.IRON_PICKAXE,   1, Material.GOLD_INGOT,  5, "tools", "iron_pickaxe");
            case 21 -> new ShopItem(Material.IRON_AXE,       1, Material.GOLD_INGOT,  5, "tools", "iron_axe");
            case 28 -> new ShopItem(Material.DIAMOND_SWORD,   1, Material.EMERALD,    3, "tools", "diamond_sword");
            case 29 -> new ShopItem(Material.DIAMOND_PICKAXE, 1, Material.EMERALD,    3, "tools", "diamond_pickaxe");
            case 30 -> new ShopItem(Material.DIAMOND_AXE,     1, Material.EMERALD,    3, "tools", "diamond_axe");
            default -> null;
        };
    }

    private static ShopItem miscItem(int slot) {
        return switch (slot) {
            case 10 -> new ShopItem(Material.TNT,          1, Material.GOLD_INGOT,  8, "misc", "tnt");
            case 11 -> new ShopItem(Material.ENDER_PEARL,  1, Material.EMERALD,      4, "misc", "ender_pearl");
            case 12 -> new ShopItem(Material.GOLDEN_APPLE, 1, Material.GOLD_INGOT,  5, "misc", "golden_apple");
            case 13 -> new ShopItem(Material.FIRE_CHARGE,  1, Material.GOLD_INGOT, 40, "misc", "fireball");
            case 14 -> new ShopItem(Material.WATER_BUCKET, 1, Material.GOLD_INGOT,  6, "misc", "water_bucket");
            default -> null;
        };
    }

    private static ShopItem potionsItem(int slot) {
        return switch (slot) {
            case 10 -> new ShopItem(Material.POTION, 1, Material.GOLD_INGOT, 4, "potions", "healing");
            case 11 -> new ShopItem(Material.POTION, 1, Material.GOLD_INGOT, 4, "potions", "speed");
            case 12 -> new ShopItem(Material.POTION, 1, Material.GOLD_INGOT, 4, "potions", "regen");
            case 13 -> new ShopItem(Material.POTION, 1, Material.GOLD_INGOT, 8, "potions", "invisibility");
            case 14 -> new ShopItem(Material.POTION, 1, Material.GOLD_INGOT, 4, "potions", "jump");
            default -> null;
        };
    }
}
