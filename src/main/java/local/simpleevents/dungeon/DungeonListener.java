package local.simpleevents.dungeon;

import local.simpleevents.SimpleEventsPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles GUI click interactions for the dungeon browser
 * and cleans up players who disconnect mid-dungeon.
 */
public class DungeonListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final SimpleEventsPlugin plugin;
    private final DungeonManager manager;
    private final DungeonGui gui;

    public DungeonListener(SimpleEventsPlugin plugin, DungeonManager manager, DungeonGui gui) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String rawTitle = LEGACY.serialize(event.getView().title());
        if (!rawTitle.equals(DungeonGui.MAIN_TITLE)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        int slot = event.getRawSlot();
        DungeonType[] types = DungeonType.values();

        for (int i = 0; i < DungeonGui.DUNGEON_SLOTS.length && i < types.length; i++) {
            if (slot == DungeonGui.DUNGEON_SLOTS[i]) {
                DungeonType type = types[i];
                if (event.isLeftClick()) {
                    gui.openNormalLoot(player, type);
                } else if (event.isRightClick()) {
                    gui.openHeroicLoot(player, type);
                }
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (manager.isInsideDungeon(event.getPlayer())) {
            manager.exitDungeon(event.getPlayer());
        }
    }
}
