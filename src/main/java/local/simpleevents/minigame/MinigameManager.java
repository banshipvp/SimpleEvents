package local.simpleevents.minigame;

import local.simpleevents.SimpleEventsPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Central manager for all minigame sessions and the token economy.
 *
 * - One MinigameSession per MinigameType is maintained at all times.
 * - Sessions cycle automatically between WAITING → STARTING → IN_PROGRESS → ENDED → WAITING.
 * - Tokens are persisted in config.yml under minigames.tokens.<uuid>.
 */
public class MinigameManager {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final String TOKEN_PATH = "minigames.tokens";
    private static final String LOC_PATH   = "minigames.locations";

    // Default countdown seconds before a match begins
    static final int COUNTDOWN_SECONDS = 30;
    // Default waiting period (seconds) before the next match waiting room opens — 30 minutes
    static final int NEXT_MATCH_WAIT_SECONDS = 1800;
    // Maximum multiplier value
    static final double MAX_MULTIPLIER = 3.0;

    private final SimpleEventsPlugin plugin;
    private final Map<MinigameType, MinigameSession> sessions = new EnumMap<>(MinigameType.class);

    public MinigameManager(SimpleEventsPlugin plugin) {
        this.plugin = plugin;
        // Initialise a WAITING session for each type
        for (MinigameType type : MinigameType.values()) {
            createNewSession(type);
        }
    }

    // ── Session lifecycle ──────────────────────────────────────────────────────

    private void createNewSession(MinigameType type) {
        String id = type.getKey() + "-" + System.currentTimeMillis();
        MinigameSession session = new MinigameSession(plugin, type, id);
        // Schedule the next match start time (NEXT_MATCH_WAIT_SECONDS from now)
        session.setNextMatchMillis(System.currentTimeMillis() + NEXT_MATCH_WAIT_SECONDS * 1000L);
        sessions.put(type, session);
        scheduleAutoStart(session);
    }

    /**
     * Schedules a repeating 1-second countdown that begins automatically when
     * the waiting time expires.  If not enough players join, the multiplier is
     * bumped and a new session is created.
     */
    private void scheduleAutoStart(MinigameSession session) {
        int[] secondsLeft = {NEXT_MATCH_WAIT_SECONDS};

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (session.getState() == MinigameState.ENDED) {
                Bukkit.getScheduler().cancelTask(session.getCountdownTaskId());
                return;
            }

            secondsLeft[0]--;
            if (secondsLeft[0] <= 0) {
                Bukkit.getScheduler().cancelTask(session.getCountdownTaskId());
                startCountdown(session);
            }
        }, 20L, 20L);

        session.setCountdownTaskId(taskId);
    }

    /**
     * Starts the 30-second countdown from WAITING → IN_PROGRESS.
     * If the game has no participants at the end, the multiplier increases and
     * a new session replaces this one.
     */
    public void startCountdown(MinigameSession session) {
        if (session.getState() != MinigameState.WAITING) return;
        session.setState(MinigameState.STARTING);
        session.setSecondsRemaining(COUNTDOWN_SECONDS);

        int[] timeLeft = {COUNTDOWN_SECONDS};

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            timeLeft[0]--;

            if (timeLeft[0] % 10 == 0 || timeLeft[0] <= 5) {
                broadcastToSession(session, "§e[" + LEGACY.serialize(
                        LEGACY.deserialize(session.getType().getDisplayName()))
                        + "§e] §7Match starting in §e" + timeLeft[0] + "§7 second(s)!");
            }

            if (timeLeft[0] <= 0) {
                Bukkit.getScheduler().cancelTask(session.getCountdownTaskId());
                int minPlayers = 4;
                if (session.getParticipantCount() < minPlayers) {
                    // Not enough players – bump multiplier, reset
                    double newMult = Math.min(session.getPointMultiplier() + 0.5, MAX_MULTIPLIER);
                    session.setState(MinigameState.ENDED);
                    if (session.getParticipantCount() == 0) {
                        plugin.getLogger().info("[Minigames] No players for "
                                + session.getType().getKey() + ", bumping multiplier to " + newMult);
                    } else {
                        broadcastToSession(session, "§c[Minigames] Not enough players to start ("
                                + session.getParticipantCount() + "/" + minPlayers + " needed). Resetting...");
                        plugin.getLogger().info("[Minigames] Not enough players for "
                                + session.getType().getKey() + ", bumping multiplier to " + newMult);
                    }
                    MinigameSession next = newSessionFor(session.getType());
                    next.setPointMultiplier(newMult);
                } else {
                    session.setState(MinigameState.IN_PROGRESS);
                    Location gameSpawn = getSpawnLocation(session.getType());
                    if (gameSpawn != null) {
                        for (UUID uid : session.getParticipantIds()) {
                            Player p = Bukkit.getPlayer(uid);
                            if (p != null) p.teleport(gameSpawn);
                        }
                    }
                    broadcastToSession(session, "§a[Minigames] §lThe match has begun! §aGood luck!");
                }
            }
        }, 20L, 20L);

        session.setCountdownTaskId(taskId);
    }

    /**
     * Ends the given session, awards tokens, and creates a new WAITING session.
     *
     * @param session  the session to end
     * @param ranking  ordered list of UUIDs (1st = index 0) for token distribution.
     *                 Pass an empty list if there are no winners (e.g. server restart).
     */
    public void endSession(MinigameSession session, List<UUID> ranking) {
        if (session.getState() == MinigameState.ENDED) return;
        session.setState(MinigameState.ENDED);

        // Cancel any running countdown
        if (session.getCountdownTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(session.getCountdownTaskId());
        }

        MinigameType type = session.getType();
        double mult = session.getPointMultiplier();

        // Award tokens to each placed participant
        for (int i = 0; i < ranking.size(); i++) {
            UUID uid = ranking.get(i);
            int place = i + 1;
            int base  = type.getTokensForPlacement(place);
            int awarded = (int) Math.round(base * mult);
            if (awarded > 0) {
                addTokens(uid, awarded);
                Player p = Bukkit.getPlayer(uid);
                if (p != null) {
                    p.sendMessage("§6[Minigames] §eYou finished §6#" + place
                            + " §eand earned §6" + awarded + " §eMinigame Token"
                            + (awarded == 1 ? "" : "s") + "§e!"
                            + (mult > 1.0 ? " §7(§a" + mult + "x §7multiplier)" : ""));
                }
            }
        }

        // Announce in chat to all online players
        if (!ranking.isEmpty()) {
            UUID winner = ranking.get(0);
            String winnerName = Optional.ofNullable(Bukkit.getPlayer(winner))
                    .map(Player::getName)
                    .orElse("Unknown");
            Bukkit.broadcastMessage("§6[Minigames] §e" + winnerName
                    + " §7won §6" + LEGACY.serialize(LEGACY.deserialize(type.getDisplayName()))
                    + "§7! Congratulations!");
        }

        // Queue next session
        newSessionFor(type);
    }

    private MinigameSession newSessionFor(MinigameType type) {
        String id = type.getKey() + "-" + System.currentTimeMillis();
        MinigameSession session = new MinigameSession(plugin, type, id);
        session.setNextMatchMillis(System.currentTimeMillis() + NEXT_MATCH_WAIT_SECONDS * 1000L);
        sessions.put(type, session);
        scheduleAutoStart(session);
        return session;
    }

    // ── Queries ────────────────────────────────────────────────────────────────

    public MinigameSession getSession(MinigameType type) { return sessions.get(type); }

    /** Returns the session a player is currently in, or null. */
    public MinigameSession getPlayerSession(Player player) {
        for (MinigameSession s : sessions.values()) {
            if (s.hasParticipant(player)) return s;
        }
        return null;
    }

    // ── Admin controls ─────────────────────────────────────────────────────────

    /**
     * Forces a session to start immediately (admin command).
     * If it is WAITING, begins the countdown at once.
     */
    public boolean forceStart(MinigameType type) {
        MinigameSession session = sessions.get(type);
        if (session == null || session.getState() != MinigameState.WAITING) return false;
        if (session.getCountdownTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(session.getCountdownTaskId());
        }
        startCountdown(session);
        return true;
    }

    /**
     * Forces the current session to end with the current ranking derived from points.
     */
    public boolean forceEnd(MinigameType type) {
        MinigameSession session = sessions.get(type);
        if (session == null || session.getState() == MinigameState.ENDED) return false;
        endSession(session, session.getRanking());
        return true;
    }

    /** Adds a player to the current WAITING/STARTING session for the given type. */
    public boolean joinSession(Player player, MinigameType type) {
        MinigameSession existing = getPlayerSession(player);
        if (existing != null) {
            player.sendMessage("§c[Minigames] You are already in a session. Use /minigame leave to exit.");
            return false;
        }
        MinigameSession session = sessions.get(type);
        if (session == null || (session.getState() != MinigameState.WAITING
                && session.getState() != MinigameState.STARTING)) {
            player.sendMessage("§c[Minigames] That minigame is currently in progress or unavailable.");
            return false;
        }
        Location lobby = getSpawnLocation(type);
        if (lobby == null) {
            player.sendMessage("§c[Minigames] The spawn/lobby for this minigame has not been set. Please contact an admin.");
            return false;
        }
        boolean added = session.addParticipant(player);
        if (added) {
            player.sendMessage("§a[Minigames] §7You joined §a"
                    + LEGACY.serialize(LEGACY.deserialize(type.getDisplayName()))
                    + "§7! Players: §a" + session.getParticipantCount());
            player.teleport(lobby);
        }
        return added;
    }

    /** Removes the player from whatever session they are in. */
    public boolean leaveSession(Player player) {
        MinigameSession session = getPlayerSession(player);
        if (session == null) return false;
        session.removeParticipant(player);
        player.sendMessage("§c[Minigames] You left the session.");
        return true;
    }

    // ── Spawn location ─────────────────────────────────────────────────────────

    public void setSpawnLocation(MinigameType type, Location loc) {
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

    public Location getSpawnLocation(MinigameType type) {
        String path = LOC_PATH + "." + type.getKey();
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.contains(path + ".world")) return null;
        String worldName = cfg.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        double x     = cfg.getDouble(path + ".x");
        double y     = cfg.getDouble(path + ".y");
        double z     = cfg.getDouble(path + ".z");
        float  yaw   = (float) cfg.getDouble(path + ".yaw");
        float  pitch = (float) cfg.getDouble(path + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    // ── Token economy ──────────────────────────────────────────────────────────

    public int getTokens(UUID playerId) {
        return plugin.getConfig().getInt(TOKEN_PATH + "." + playerId, 0);
    }

    public void addTokens(UUID playerId, int amount) {
        int current = getTokens(playerId);
        plugin.getConfig().set(TOKEN_PATH + "." + playerId, current + amount);
        plugin.saveConfig();
    }

    public boolean spendTokens(UUID playerId, int amount) {
        int current = getTokens(playerId);
        if (current < amount) return false;
        plugin.getConfig().set(TOKEN_PATH + "." + playerId, current - amount);
        plugin.saveConfig();
        return true;
    }

    public void setTokens(UUID playerId, int amount) {
        plugin.getConfig().set(TOKEN_PATH + "." + playerId, Math.max(0, amount));
        plugin.saveConfig();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void broadcastToSession(MinigameSession session, String msg) {
        for (UUID uid : session.getParticipantIds()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) p.sendMessage(msg);
        }
    }

    /** Called on plugin disable — cancel all scheduler tasks. */
    public void shutdown() {
        for (MinigameSession s : sessions.values()) {
            if (s.getCountdownTaskId() != -1) {
                Bukkit.getScheduler().cancelTask(s.getCountdownTaskId());
            }
        }
    }
}
