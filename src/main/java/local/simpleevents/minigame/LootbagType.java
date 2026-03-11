package local.simpleevents.minigame;

import org.bukkit.Material;

/**
 * Lootbag tiers purchasable from the Minigame Token Shop.
 *
 * Each bag advertises a set of possible contents (display only) and costs a
 * certain number of Minigame Tokens.
 */
public enum LootbagType {

    TIER_1(
            "lootbag_i",
            "§e§oMinigames Lootbag §r§e<I>",
            Material.CHEST,
            2,
            4,
            new String[]{
                    "§7You've dominated your enemies",
                    "§7and shown the Cosmoverse that you",
                    "§7are a force to be reckoned with!",
                    "",
                    "§fContains §a2 §fof the following:",
                    "§8» §7Cosmic Crates",
                    "§8» §7Exclusive Disguises",
                    "§8» §7Admin Weapons",
                    "§8» §7Armor Crystals",
                    "§8» §7Dungeon Items",
                    "§8» §7...and more!"
            }
    ),

    TIER_2(
            "lootbag_ii",
            "§e§oMinigames Lootbag §r§e<II>",
            Material.CHEST,
            4,
            7,
            new String[]{
                    "§7You've dominated your enemies",
                    "§7and shown the Cosmoverse that you",
                    "§7are a force to be reckoned with!",
                    "",
                    "§fContains §a4 §fof the following:",
                    "§8» §7Cosmic Crates",
                    "§8» §7Exclusive Disguises",
                    "§8» §7Admin Weapons",
                    "§8» §7Armor Crystals",
                    "§8» §7Dungeon Items",
                    "§8» §7...and more!"
            }
    ),

    TIER_3(
            "lootbag_iii",
            "§e§oMinigames Lootbag §r§e<III>",
            Material.CHEST,
            6,
            10,
            new String[]{
                    "§7You've dominated your enemies",
                    "§7and shown the Cosmoverse that you",
                    "§7are a force to be reckoned with!",
                    "",
                    "§fContains §a6 §fof the following:",
                    "§8» §7Cosmic Crates",
                    "§8» §7Exclusive Disguises",
                    "§8» §7Admin Weapons",
                    "§8» §7Armor Crystals",
                    "§8» §7Dungeon Items",
                    "§8» §7...and more!"
            }
    ),

    TIER_4(
            "lootbag_iv",
            "§5§oMinigames Lootbag §r§5<IV>",
            Material.ENDER_CHEST,
            6,
            50,
            new String[]{
                    "§7You've dominated your enemies",
                    "§7and shown the Cosmoverse that you",
                    "§7are a force to be reckoned with!",
                    "",
                    "§fContains §a6 §fof the following:",
                    "§8» §7Cosmic Crates",
                    "§8» §7Exclusive Disguises",
                    "§8» §7Admin Weapons",
                    "§8» §7Armor Crystals",
                    "§8» §7Dungeon Items",
                    "§8» §7...and more!"
            }
    );

    private final String key;
    private final String displayName;
    private final Material icon;
    private final int itemCount;
    private final int tokenCost;
    private final String[] lore;

    LootbagType(String key, String displayName, Material icon, int itemCount, int tokenCost, String[] lore) {
        this.key = key;
        this.displayName = displayName;
        this.icon = icon;
        this.itemCount = itemCount;
        this.tokenCost = tokenCost;
        this.lore = lore;
    }

    public String getKey()         { return key; }
    public String getDisplayName() { return displayName; }
    public Material getIcon()      { return icon; }
    public int getItemCount()      { return itemCount; }
    public int getTokenCost()      { return tokenCost; }
    public String[] getLore()      { return lore; }
}
