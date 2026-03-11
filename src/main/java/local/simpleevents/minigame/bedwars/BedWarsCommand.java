package local.simpleevents.minigame.bedwars;

import local.simpleevents.SimpleEventsPlugin;
import local.simplefactions.FactionManager;
import local.simplefactions.SimpleFactionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /bedwars  (alias /bw)
 *
 * Player commands (no special permission):
 *   /bw join                – join the lobby
 *   /bw leave               – leave the lobby
 *   /bw lobby               – list lobby players
 *   /bw status              – show phase and team summary
 *
 * Admin commands (permission: simpleevents.bedwars.admin):
 *   /bw start               – form teams from lobby players and begin the game
 *   /bw end                 – force-end and clean up
 *   /bw reset               – return to LOBBY phase for a fresh match
 *
 *   /bw set teamspawn    <colour>  – save team spawn (your location)
 *   /bw set teamgenerator <colour> – save island generator location (your location)
 *   /bw set teamshop     <colour>  – save shop NPC location and spawn NPC
 *   /bw set teamupgrade  <colour>  – save upgrader NPC location and spawn NPC
 *   /bw set teambed      <colour>  – save team bed location
 *   /bw set teamchest    <colour>  – save team chest location
 *   /bw set diamond                – save central diamond generator location
 *   /bw set emerald                – save central emerald generator location
 */
public class BedWarsCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN = "simpleevents.bedwars.admin";
    private static final String PREFIX = "§5[Bed Wars] §r";

    private final SimpleEventsPlugin plugin;
    private final BedWarsGame        game;
    private final BedWarsNpcManager  npcManager;
    private final BedWarsLocationStore locationStore;

    public BedWarsCommand(SimpleEventsPlugin plugin, BedWarsGame game,
                          BedWarsNpcManager npcManager, BedWarsLocationStore locationStore) {
        this.plugin        = plugin;
        this.game          = game;
        this.npcManager    = npcManager;
        this.locationStore = locationStore;
    }

    // ── Dispatch ───────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "join"   -> cmdJoin(sender);
            case "leave"  -> cmdLeave(sender);
            case "lobby"  -> cmdLobby(sender);
            case "status" -> cmdStatus(sender);
            case "start"  -> { if (checkAdmin(sender)) cmdStart(sender); }
            case "end"    -> { if (checkAdmin(sender)) cmdEnd(sender); }
            case "reset"  -> { if (checkAdmin(sender)) cmdReset(sender); }
            case "set"    -> { if (checkAdmin(sender)) cmdSet(sender, args); }
            default       -> sendHelp(sender);
        }
        return true;
    }

    // ── Player commands ────────────────────────────────────────────────────────

    private void cmdJoin(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (game.getPhase() != BedWarsGame.GamePhase.LOBBY) {
            player.sendMessage(PREFIX + "§cThe game is already in progress.");
            return;
        }
        if (game.isInLobby(player.getUniqueId())) {
            player.sendMessage(PREFIX + "§cYou are already in the lobby.");
            return;
        }
        game.joinLobby(player);
        Bukkit.broadcastMessage(PREFIX + "§a" + player.getName() + " §7joined the lobby! ("
                + game.getLobbySize() + " players)");
    }

    private void cmdLeave(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        if (!game.isInLobby(player.getUniqueId())) {
            player.sendMessage(PREFIX + "§cYou are not in the lobby.");
            return;
        }
        game.leaveLobby(player);
        player.sendMessage(PREFIX + "§7You left the lobby.");
    }

    private void cmdLobby(CommandSender sender) {
        Set<UUID> lobby = game.getLobbyPlayers();
        sender.sendMessage("§5--- Bed Wars Lobby (" + lobby.size() + " players) ---");
        if (lobby.isEmpty()) {
            sender.sendMessage("§7  (empty)");
        } else {
            for (UUID uid : lobby) {
                Player p = Bukkit.getPlayer(uid);
                String name = p != null ? p.getName() : uid.toString();
                sender.sendMessage("§7  - " + name);
            }
        }
    }

    private void cmdStatus(CommandSender sender) {
        BedWarsGame.GamePhase phase = game.getPhase();
        sender.sendMessage("§5--- Bed Wars Status ---");
        sender.sendMessage("§7Phase: §e" + phase.name());
        if (phase == BedWarsGame.GamePhase.LOBBY) {
            sender.sendMessage("§7Lobby players: §e" + game.getLobbySize());
        } else {
            sender.sendMessage("§7Active teams: §e" + game.getAllIslands().size());
            long alive = game.getAllIslands().stream()
                    .filter(i -> i.getMembers().stream().anyMatch(uid2 -> !i.isEliminated(uid2)))
                    .count();
            sender.sendMessage("§7Alive teams: §e" + alive);
        }
    }

    // ── Admin commands ─────────────────────────────────────────────────────────

    private void cmdStart(CommandSender sender) {
        FactionManager fm = null;
        try {
            fm = SimpleFactionsPlugin.getInstance().getFactionManager();
        } catch (Exception ignored) { /* SimpleFactions not loaded */ }

        String err = game.startGame(fm);
        if (err != null) {
            sender.sendMessage(PREFIX + "§c" + err);
            return;
        }
        npcManager.spawnAll();
        Bukkit.broadcastMessage(PREFIX + "§aThe game has begun! Good luck!");
        sender.sendMessage(PREFIX + "§aGame started successfully.");
    }

    private void cmdEnd(CommandSender sender) {
        game.stop();
        npcManager.removeAll();
        sender.sendMessage(PREFIX + "§cGame ended.");
    }

    private void cmdReset(CommandSender sender) {
        npcManager.removeAll();
        game.reset();
        sender.sendMessage(PREFIX + "§aGame reset. Players may now join the lobby.");
    }

    // ── /bw set … ─────────────────────────────────────────────────────────────

    private void cmdSet(CommandSender sender, String[] args) {
        if (args.length < 2) { sendSetHelp(sender); return; }
        Player player = requirePlayer(sender);
        if (player == null) return;

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "teamspawn"     -> setTeamLocation(player, args, 2,
                    (t, loc) -> locationStore.setSpawn(t, loc), "spawn");
            case "teamgenerator" -> setTeamLocation(player, args, 2,
                    (t, loc) -> locationStore.setGenerator(t, loc), "island generator");
            case "teamshop"      -> setTeamLocationAndNpc(player, args, 2, "shop");
            case "teamupgrade"   -> setTeamLocationAndNpc(player, args, 2, "upgrade");
            case "teambed"       -> setTeamLocation(player, args, 2,
                    (t, loc) -> locationStore.setBed(t, loc), "bed");
            case "teamchest"     -> setTeamLocation(player, args, 2,
                    (t, loc) -> locationStore.setChest(t, loc), "team chest");
            case "diamond"       -> {
                locationStore.setDiamond(player.getLocation());
                player.sendMessage(PREFIX + "§bCentral diamond generator location saved.");
            }
            case "emerald"       -> {
                locationStore.setEmerald(player.getLocation());
                player.sendMessage(PREFIX + "§aCentral emerald generator location saved.");
            }
            default -> sendSetHelp(sender);
        }
    }

    @FunctionalInterface
    private interface TeamLocationSetter {
        void set(BedWarsTeam team, org.bukkit.Location loc);
    }

    private void setTeamLocation(Player player, String[] args, int colourIdx,
                                 TeamLocationSetter setter, String label) {
        BedWarsTeam team = parseTeam(player, args, colourIdx);
        if (team == null) return;
        setter.set(team, player.getLocation());
        player.sendMessage(PREFIX + team.getColor() + team.getDisplayName()
                + " §r" + label + " location saved.");
    }

    private void setTeamLocationAndNpc(Player player, String[] args, int colourIdx, String role) {
        BedWarsTeam team = parseTeam(player, args, colourIdx);
        if (team == null) return;

        if (role.equals("shop")) {
            locationStore.setShop(team, player.getLocation());
            BedWarsIsland island = game.getIsland(team);
            if (island != null) island.setShopNpcLocation(player.getLocation());
            npcManager.spawnShopNpc(team, player.getLocation());
            player.sendMessage(PREFIX + team.getColor() + team.getDisplayName()
                    + " §rshop NPC location saved and NPC spawned.");
        } else {
            locationStore.setUpgrader(team, player.getLocation());
            BedWarsIsland island = game.getIsland(team);
            if (island != null) island.setUpgraderNpcLocation(player.getLocation());
            npcManager.spawnUpgraderNpc(team, player.getLocation());
            player.sendMessage(PREFIX + team.getColor() + team.getDisplayName()
                    + " §rupgrader NPC location saved and NPC spawned.");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean checkAdmin(CommandSender sender) {
        if (sender.hasPermission(ADMIN)) return true;
        sender.sendMessage(PREFIX + "§cNo permission.");
        return false;
    }

    private Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "§cThis command must be run by a player.");
            return null;
        }
        return player;
    }

    private BedWarsTeam parseTeam(CommandSender sender, String[] args, int idx) {
        if (args.length <= idx) {
            sender.sendMessage(PREFIX + "§cSpecify a colour: "
                    + Arrays.stream(BedWarsTeam.values()).map(BedWarsTeam::name)
                            .collect(Collectors.joining(", ")));
            return null;
        }
        try {
            return BedWarsTeam.valueOf(args[idx].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(PREFIX + "§cInvalid colour '" + args[idx] + "'. Valid: "
                    + Arrays.stream(BedWarsTeam.values()).map(BedWarsTeam::name)
                            .collect(Collectors.joining(", ")));
            return null;
        }
    }

    private void sendHelp(CommandSender sender) {
        boolean admin = sender.hasPermission(ADMIN);
        sender.sendMessage("§5--- Bed Wars Commands ---");
        sender.sendMessage("§e/bw join         §7- Join the lobby");
        sender.sendMessage("§e/bw leave        §7- Leave the lobby");
        sender.sendMessage("§e/bw lobby        §7- List lobby players");
        sender.sendMessage("§e/bw status       §7- Show game status");
        if (admin) {
            sender.sendMessage("§6/bw start              §7- Start the game");
            sender.sendMessage("§6/bw end                §7- End the game");
            sender.sendMessage("§6/bw reset              §7- Reset to lobby");
            sender.sendMessage("§6/bw set teamspawn    <colour>  §7- Set team spawn");
            sender.sendMessage("§6/bw set teamgenerator <colour> §7- Set island generator");
            sender.sendMessage("§6/bw set teamshop     <colour>  §7- Set & spawn shop NPC");
            sender.sendMessage("§6/bw set teamupgrade  <colour>  §7- Set & spawn upgrade NPC");
            sender.sendMessage("§6/bw set teambed      <colour>  §7- Set team bed");
            sender.sendMessage("§6/bw set teamchest    <colour>  §7- Set team chest");
            sender.sendMessage("§6/bw set diamond               §7- Set diamond generator");
            sender.sendMessage("§6/bw set emerald               §7- Set emerald generator");
        }
    }

    private void sendSetHelp(CommandSender sender) {
        sender.sendMessage(PREFIX + "§cUsage: /bw set <teamspawn|teamgenerator|teamshop|"
                + "teamupgrade|teambed|teamchest|diamond|emerald> [colour]");
    }

    // ── Tab completion ─────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean admin = sender.hasPermission(ADMIN);

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("join", "leave", "lobby", "status"));
            if (admin) subs.addAll(Arrays.asList("start", "end", "reset", "set"));
            return filter(args[0], subs);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set") && admin) {
            return filter(args[1], Arrays.asList("teamspawn", "teamgenerator", "teamshop",
                    "teamupgrade", "teambed", "teamchest", "diamond", "emerald"));
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set") && admin) {
            if (args[1].toLowerCase().startsWith("team")) {
                return filter(args[2], Arrays.stream(BedWarsTeam.values())
                        .map(t -> t.name().toLowerCase()).collect(Collectors.toList()));
            }
        }

        return Collections.emptyList();
    }

    private List<String> filter(String prefix, List<String> options) {
        String low = prefix.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(low)).collect(Collectors.toList());
    }
}
