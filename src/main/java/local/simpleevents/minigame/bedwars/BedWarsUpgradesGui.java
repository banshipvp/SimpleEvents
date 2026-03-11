package local.simpleevents.minigame.bedwars;

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
 * Team Upgrades GUI — purchased with iron / gold / diamonds / emeralds.
 *
 * Available upgrades:
 *  Sharpness I    – 8 diamonds  (all team swords)
 *  Protection I-V – 4 diamonds each level (team armour)
 *  Efficiency I   – 6 diamonds  (team tools)
 *  Haste          – 4 diamonds  (permanent Haste I on island)
 *  Regen on Kill  – 4 diamonds  (temporary Regen on kill)
 *  Island Regen   – 6 diamonds  (Regen I while inside island area)
 *  Trap Lv.1      – 5 diamonds  (Blindness 3 for 7s)
 *  Trap Lv.2      – 10 diamonds (Blindness 3 + Fatigue for 7s)
 *  Generator Lv.1 – 15 iron
 *  Generator Lv.2 – 8 gold
 *  Generator Lv.3 – 4 diamonds
 */
public class BedWarsUpgradesGui {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    public static final String TITLE = "§5Team Upgrades";

    // Slot layout
    static final int SLOT_SHARPNESS    = 10;
    static final int SLOT_PROTECTION   = 11;
    static final int SLOT_EFFICIENCY   = 12;
    static final int SLOT_HASTE        = 13;
    static final int SLOT_REGEN_KILL   = 14;
    static final int SLOT_ISLAND_REGEN = 15;
    static final int SLOT_TRAP         = 19;
    static final int SLOT_GEN_UPGRADE  = 28;

    public static void open(Player player, BedWarsGame game) {
        Inventory inv = Bukkit.createInventory(null, 54, LEGACY.deserialize(TITLE));

        ItemStack border = glass();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int r = 1; r <= 4; r++) {
            inv.setItem(r * 9, border);
            inv.setItem(r * 9 + 8, border);
        }

        BedWarsIsland island = game.getIslandFor(player);
        if (island == null) { player.closeInventory(); return; }

        inv.setItem(SLOT_SHARPNESS,    buildSharpness(island));
        inv.setItem(SLOT_PROTECTION,   buildProtection(island));
        inv.setItem(SLOT_EFFICIENCY,   buildEfficiency(island));
        inv.setItem(SLOT_HASTE,        buildHaste(island));
        inv.setItem(SLOT_REGEN_KILL,   buildRegenOnKill(island));
        inv.setItem(SLOT_ISLAND_REGEN, buildIslandRegen(island));
        inv.setItem(SLOT_TRAP,         buildTrap(island));
        inv.setItem(SLOT_GEN_UPGRADE,  buildGenUpgrade(island));

        player.openInventory(inv);
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private static ItemStack buildSharpness(BedWarsIsland island) {
        int current = island.getSharpnessLevel();
        boolean purchased = current >= 1;
        return icon(Material.DIAMOND_SWORD,
                "§bSharpness Upgrade",
                purchased ? "§a✔ Purchased" : "§7Adds Sharpness I to all team swords.",
                "§7Cost: §b8 Diamonds",
                purchased ? "§7Already purchased." : "§eClick to purchase");
    }

    private static ItemStack buildProtection(BedWarsIsland island) {
        int current = island.getProtectionLevel();
        String status = current == 0 ? "§7Not purchased"
                : "§aCurrent Level: " + current + "/5";
        String cost = current < 5 ? "§7Cost: §b" + 4 + " Diamonds (Lvl " + (current + 1) + ")" : "§aMax Level!";
        return icon(Material.IRON_CHESTPLATE,
                "§aProtection Upgrade",
                status,
                cost,
                current < 5 ? "§eClick to purchase next level" : "");
    }

    private static ItemStack buildEfficiency(BedWarsIsland island) {
        boolean purchased = island.getEfficiencyLevel() >= 1;
        return icon(Material.IRON_PICKAXE,
                "§6Efficiency Upgrade",
                purchased ? "§a✔ Purchased" : "§7Adds Efficiency I to all team tools.",
                "§7Cost: §b6 Diamonds",
                purchased ? "§7Already purchased." : "§eClick to purchase");
    }

    private static ItemStack buildHaste(BedWarsIsland island) {
        boolean purchased = island.hasHaste();
        return icon(Material.GOLDEN_PICKAXE,
                "§6Haste Upgrade",
                purchased ? "§a✔ Active" : "§7Gives Haste I permanently on your island.",
                "§7Cost: §b4 Diamonds",
                purchased ? "" : "§eClick to purchase");
    }

    private static ItemStack buildRegenOnKill(BedWarsIsland island) {
        boolean purchased = island.hasRegenOnKill();
        return icon(Material.GOLDEN_APPLE,
                "§cRegen on Kill",
                purchased ? "§a✔ Active" : "§7Temporary Regeneration on kill.",
                "§7Cost: §b4 Diamonds",
                purchased ? "" : "§eClick to purchase");
    }

    private static ItemStack buildIslandRegen(BedWarsIsland island) {
        boolean purchased = island.hasIslandRegen();
        return icon(Material.BEACON,
                "§aIsland Regeneration",
                purchased ? "§a✔ Active" : "§7Regen I while inside your island area.",
                "§7Cost: §b6 Diamonds",
                purchased ? "" : "§eClick to purchase");
    }

    private static ItemStack buildTrap(BedWarsIsland island) {
        int lvl = island.getTrapLevel();
        boolean ready = island.isTrapReady();
        String status = lvl == 0 ? "§cNo trap purchased"
                : ready ? "§a✔ Trap Level " + lvl + " ready"
                : "§eLevel " + lvl + " (used — repurchase to rearm)";
        String cost;
        if (lvl == 0)      cost = "§7Lvl 1: §b5 Diamonds | Lvl 2: §b10 Diamonds";
        else if (lvl == 1) cost = "§7Upgrade to Lvl 2: §b10 Diamonds";
        else               cost = "§7Max level. Re-buy after use.";

        return icon(Material.TRIPWIRE_HOOK,
                "§eBase Trap",
                status,
                "§7Lvl 1: Blindness 3 for 7s",
                "§7Lvl 2: + Mining Fatigue for 7s",
                cost,
                "§eClick to purchase / upgrade");
    }

    private static ItemStack buildGenUpgrade(BedWarsIsland island) {
        GeneratorTier current = island.getGeneratorTier();
        int nextOrdinal = current.ordinal() + 1;
        boolean maxed = nextOrdinal >= GeneratorTier.values().length;

        String costLine;
        if (maxed) {
            costLine = "§aMax Tier reached!";
        } else {
            int[] ironCosts    = BedWarsIsland.GEN_UPGRADE_COST_IRON;
            int[] goldCosts    = BedWarsIsland.GEN_UPGRADE_COST_GOLD;
            int[] diamondCosts = BedWarsIsland.GEN_UPGRADE_COST_DIAMOND;
            int ic = ironCosts[nextOrdinal], gc = goldCosts[nextOrdinal], dc = diamondCosts[nextOrdinal];
            costLine = "§7Cost: " + (ic > 0 ? "§f" + ic + " Iron " : "")
                    + (gc > 0 ? "§6" + gc + " Gold " : "")
                    + (dc > 0 ? "§b" + dc + " Diamonds" : "");
        }

        return icon(Material.FURNACE,
                "§eGenerator Upgrade",
                "§7Current: §a" + current.getDisplayName(),
                maxed ? "§aMaxed!" : "§7Next tier boosts iron, gold, diamonds, emeralds.",
                costLine,
                maxed ? "" : "§eClick to upgrade");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static ItemStack icon(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(name));
        List<Component> loreList = new ArrayList<>();
        for (String line : lore) {
            if (!line.isEmpty()) loreList.add(LEGACY.deserialize(line));
        }
        meta.lore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack glass() {
        ItemStack item = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }
}
