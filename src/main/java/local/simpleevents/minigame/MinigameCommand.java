package local.simpleevents.minigame;

import local.simpleevents.SimpleEventsPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * /minigame command handler.
 *
 * Player sub-commands:
 *   /minigame            – open the minigames browser GUI
 *   /minigame shop       – open the token shop
 *   /minigame leave      – leave your current session
 *   /minigame tokens     – check your token balance
 *
 * Admin sub-commands (requires simpleevents.minigame.admin):
 *   /minigame start <type>         – force-start a minigame
 *   /minigame end <type>           – force-end a minigame
 *   /minigame setspawn <type>      – set spawn location to your position
 *   /minigame tokens give <player> <amount>
 *   /minigame tokens take <player> <amount>
 *   /minigame tokens set  <player> <amount>
 */
public class MinigameCommand implements CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final String ADMIN_PERM = "simpleevents.minigame.admin";

    private final SimpleEventsPlugin plugin;
    private final MinigameManager manager;
    private final MinigameGui gui;

    public MinigameCommand(SimpleEventsPlugin plugin, MinigameManager manager, MinigameGui gui) {
        this.plugin   = plugin;
        this.manager  = manager;
        this.gui      = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cThis command is player-only.");
                return true;
            }
            gui.openBrowser(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "shop" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("§cPlayer only."); return true; }
                gui.openShop(player);
            }
            case "leave" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("§cPlayer only."); return true; }
                if (!manager.leaveSession(player)) {
                    player.sendMessage("§c[Minigames] You are not in any minigame session.");
                }
            }
            case "tokens"     -> handleTokens(sender, args);
            case "start"      -> handleStart(sender, args);
            case "forcestart" -> handleForceStart(sender, args);
            case "end"        -> handleEnd(sender, args);
            case "setspawn"   -> handleSetSpawn(sender, args);
            default -> sender.sendMessage("§c[Minigames] Unknown sub-command. Try /minigame.");
        }

        return true;
    }

    // ── Sub-command handlers ───────────────────────────────────────────────────

    private void handleTokens(CommandSender sender, String[] args) {
        // /minigame tokens                     → own balance (player only)
        // /minigame tokens give|take|set <player> <amount>   → admin
        if (args.length == 1) {
            if (!(sender instanceof Player player)) { sender.sendMessage("§cPlayer only."); return; }
            int bal = manager.getTokens(player.getUniqueId());
            player.sendMessage("§6[Minigames] §7Your token balance: §6" + bal);
            return;
        }

        if (!sender.hasPermission(ADMIN_PERM)) {
            sender.sendMessage("§c[Minigames] No permission.");
            return;
        }

        if (args.length < 4) {
            sender.sendMessage("§cUsage: /minigame tokens <give|take|set> <player> <amount>");
            return;
        }

        String operation  = args[1].toLowerCase();
        String targetName = args[2];
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
            if (amount < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage("§c[Minigames] Amount must be a non-negative integer.");
            return;
        }

        org.bukkit.OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);
        UUID uid = target.getUniqueId();

        switch (operation) {
            case "give" -> {
                manager.addTokens(uid, amount);
                sender.sendMessage("§a[Minigames] §7Gave §6" + amount + " §7tokens to §a" + targetName + "§7.");
                Player online = plugin.getServer().getPlayer(uid);
                if (online != null) online.sendMessage("§6[Minigames] §7You received §6" + amount + " §7Minigame Token" + (amount == 1 ? "" : "s") + ".");
            }
            case "take" -> {
                int current = manager.getTokens(uid);
                int newBal  = Math.max(0, current - amount);
                manager.setTokens(uid, newBal);
                sender.sendMessage("§a[Minigames] §7Took §6" + (current - newBal) + " §7tokens from §a" + targetName + "§7.");
            }
            case "set" -> {
                manager.setTokens(uid, amount);
                sender.sendMessage("§a[Minigames] §7Set §a" + targetName + "§7's tokens to §6" + amount + "§7.");
            }
            default -> sender.sendMessage("§cUsage: /minigame tokens <give|take|set> <player> <amount>");
        }
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERM)) { sender.sendMessage("§c[Minigames] No permission."); return; }
        if (args.length < 2) { sender.sendMessage("§cUsage: /minigame start <bedwars|skywars|battleroyale>"); return; }
        MinigameType type = resolveType(args[1]);
        if (type == null) { sender.sendMessage("§cUnknown minigame type."); return; }
        if (manager.forceStart(type)) {
            sender.sendMessage("§a[Minigames] Starting " + type.getKey() + " countdown.");
        } else {
            sender.sendMessage("§c[Minigames] That session cannot be started right now.");
        }
    }

    private void handleForceStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERM)) { sender.sendMessage("§c[Minigames] No permission."); return; }
        if (args.length < 2) { sender.sendMessage("§cUsage: /minigame forcestart <bedwars|skywars|battleroyale>"); return; }
        MinigameType type = resolveType(args[1]);
        if (type == null) { sender.sendMessage("§cUnknown minigame type."); return; }
        if (manager.forceStartImmediate(type)) {
            sender.sendMessage("§a[Minigames] Force-started §6" + type.getKey() + " §aimmediately (no countdown, no player minimum).");
        } else {
            sender.sendMessage("§c[Minigames] Could not force-start — session may be in-progress, or nobody has joined yet.");
        }
    }

    private void handleEnd(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERM)) { sender.sendMessage("§c[Minigames] No permission."); return; }
        if (args.length < 2) { sender.sendMessage("§cUsage: /minigame end <bedwars|skywars|battleroyale>"); return; }
        MinigameType type = resolveType(args[1]);
        if (type == null) { sender.sendMessage("§cUnknown minigame type."); return; }
        if (manager.forceEnd(type)) {
            sender.sendMessage("§a[Minigames] Ended " + type.getKey() + " session.");
        } else {
            sender.sendMessage("§c[Minigames] That session cannot be ended right now.");
        }
    }

    private void handleSetSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERM)) { sender.sendMessage("§c[Minigames] No permission."); return; }
        if (!(sender instanceof Player player)) { sender.sendMessage("§cPlayer only."); return; }
        if (args.length < 2) { sender.sendMessage("§cUsage: /minigame setspawn <bedwars|skywars|battleroyale>"); return; }
        MinigameType type = resolveType(args[1]);
        if (type == null) { sender.sendMessage("§cUnknown minigame type."); return; }
        manager.setSpawnLocation(type, player.getLocation());
        player.sendMessage("§a[Minigames] Spawn location for §6" + type.getKey() + " §aset.");
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("shop", "leave", "tokens"));
            if (sender.hasPermission(ADMIN_PERM)) {
                subs.addAll(Arrays.asList("start", "forcestart", "end", "setspawn"));
            }
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            List<String> needsType = Arrays.asList("start", "forcestart", "end", "setspawn");
            if (needsType.contains(sub)) {
                for (MinigameType t : MinigameType.values()) {
                    if (t.getKey().startsWith(args[1].toLowerCase())) completions.add(t.getKey());
                }
            } else if (sub.equals("tokens") && sender.hasPermission(ADMIN_PERM)) {
                for (String op : Arrays.asList("give", "take", "set")) {
                    if (op.startsWith(args[1].toLowerCase())) completions.add(op);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("tokens") && sender.hasPermission(ADMIN_PERM)) {
            plugin.getServer().getOnlinePlayers().forEach(p -> {
                if (p.getName().toLowerCase().startsWith(args[2].toLowerCase())) completions.add(p.getName());
            });
        }

        return completions;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private MinigameType resolveType(String input) {
        // Accept key (bed_wars) or short alias (bedwars, skywars, battleroyale)
        for (MinigameType t : MinigameType.values()) {
            if (t.getKey().equalsIgnoreCase(input)
                    || t.getKey().replace("_", "").equalsIgnoreCase(input)) {
                return t;
            }
        }
        return MinigameType.fromKey(input);
    }
}
