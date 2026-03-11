package local.simpleevents.dungeon;

import local.simpleevents.SimpleEventsPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Manages dungeon instances, player tracking, key issuance, and rewards.
 */
public class DungeonManager {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    /** One active instance per dungeon type (players share an instance). */
    private final Map<DungeonType, ActiveDungeon> activeDungeons = new EnumMap<>(DungeonType.class);

    /** Maps each player UUID to the dungeon they're currently inside. */
    private final Map<UUID, DungeonType> playerDungeons = new HashMap<>();

    private final SimpleEventsPlugin plugin;

    public DungeonManager(SimpleEventsPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Location storage ───────────────────────────────────────────────────────

    private static final String LOC_PATH = "dungeons.locations";

    /**
     * Saves the dungeon's entry location to config.yml.
     */
    public void setLocation(DungeonType type, Location loc) {
        String path = LOC_PATH + "." + type.getKey() + ".";
        FileConfiguration cfg = plugin.getConfig();
        cfg.set(path + "world", loc.getWorld().getName());
        cfg.set(path + "x", loc.getX());
        cfg.set(path + "y", loc.getY());
        cfg.set(path + "z", loc.getZ());
        cfg.set(path + "yaw", (double) loc.getYaw());
        cfg.set(path + "pitch", (double) loc.getPitch());
        plugin.saveConfig();
    }

    /**
     * Removes the dungeon's stored entry location from config.yml.
     */
    public void removeLocation(DungeonType type) {
        plugin.getConfig().set(LOC_PATH + "." + type.getKey(), null);
        plugin.saveConfig();
    }

    /**
     * Returns the stored entry location for this dungeon, or null if unset.
     */
    public Location getLocation(DungeonType type) {
        String path = LOC_PATH + "." + type.getKey();
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.contains(path + ".world")) return null;
        String worldName = cfg.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("[Dungeons] World '" + worldName + "' for dungeon '"
                    + type.getKey() + "' is not loaded.");
            return null;
        }
        double x = cfg.getDouble(path + ".x");
        double y = cfg.getDouble(path + ".y");
        double z = cfg.getDouble(path + ".z");
        float yaw = (float) cfg.getDouble(path + ".yaw");
        float pitch = (float) cfg.getDouble(path + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Returns a map of every dungeon type that has a location set, with its Location.
     */
    public Map<DungeonType, Location> getAllLocations() {
        Map<DungeonType, Location> result = new LinkedHashMap<>();
        for (DungeonType type : DungeonType.values()) {
            Location loc = getLocation(type);
            if (loc != null) result.put(type, loc);
        }
        return result;
    }

    // ── Instance management ────────────────────────────────────────────────────

    /** Returns the active dungeon for the given type, or null if empty. */
    public ActiveDungeon getActiveDungeon(DungeonType type) {
        return activeDungeons.get(type);
    }

    public Map<DungeonType, ActiveDungeon> getAllActiveDungeons() {
        return Collections.unmodifiableMap(activeDungeons);
    }

    /**
     * Adds the player to a dungeon run. Creates a new instance if none exists.
     * Returns false if the player is already in a dungeon or there is a mode conflict.
     */
    public boolean enterDungeon(Player player, DungeonType type, boolean heroic) {
        if (playerDungeons.containsKey(player.getUniqueId())) {
            player.sendMessage("§5§lDungeons §8» §cYou are already inside a dungeon!");
            return false;
        }

        ActiveDungeon existing = activeDungeons.get(type);
        if (existing == null) {
            List<DungeonDebuff> debuffs = heroic ? pickRandomDebuffs(2) : List.of();
            existing = new ActiveDungeon(type, heroic, debuffs);
            activeDungeons.put(type, existing);
        } else if (existing.isHeroic() != heroic) {
            String mode = existing.isHeroic() ? "§cHeroic" : "§eNormal";
            player.sendMessage("§5§lDungeons §8» §7A " + mode + " §7instance of this dungeon is already running!");
            return false;
        }

        existing.addParticipant(player);
        playerDungeons.put(player.getUniqueId(), type);
        broadcastEntry(player, type, heroic, existing.getParticipantCount());
        return true;
    }

    /**
     * Removes the player from whichever dungeon they are in.
     * Cleans up the instance if it becomes empty.
     */
    public void exitDungeon(Player player) {
        DungeonType type = playerDungeons.remove(player.getUniqueId());
        if (type == null) return;
        ActiveDungeon dungeon = activeDungeons.get(type);
        if (dungeon == null) return;
        dungeon.removeParticipant(player);
        if (dungeon.getParticipantCount() == 0) {
            activeDungeons.remove(type);
        }
    }

    public boolean isInsideDungeon(Player player) {
        return playerDungeons.containsKey(player.getUniqueId());
    }

    public DungeonType getPlayerDungeon(Player player) {
        return playerDungeons.get(player.getUniqueId());
    }

    // ── Rewards ────────────────────────────────────────────────────────────────

    /**
     * Marks a dungeon run complete for the player: awards money, gives lootbag, broadcasts.
     */
    public void completeRun(Player player, DungeonType type, boolean heroic) {
        exitDungeon(player);

        long rewardMin = heroic ? type.getHeroicRewardMin() : type.getNormalRewardMin();
        long rewardMax = heroic ? type.getHeroicRewardMax() : type.getNormalRewardMax();
        long reward = randomInRange(rewardMin, rewardMax);

        // Vault economy
        var rsp = Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp != null) {
            rsp.getProvider().depositPlayer(player, (double) reward);
        }

        // Lootbag item
        ItemStack lootbag = buildLootbag(type, heroic, reward);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(lootbag);
        if (!overflow.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflow.get(0));
        }

        // Next-dungeon unlock reward description
        if (type.getUnlockRewardDescription() != null && type.ordinal() < DungeonType.values().length - 1) {
            DungeonType next = DungeonType.values()[type.ordinal() + 1];
            player.sendMessage("§5§lDungeons §8» §7You unlocked: §r" + next.getDisplayName());
        }

        broadcastComplete(player, type, heroic, reward);
    }

    // ── Key creation ──────────────────────────────────────────────────────────

    public ItemStack createKey(DungeonType type, boolean heroic) {
        return DungeonKey.create(plugin, type, heroic);
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────

    public void shutdown() {
        activeDungeons.clear();
        playerDungeons.clear();
    }

    // ── Broadcasts ─────────────────────────────────────────────────────────────

    private void broadcastEntry(Player player, DungeonType type, boolean heroic, int total) {
        String bar = "§8§m                                        ";
        String mode = heroic ? " §c[HEROIC]" : "";
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage("");
            p.sendMessage(bar);
            p.sendMessage("  §5§l⚔ DUNGEON ENTERED ⚔");
            p.sendMessage("  §f" + player.getName() + " §7has entered §r" + type.getDisplayName() + mode + "§7.");
            p.sendMessage("  §7Players inside: §e" + total);
            p.sendMessage(bar);
            p.sendMessage("");
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_PORTAL_TRAVEL, 0.4f, 1.5f);
        });
    }

    private void broadcastComplete(Player player, DungeonType type, boolean heroic, long reward) {
        String bar = "§8§m                                        ";
        String mode = heroic ? " §c[HEROIC]" : "";
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage("");
            p.sendMessage(bar);
            p.sendMessage("  §a§l✔ DUNGEON COMPLETE ✔");
            p.sendMessage("  §f" + player.getName() + " §7completed §r" + type.getDisplayName() + mode + "§7!");
            p.sendMessage("  §7Reward: §a$" + String.format("%,d", reward));
            p.sendMessage(bar);
            p.sendMessage("");
            p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.0f);
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private List<DungeonDebuff> pickRandomDebuffs(int count) {
        List<DungeonDebuff> all = new ArrayList<>(Arrays.asList(DungeonDebuff.values()));
        Collections.shuffle(all);
        return new ArrayList<>(all.subList(0, Math.min(count, all.size())));
    }

    private long randomInRange(long min, long max) {
        if (min >= max) return min;
        return min + (long) (Math.random() * (max - min + 1));
    }

    private ItemStack buildLootbag(DungeonType type, boolean heroic, long reward) {
        ItemStack item = new ItemStack(Material.BUNDLE);
        var meta = item.getItemMeta();
        String prefix = heroic ? "§c§l" : "§e§l";
        meta.displayName(LEGACY.deserialize(prefix + "Dungeon Lootbag"));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(LEGACY.deserialize("§7From: §r" + type.getDisplayName()));
        lore.add(LEGACY.deserialize("§7Mode: " + (heroic ? "§c§lHeroic" : "§eNormal")));
        lore.add(LEGACY.deserialize(""));
        lore.add(LEGACY.deserialize("§7Redeem this bag for:"));
        lore.add(LEGACY.deserialize("§a  $" + String.format("%,d", reward)));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
