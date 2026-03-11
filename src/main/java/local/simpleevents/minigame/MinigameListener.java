package local.simpleevents.minigame;

import local.simpleevents.SimpleEventsPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles all GUI click interactions for the Minigames browser and Token Shop.
 */
public class MinigameListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final SimpleEventsPlugin plugin;
    private final MinigameManager manager;
    private final MinigameGui gui;

    public MinigameListener(SimpleEventsPlugin plugin, MinigameManager manager, MinigameGui gui) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String rawTitle = LEGACY.serialize(event.getView().title());

        if (rawTitle.equals(MinigameGui.MAIN_TITLE)) {
            handleBrowserClick(event, player);
        } else if (rawTitle.equals(MinigameGui.SHOP_TITLE)) {
            handleShopClick(event, player);
        }
    }

    private void handleBrowserClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        int slot = event.getRawSlot();

        // Shop shortcut
        if (slot == MinigameGui.SHOP_SHORTCUT_SLOT) {
            gui.openShop(player);
            return;
        }

        // Minigame icons
        MinigameType[] types = MinigameType.values();
        for (int i = 0; i < MinigameGui.MINIGAME_SLOTS.length && i < types.length; i++) {
            if (slot == MinigameGui.MINIGAME_SLOTS[i]) {
                MinigameType type = types[i];
                if (event.isLeftClick() && !event.isShiftClick()) {
                    // Join the session
                    manager.joinSession(player, type);
                    player.closeInventory();
                } else if (event.isRightClick()) {
                    // View leaderboard placeholder
                    player.sendMessage("§5[Minigames] §7Leaderboard for §5"
                            + LEGACY.serialize(LEGACY.deserialize(type.getDisplayName()))
                            + " §7is not yet available.");
                } else if (event.isShiftClick() && event.isLeftClick()) {
                    // View stats placeholder
                    player.sendMessage("§5[Minigames] §7Stats for §5"
                            + LEGACY.serialize(LEGACY.deserialize(type.getDisplayName()))
                            + " §7are not yet tracked.");
                }
                return;
            }
        }
    }

    private void handleShopClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        int slot = event.getRawSlot();

        // Back button
        if (slot == 49) {
            gui.openBrowser(player);
            return;
        }

        // Lootbag purchases
        LootbagType[] bags = LootbagType.values();
        for (int i = 0; i < MinigameGui.LOOTBAG_SLOTS.length && i < bags.length; i++) {
            if (slot == MinigameGui.LOOTBAG_SLOTS[i]) {
                LootbagType bag = bags[i];
                if (event.isLeftClick()) {
                    purchaseLootbag(player, bag);
                } else if (event.isRightClick()) {
                    // View lootbox contents (display only – no purchase)
                    player.sendMessage("§5[Minigames] §7" + LEGACY.serialize(LEGACY.deserialize(bag.getDisplayName()))
                            + " §7contains §6" + bag.getItemCount() + " §7random rewards.");
                }
                return;
            }
        }
    }

    private void purchaseLootbag(Player player, LootbagType bag) {
        int cost    = bag.getTokenCost();
        int balance = manager.getTokens(player.getUniqueId());
        if (balance < cost) {
            player.sendMessage("§c[Minigames] You need §6" + cost + " §cMinigame Token"
                    + (cost == 1 ? "" : "s") + " but only have §6" + balance + "§c.");
            // Refresh shop so the affordability indicator updates
            gui.openShop(player);
            return;
        }
        manager.spendTokens(player.getUniqueId(), cost);
        player.sendMessage("§a[Minigames] §7You purchased §a"
                + LEGACY.serialize(LEGACY.deserialize(bag.getDisplayName()))
                + "§7! §7(Remaining: §6" + manager.getTokens(player.getUniqueId()) + " §7tokens)");
        // TODO: give the actual lootbag item to the player's inventory once loot tables are defined
        player.closeInventory();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Remove the player from any active session when they disconnect
        manager.leaveSession(event.getPlayer());
    }
}
