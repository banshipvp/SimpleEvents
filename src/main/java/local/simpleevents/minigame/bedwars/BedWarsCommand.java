package local.simpleevents.minigame.bedwars;

import local.simpleevents.SimpleEventsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin command: /bedwars (alias /bw)
 *
 * Subcommands:
 *   /bw setspawn   <team>              – set team spawn to your location
 *   /bw setbed     <team>              – set team bed location
 *   /bw setgenerator <team>            – set island generator location
 *   /bw setchest   <team>              – set team chest location
 *   /bw setnpc     shop|upgrader <team>– spawn NPC at your location
 *   /bw setdiamond                     – set central diamond generator
 *   /bw setemerald                     – set central emerald generator
 *   /bw start                          – force start the game
 *   /bw end                            – force end the game
 *   /bw addplayer  <player> <team>     – add a player to a team
 *
 * Permission: simpleevents.bedwars.admin
 */
public class BedWarsCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "simpleevents.bedwars.admin";
    private static final String PREFIX = "§5[Bed Wars] §r";

    private final SimpleEventsPlugin plugin;
    private final BedWarsGame        game;
    private final BedWarsNpcManager  npcManager;

    // Central generator locations (stored separately since they're not per-island)
    private org.bukkit.Location diamondGenLoc;
    private org.bukkit.Location emeraldGenLoc;

    public BedWarsCommand(SimpleEventsPlugin plugin, BedWarsGame game, BedWarsNpcManager npcManager) {
        this.plugin     = plugin;
        this.game       = game;
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "setspawn"      -> cmdSetSpawn(sender, args);
            case "setbed"        -> cmdSetBed(sender, args);
            case "setgenerator"  -> cmdSetGenerator(sender, args);
            case "setchest"      -> cmdSetChest(sender, args);
            case "setnpc"        -> cmdSetNpc(sender, args);
            case "setdiamond"    -> cmdSetDiamond(sender);
            case "setemerald"    -> cmdSetEmerald(sender);
            case "start"         -> cmdStart(sender);
            case "end"           -> cmdEnd(sender);
            case "addplayer"     -> cmdAddPlayer(sender, args);
            default              -> sendHelp(sender);
        }
        return true;
    }

    // ── Subcommand handlers ───────────────────────────────────────────────────

    private void cmdSetSpawn(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        BedWarsTeam team = parseTeam(sender, args, 1);
        if (team == null) return;

        game.getIsland(team).setSpawnLocation(player.getLocation());
        player.sendMessage(PREFIX + team.getDisplayName() + "§r spawn set to your location.");
    }

    private void cmdSetBed(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        BedWarsTeam team = parseTeam(sender, args, 1);
        if (team == null) return;

        game.getIsland(team).setBedLocation(player.getLocation());
        player.sendMessage(PREFIX + team.getDisplayName() + "§r bed location set.");
    }

    private void cmdSetGenerator(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        BedWarsTeam team = parseTeam(sender, args, 1);
        if (team == null) return;

        game.getIsland(team).setIslandGeneratorLoc(player.getLocation());
        player.sendMessage(PREFIX + team.getDisplayName() + "§r island generator location set.");
    }

    private void cmdSetChest(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        BedWarsTeam team = parseTeam(sender, args, 1);
        if (team == null) return;

        game.getIsland(team).setTeamChestLocation(player.getLocation());
        player.sendMessage(PREFIX + team.getDisplayName() + "§r team chest location set.");
    }

    private void cmdSetNpc(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "§cUsage: /bw setnpc <shop|upgrader> <team>");
            return;
        }
        String roleArg = args[1].toLowerCase();
        BedWarsTeam team = parseTeam(sender, args, 2);
        if (team == null) return;

        BedWarsIsland island = game.getIsland(team);
        switch (roleArg) {
            case "shop" -> {
                island.setShopNpcLocation(player.getLocation());
                npcManager.spawnShopNpc(team);
                player.sendMessage(PREFIX + team.getDisplayName() + "§r shop NPC spawned.");
            }
            case "upgrader" -> {
                island.setUpgraderNpcLocation(player.getLocation());
                npcManager.spawnUpgraderNpc(team);
                player.sendMessage(PREFIX + team.getDisplayName() + "§r upgrader NPC spawned.");
            }
            default -> sender.sendMessage(PREFIX + "§cRole must be 'shop' or 'upgrader'.");
        }
    }

    private void cmdSetDiamond(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        diamondGenLoc = player.getLocation();
        CentralGenerator cg = game.getCentralGenerator();
        if (cg != null) cg.setDiamondLocation(diamondGenLoc);
        player.sendMessage(PREFIX + "§bCentral diamond generator location set.");
    }

    private void cmdSetEmerald(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        emeraldGenLoc = player.getLocation();
        CentralGenerator cg = game.getCentralGenerator();
        if (cg != null) cg.setEmeraldLocation(emeraldGenLoc);
        player.sendMessage(PREFIX + "§aCentral emerald generator location set.");
    }

    private void cmdStart(CommandSender sender) {
        if (game.isEnded()) {
            sender.sendMessage(PREFIX + "§cGame has already ended.");
            return;
        }
        // Create and wire the central generator if locations are set
        if (diamondGenLoc != null || emeraldGenLoc != null) {
            CentralGenerator cg = new CentralGenerator(plugin, diamondGenLoc, emeraldGenLoc);
            game.setCentralGenerator(cg);
            cg.start();
        }
        game.start();
        sender.sendMessage(PREFIX + "§aGame started!");
    }

    private void cmdEnd(CommandSender sender) {
        game.stop();
        npcManager.removeAll();
        sender.sendMessage(PREFIX + "§cGame ended.");
    }

    private void cmdAddPlayer(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "§cUsage: /bw addplayer <player> <team>");
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) { sender.sendMessage(PREFIX + "§cPlayer not found."); return; }
        BedWarsTeam team = parseTeam(sender, args, 2);
        if (team == null) return;

        game.addPlayer(target, team);
        sender.sendMessage(PREFIX + "Added " + target.getName() + " to " + team.getDisplayName() + "§r.");
        target.sendMessage(PREFIX + "You have been added to " + team.getDisplayName() + "§r team!");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "§cThis command must be run by a player.");
            return null;
        }
        return player;
    }

    private BedWarsTeam parseTeam(CommandSender sender, String[] args, int idx) {
        if (args.length <= idx) {
            sender.sendMessage(PREFIX + "§cSpecify a team: RED, BLUE, GREEN, YELLOW");
            return null;
        }
        try {
            return BedWarsTeam.valueOf(args[idx].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(PREFIX + "§cInvalid team. Choose: RED, BLUE, GREEN, YELLOW");
            return null;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§5--- Bed Wars Admin Commands ---");
        sender.sendMessage("§e/bw setspawn <team>          §7- Set team spawn");
        sender.sendMessage("§e/bw setbed <team>            §7- Set team bed location");
        sender.sendMessage("§e/bw setgenerator <team>      §7- Set island generator");
        sender.sendMessage("§e/bw setchest <team>          §7- Set team chest location");
        sender.sendMessage("§e/bw setnpc shop <team>       §7- Spawn shop NPC");
        sender.sendMessage("§e/bw setnpc upgrader <team>   §7- Spawn upgrader NPC");
        sender.sendMessage("§e/bw setdiamond               §7- Set central diamond gen");
        sender.sendMessage("§e/bw setemerald               §7- Set central emerald gen");
        sender.sendMessage("§e/bw addplayer <player> <team>§7- Add player to team");
        sender.sendMessage("§e/bw start                    §7- Start the game");
        sender.sendMessage("§e/bw end                      §7- End the game");
    }

    // ── Tab completion ─────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERM)) return Collections.emptyList();

        List<String> teamNames = Arrays.stream(BedWarsTeam.values())
                .map(t -> t.name().toLowerCase())
                .collect(Collectors.toList());

        if (args.length == 1) {
            return Arrays.asList("setspawn", "setbed", "setgenerator", "setchest",
                    "setnpc", "setdiamond", "setemerald", "start", "end", "addplayer");
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "setspawn", "setbed", "setgenerator", "setchest" -> teamNames;
                case "setnpc"    -> Arrays.asList("shop", "upgrader");
                case "addplayer" -> null; // player names
                default          -> Collections.emptyList();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setnpc")) {
            return teamNames;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("addplayer")) {
            return teamNames;
        }
        return Collections.emptyList();
    }
}
