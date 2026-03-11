package local.simpleevents.minigame;

import local.simpleevents.SimpleEventsPlugin;
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
 * Builds and opens the two Minigames GUIs:
 *
 * 1. Main browser  (/minigame)
 *    - Row 0:        dark-purple glass border  (slots 0-8)
 *    - Row 5:        dark-purple glass border  (slots 45-53)
 *    - Left/right column borders each inner row
 *    - Minigame icons at slots 10, 19, 28
 *    - Shop shortcut  at slot 6
 *    - Info book      at slot 4
 *
 * 2. Token shop  (right-click on border slot 6 OR shop item)
 *    - Lootbag I   → slot 11
 *    - Lootbag II  → slot 13
 *    - Lootbag III → slot 15
 *    - Lootbag IV  → slot 29
 *    - Token balance display at top-center
 */
public class MinigameGui {

    public static final String MAIN_TITLE = "§5§lMinigames";
    public static final String SHOP_TITLE = "§5§lMinigame Token Shop";

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    static final int[] MINIGAME_SLOTS = {10, 19, 28};
    static final int SHOP_SHORTCUT_SLOT = 6;

    /** Lootbag icon slots inside the token shop. */
    static final int[] LOOTBAG_SLOTS = {11, 13, 15, 29};

    private final MinigameManager manager;

    public MinigameGui(MinigameManager manager) {
        this.manager = manager;
    }

    // ── Main browser ──────────────────────────────────────────────────────────

    public void openBrowser(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, LEGACY.deserialize(MAIN_TITLE));

        ItemStack border = buildBorder();
        // Top and bottom rows
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        // Side columns
        for (int r = 1; r <= 4; r++) {
            inv.setItem(r * 9, border);
            inv.setItem(r * 9 + 8, border);
        }

        // Info item (top-center)
        inv.setItem(4, buildInfoItem());

        // Shop shortcut (top-right area)
        inv.setItem(SHOP_SHORTCUT_SLOT, buildShopShortcut(player));

        // Minigame icons
        MinigameType[] types = MinigameType.values();
        for (int i = 0; i < types.length && i < MINIGAME_SLOTS.length; i++) {
            inv.setItem(MINIGAME_SLOTS[i], buildMinigameIcon(types[i]));
        }

        player.openInventory(inv);
    }

    // ── Token shop ─────────────────────────────────────────────────────────────

    public void openShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, LEGACY.deserialize(SHOP_TITLE));

        ItemStack border = buildBorder();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int r = 1; r <= 4; r++) {
            inv.setItem(r * 9, border);
            inv.setItem(r * 9 + 8, border);
        }

        // Token balance (top-center)
        inv.setItem(4, buildTokenBalance(player));

        // Back button
        inv.setItem(49, buildBackButton());

        // Lootbags
        LootbagType[] bags = LootbagType.values();
        for (int i = 0; i < bags.length && i < LOOTBAG_SLOTS.length; i++) {
            inv.setItem(LOOTBAG_SLOTS[i], buildLootbagIcon(bags[i], player));
        }

        player.openInventory(inv);
    }

    // ── Detail views ──────────────────────────────────────────────────────────

    /**
     * Opens the detail popup for a minigame (hovering tooltip-style inventory).
     * Shows description, next match time, team size, multiplier, and reward info.
     */
    public void openMinigameDetail(Player player, MinigameType type) {
        String title = "§5" + strip(type.getDisplayName());
        Inventory inv = Bukkit.createInventory(null, 27, LEGACY.deserialize(title));

        ItemStack border = buildBorder();
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        inv.setItem(13, buildMinigameDetailItem(type));
        inv.setItem(22, buildBackButton());

        player.openInventory(inv);
    }

    // ── Item builders ─────────────────────────────────────────────────────────

    private ItemStack buildBorder() {
        ItemStack item = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize("§5§lMinigames Info"));
        List<Component> lore = new ArrayList<>();
        lore.add(LEGACY.deserialize("§7Participate in PvP minigames to earn"));
        lore.add(LEGACY.deserialize("§eMinigame Tokens§7, which you can spend"));
        lore.add(LEGACY.deserialize("§7in the §5Token Shop §7for exclusive loot!"));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§7Click a minigame to see details and join."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildShopShortcut(Player player) {
        int balance = manager.getTokens(player.getUniqueId());
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize("§6§lToken Shop"));
        List<Component> lore = new ArrayList<>();
        lore.add(LEGACY.deserialize("§7Your balance: §6" + balance + " §7token" + (balance == 1 ? "" : "s")));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§eClick to browse lootbags!"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildTokenBalance(Player player) {
        int balance = manager.getTokens(player.getUniqueId());
        ItemStack item = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize("§6§lMinigame Tokens"));
        List<Component> lore = new ArrayList<>();
        lore.add(LEGACY.deserialize("§7Your balance: §6" + balance + " §7token" + (balance == 1 ? "" : "s")));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§7Earn tokens by winning minigames!"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize("§c« Back"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildMinigameIcon(MinigameType type) {
        MinigameSession session = manager.getSession(type);
        ItemStack item = new ItemStack(type.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(type.getDisplayName()));

        List<Component> lore = new ArrayList<>();

        // Status
        lore.add(Component.empty());
        if (session != null) {
            String statusColor = switch (session.getState()) {
                case WAITING    -> "§c";
                case STARTING   -> "§e";
                case IN_PROGRESS -> "§a";
                case ENDED      -> "§8";
            };
            lore.add(LEGACY.deserialize("§fStatus: " + statusColor + session.getState().name().replace('_', ' ')));

            long secsLeft = session.getNextMatchSecondsRemaining();
            if (session.getState() == MinigameState.WAITING && secsLeft > 0) {
                long m = secsLeft / 60, s = secsLeft % 60;
                lore.add(LEGACY.deserialize("§fNext Match: §a" + m + "m " + s + "s"));
            } else if (session.getState() == MinigameState.STARTING) {
                lore.add(LEGACY.deserialize("§fStarting in: §e" + session.getSecondsRemaining() + "s"));
            } else if (session.getState() == MinigameState.IN_PROGRESS) {
                lore.add(LEGACY.deserialize("§fPlayers: §a" + session.getParticipantCount()));
            }

            lore.add(LEGACY.deserialize("§fTeam Size: §a" + type.getTeamSize()));
            lore.add(LEGACY.deserialize("§fPoint/Token Multiplier: §a" + session.getPointMultiplier() + "x"));

            // Multiplier explanation
            if (session.getPointMultiplier() > 1.0 || session.getState() == MinigameState.WAITING) {
                lore.add(LEGACY.deserialize("§8(This multiplier increases by 0.5x each"));
                lore.add(LEGACY.deserialize("§8time the game does not start, up to"));
                lore.add(LEGACY.deserialize("§8a maximum of 3x. Once the game starts"));
                lore.add(LEGACY.deserialize("§8successfully, this multiplier resets"));
                lore.add(LEGACY.deserialize("§8back to 1x)"));
            }
        }

        // Game-specific info
        lore.add(Component.empty());
        for (String line : type.getDescription().split("\n")) {
            lore.add(LEGACY.deserialize("§7" + line));
        }

        // Point rewards
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§5§lPoint Rewards"));
        for (String s : type.getPointRewards()) {
            lore.add(LEGACY.deserialize(s));
        }

        // Actions
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§eLeft-Click §7to join."));
        lore.add(LEGACY.deserialize("§eRight-Click §7to view leaderboards."));
        lore.add(LEGACY.deserialize("§eShift Left-Click §7to view stats."));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildMinigameDetailItem(MinigameType type) {
        ItemStack item = new ItemStack(type.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(type.getDisplayName()));

        List<Component> lore = new ArrayList<>();
        for (String line : type.getDescription().split("\n")) {
            lore.add(LEGACY.deserialize("§7" + line));
        }
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§5§lToken Rewards"));
        lore.add(LEGACY.deserialize("§f1st place: §610 §fMinigame Tokens, §b106 §fCore Chunk Points"));
        lore.add(LEGACY.deserialize("§f2nd place: §64 §fMinigame Tokens, §b53 §fCore Chunk Points"));
        lore.add(LEGACY.deserialize("§f3rd place: §63 §fMinigame Tokens, §b26 §fCore Chunk Points"));
        lore.add(LEGACY.deserialize("§f4th place: §62 §fMinigame Tokens"));
        lore.add(LEGACY.deserialize("§f5th place: §61 §fMinigame Token"));
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§5§lPoint Rewards"));
        for (String s : type.getPointRewards()) {
            lore.add(LEGACY.deserialize(s));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildLootbagIcon(LootbagType bag, Player player) {
        int balance = manager.getTokens(player.getUniqueId());
        int cost    = bag.getTokenCost();
        boolean canAfford = balance >= cost;

        ItemStack item = new ItemStack(bag.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(bag.getDisplayName()));

        List<Component> lore = new ArrayList<>();
        for (String line : bag.getLore()) {
            lore.add(LEGACY.deserialize(line));
        }
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§fCosts"));
        if (canAfford) {
            lore.add(LEGACY.deserialize("§8» §6" + cost + " Minigame Tokens §a✔"));
        } else {
            lore.add(LEGACY.deserialize("§8» §6" + cost + " Minigame Tokens §c✘"));
            lore.add(LEGACY.deserialize("§8  §7(Balance: §6" + balance
                    + "§7, requires §6" + (cost - balance) + " §7more)"));
        }
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize("§eLeft-Click §7to purchase item."));
        lore.add(LEGACY.deserialize("§eRight-Click §7to view lootbox."));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String strip(String legacy) {
        return legacy.replaceAll("§[0-9a-fA-Fk-oK-OrR]", "");
    }
}
