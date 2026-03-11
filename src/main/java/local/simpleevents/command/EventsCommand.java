package local.simpleevents.command;

import local.simpleevents.SimpleEventsPlugin;
import local.simpleevents.boss.BossManager;
import local.simpleevents.boss.BossType;
import local.simpleevents.dungeon.DungeonGui;
import local.simpleevents.dungeon.DungeonManager;
import local.simpleevents.dungeon.DungeonType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * /events — admin command for managing SimpleEvents.
 *
 * Subcommands:
 *   /events boss spawn [type]       — force spawn a boss
 *   /events boss info               — show active boss info
 *   /events boss timer              — show time until next auto-spawn
 *   /events boss kill               — despawn the active boss
 *   /events boss list               — list all boss types
 *   /events dungeon list            — list all dungeon statuses
 *   /events dungeon locations        — list all set dungeon locations
 *   /events dungeon setlocation <type> — set entry location to your position
 *   /events dungeon removelocation <type> — remove stored entry location
 *   /events dungeon key <player> <type> [heroic] — give a dungeon key
 *   /events dungeon enter <player> <type> [heroic] — force-enter a player
 *   /events dungeon exit <player>   — force-exit a player
 *   /events dungeon complete <player> — force-complete a player's run
 */
public class EventsCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "simpleevents.admin";

    private final SimpleEventsPlugin plugin;
    private final BossManager bossManager;
    private final DungeonManager dungeonManager;
    private final DungeonGui dungeonGui;

    public EventsCommand(SimpleEventsPlugin plugin, BossManager bossManager,
                         DungeonManager dungeonManager, DungeonGui dungeonGui) {
        this.plugin = plugin;
        this.bossManager = bossManager;
        this.dungeonManager = dungeonManager;
        this.dungeonGui = dungeonGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "boss" -> handleBoss(sender, args);
            case "dungeon", "dungeons" -> handleDungeon(sender, args);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleBoss(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendBossHelp(sender);
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "spawn" -> {
                if (args.length >= 3) {
                    // Specific boss type
                    String key = args[2].toLowerCase();
                    BossType type = findBossType(key);
                    if (type == null) {
                        sender.sendMessage("§cUnknown boss type: §e" + key);
                        sender.sendMessage("§7Use §e/events boss list §7to see all types.");
                        return true;
                    }
                    if (bossManager.getActiveBoss() != null) {
                        sender.sendMessage("§cA boss is already active! Kill or despawn it first.");
                        return true;
                    }
                    boolean spawned;
                    if (sender instanceof Player player) {
                        spawned = bossManager.spawnBossAt(type, player.getLocation());
                    } else {
                        spawned = bossManager.spawnBoss(type);
                    }
                    if (!spawned) {
                        sender.sendMessage("§cFailed to spawn boss. Check console for details.");
                    }
                } else {
                    // Random
                    if (bossManager.getActiveBoss() != null) {
                        sender.sendMessage("§cA boss is already active! Kill or despawn it first.");
                        return true;
                    }
                    boolean spawned = bossManager.spawnRandomBoss();
                    if (!spawned) {
                        sender.sendMessage("§cFailed to spawn boss. Check console for details.");
                    }
                }
            }
            case "info" -> {
                if (bossManager.getActiveBoss() == null) {
                    sender.sendMessage("§7There is no active boss right now.");
                    return true;
                }
                var active = bossManager.getActiveBoss();
                sender.sendMessage("§6§l--- Active Boss ---");
                sender.sendMessage("§7Name: §f" + active.getType().getDisplayName());
                sender.sendMessage("§7Difficulty: " + active.getType().getDifficulty());
                sender.sendMessage("§7Location: §e" + formatLoc(active.getSpawnLocation()));
                var entity = bossManager.findBossEntity();
                if (entity instanceof org.bukkit.entity.LivingEntity le) {
                    double hp = le.getHealth();
                    double max = active.getType().getMaxHealth();
                    sender.sendMessage(String.format("§7Health: §c%.0f §7/ §c%.0f", hp, max));
                }
            }
            case "timer" -> {
                if (bossManager.getActiveBoss() != null) {
                    sender.sendMessage("§7A boss is currently active.");
                } else {
                    int secs = bossManager.getSecondsUntilNextSpawn();
                    sender.sendMessage("§7Next boss in: §e" + formatTime(secs));
                }
            }
            case "kill", "remove", "despawn" -> {
                if (bossManager.getActiveBoss() == null) {
                    sender.sendMessage("§7There is no active boss to remove.");
                    return true;
                }
                org.bukkit.entity.Entity entity = bossManager.findBossEntity();
                org.bukkit.entity.LivingEntity le = (entity instanceof org.bukkit.entity.LivingEntity living) ? living : null;
                if (le != null) le.remove();
                bossManager.handleBossDeath(le, null);
                sender.sendMessage("§aActive boss removed.");
            }
            case "list" -> {
                sender.sendMessage("§6§l--- Boss Types ---");
                for (BossType type : BossType.values()) {
                    sender.sendMessage("§e" + type.getKey() + " §7— " + type.getDisplayName()
                            + " §8(" + type.getDifficulty() + "§8)");
                }
            }
            case "settimer" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /events boss settimer <seconds>");
                    return true;
                }
                try {
                    int secs = Integer.parseInt(args[2]);
                    bossManager.setSecondsUntilNextSpawn(secs);
                    sender.sendMessage("§aBoss timer set to §e" + formatTime(secs) + "§a.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid number: " + args[2]);
                }
            }
            default -> sendBossHelp(sender);
        }
        return true;
    }

    private boolean handleDungeon(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendDungeonHelp(sender);
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "list" -> {
                sender.sendMessage("§5§l--- Simple Dungeons Status ---");
                for (DungeonType type : DungeonType.values()) {
                    var active = dungeonManager.getActiveDungeon(type);
                    Location storedLoc = dungeonManager.getLocation(type);
                    String locTag = storedLoc != null
                            ? " §8[§7loc set§8]"
                            : " §8[§cno location§8]";
                    if (active == null) {
                        sender.sendMessage("§7" + type.getKey() + " §8(" + stripColor(type.getDisplayName()) + "§8) §7— §8Empty" + locTag);
                    } else {
                        String mode = active.isHeroic() ? " §c[HEROIC]" : "";
                        sender.sendMessage("§a" + type.getKey() + " §8(" + stripColor(type.getDisplayName()) + "§8)" + mode
                                + " §7— §e" + active.getParticipantCount() + " player(s)" + locTag);
                    }
                }
            }
            case "locations" -> {
                sender.sendMessage("§5§l--- Dungeon Locations ---");
                Map<DungeonType, Location> locs = dungeonManager.getAllLocations();
                if (locs.isEmpty()) {
                    sender.sendMessage("§7No dungeon locations have been set.");
                } else {
                    for (var entry : locs.entrySet()) {
                        sender.sendMessage("§r" + entry.getKey().getDisplayName()
                                + " §8» §e" + formatLoc(entry.getValue()));
                    }
                }
                // Also show which dungeons are missing
                for (DungeonType type : DungeonType.values()) {
                    if (!locs.containsKey(type)) {
                        sender.sendMessage("§r" + type.getDisplayName() + " §8» §cNot set");
                    }
                }
            }
            case "setlocation", "setloc" -> {
                // /events dungeon setlocation <type>
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cThis subcommand must be run by a player.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /events dungeon setlocation <type>");
                    return true;
                }
                DungeonType type = DungeonType.fromKey(args[2]);
                if (type == null) {
                    sender.sendMessage("§cUnknown dungeon type: " + args[2]);
                    sender.sendMessage("§7Valid types: sunken_citadel, corrupted_keep, void_sanctum, temporal_rift");
                    return true;
                }
                Location loc = player.getLocation();
                dungeonManager.setLocation(type, loc);
                sender.sendMessage("§aSet entry location for §r" + type.getDisplayName()
                        + " §ato §e" + formatLoc(loc) + "§a.");
            }
            case "removelocation", "removeloc", "dellocation", "delloc" -> {
                // /events dungeon removelocation <type>
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /events dungeon removelocation <type>");
                    return true;
                }
                DungeonType type = DungeonType.fromKey(args[2]);
                if (type == null) {
                    sender.sendMessage("§cUnknown dungeon type: " + args[2]);
                    return true;
                }
                if (dungeonManager.getLocation(type) == null) {
                    sender.sendMessage("§7No location is set for §r" + type.getDisplayName() + "§7.");
                    return true;
                }
                dungeonManager.removeLocation(type);
                sender.sendMessage("§aRemoved entry location for §r" + type.getDisplayName() + "§a.");
            }
            case "key" -> {
                // /events dungeon key <player> <type> [heroic]
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /events dungeon key <player> <type> [heroic]");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found: " + args[2]);
                    return true;
                }
                DungeonType type = DungeonType.fromKey(args[3]);
                if (type == null) {
                    sender.sendMessage("§cUnknown dungeon type: " + args[3]);
                    sender.sendMessage("§7Valid types: sunken_citadel, corrupted_keep, void_sanctum, temporal_rift");
                    return true;
                }
                boolean heroic = args.length >= 5 && args[4].equalsIgnoreCase("heroic");
                var key = dungeonManager.createKey(type, heroic);
                target.getInventory().addItem(key);
                sender.sendMessage("§aGave " + target.getName() + " a " + (heroic ? "§cHeroic §a" : "") + "key for §r" + type.getDisplayName() + "§a.");
                target.sendMessage("§5§lDungeons §8» §7You received a dungeon key for §r" + type.getDisplayName() + "§7!");
            }
            case "enter" -> {
                // /events dungeon enter <player> <type> [heroic]
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /events dungeon enter <player> <type> [heroic]");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) { sender.sendMessage("§cPlayer not found: " + args[2]); return true; }
                DungeonType type = DungeonType.fromKey(args[3]);
                if (type == null) { sender.sendMessage("§cUnknown dungeon type: " + args[3]); return true; }
                boolean heroic = args.length >= 5 && args[4].equalsIgnoreCase("heroic");
                boolean entered = dungeonManager.enterDungeon(target, type, heroic);
                if (!entered) sender.sendMessage("§cFailed to enter dungeon. See player feedback above.");
                else sender.sendMessage("§aForce-added §f" + target.getName() + " §ato §r" + type.getDisplayName() + "§a.");
            }
            case "exit" -> {
                // /events dungeon exit <player>
                if (args.length < 3) { sender.sendMessage("§cUsage: /events dungeon exit <player>"); return true; }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) { sender.sendMessage("§cPlayer not found: " + args[2]); return true; }
                if (!dungeonManager.isInsideDungeon(target)) {
                    sender.sendMessage("§7" + target.getName() + " is not in a dungeon.");
                    return true;
                }
                dungeonManager.exitDungeon(target);
                target.sendMessage("§5§lDungeons §8» §7You have been removed from the dungeon.");
                sender.sendMessage("§aRemoved §f" + target.getName() + " §afrom their dungeon.");
            }
            case "complete" -> {
                // /events dungeon complete <player>
                if (args.length < 3) { sender.sendMessage("§cUsage: /events dungeon complete <player>"); return true; }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) { sender.sendMessage("§cPlayer not found: " + args[2]); return true; }
                DungeonType type = dungeonManager.getPlayerDungeon(target);
                if (type == null) {
                    sender.sendMessage("§c" + target.getName() + " is not in a dungeon.");
                    return true;
                }
                var active = dungeonManager.getActiveDungeon(type);
                boolean heroic = active != null && active.isHeroic();
                dungeonManager.completeRun(target, type, heroic);
                sender.sendMessage("§aForce-completed " + target.getName() + "'s dungeon run.");
            }
            default -> sendDungeonHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) return List.of();

        if (args.length == 1) return filter(List.of("boss", "dungeon"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("boss")) {
            return filter(List.of("spawn", "info", "timer", "kill", "list", "settimer"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("boss") && args[1].equalsIgnoreCase("spawn")) {
            List<String> keys = new ArrayList<>();
            for (BossType t : BossType.values()) keys.add(t.getKey());
            return filter(keys, args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("dungeon")) {
            return filter(List.of("list", "locations", "setlocation", "removelocation",
                    "key", "enter", "exit", "complete"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("dungeon")
                && List.of("key", "enter", "exit", "complete").contains(args[1].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("dungeon")
                && List.of("setlocation", "setloc", "removelocation", "removeloc").contains(args[1].toLowerCase())) {
            List<String> keys = new ArrayList<>();
            for (DungeonType t : DungeonType.values()) keys.add(t.getKey());
            return filter(keys, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("dungeon")
                && List.of("key", "enter").contains(args[1].toLowerCase())) {
            List<String> keys = new ArrayList<>();
            for (DungeonType t : DungeonType.values()) keys.add(t.getKey());
            return filter(keys, args[3]);
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("dungeon")
                && List.of("key", "enter").contains(args[1].toLowerCase())) {
            return filter(List.of("heroic"), args[4]);
        }
        return List.of();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private BossType findBossType(String key) {
        for (BossType type : BossType.values()) {
            if (type.getKey().equalsIgnoreCase(key) || type.name().equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l--- SimpleEvents Help ---");
        sender.sendMessage("§e/events boss spawn [type] §7— Spawn a warzone boss");
        sender.sendMessage("§e/events boss info §7— Info on the active boss");
        sender.sendMessage("§e/events boss timer §7— Time until next boss spawn");
        sender.sendMessage("§e/events boss kill §7— Despawn the active boss");
        sender.sendMessage("§e/events boss list §7— List all boss types");
        sender.sendMessage("§e/events boss settimer <seconds> §7— Set next spawn timer");
        sender.sendMessage("§5/events dungeon list §7— List all dungeon statuses");
        sender.sendMessage("§5/events dungeon locations §7— List all set entry locations");
        sender.sendMessage("§5/events dungeon setlocation <type> §7— Set entry location to your position");
        sender.sendMessage("§5/events dungeon removelocation <type> §7— Remove stored entry location");
        sender.sendMessage("§5/events dungeon key <player> <type> [heroic] §7— Give a dungeon key");
        sender.sendMessage("§5/events dungeon enter <player> <type> [heroic] §7— Force-enter a player");
        sender.sendMessage("§5/events dungeon exit <player> §7— Force-exit a player");
        sender.sendMessage("§5/events dungeon complete <player> §7— Force-complete a run");
    }

    private void sendDungeonHelp(CommandSender sender) {
        sender.sendMessage("§5§l--- Dungeon Subcommands ---");
        sender.sendMessage("§5/events dungeon list");
        sender.sendMessage("§5/events dungeon locations");
        sender.sendMessage("§5/events dungeon setlocation <type>");
        sender.sendMessage("§5/events dungeon removelocation <type>");
        sender.sendMessage("§5/events dungeon key <player> <type> [heroic]");
        sender.sendMessage("§5/events dungeon enter <player> <type> [heroic]");
        sender.sendMessage("§5/events dungeon exit <player>");
        sender.sendMessage("§5/events dungeon complete <player>");
    }

    private static String stripColor(String s) {
        return s.replaceAll("§.", "");
    }

    private void sendBossHelp(CommandSender sender) {
        sender.sendMessage("§6§l--- Boss Subcommands ---");
        sender.sendMessage("§e/events boss spawn [type]");
        sender.sendMessage("§e/events boss info");
        sender.sendMessage("§e/events boss timer");
        sender.sendMessage("§e/events boss kill");
        sender.sendMessage("§e/events boss list");
        sender.sendMessage("§e/events boss settimer <seconds>");
    }

    private static List<String> filter(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(prefix.toLowerCase())) result.add(opt);
        }
        return result;
    }

    private static String formatLoc(org.bukkit.Location loc) {
        return loc.getWorld().getName() + " (" + loc.getBlockX() + ", "
                + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }

    private static String formatTime(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}
