package local.simpleevents.minigame;

import local.simpleevents.SimpleEventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Tracks one active minigame match.
 *
 * A session begins in WAITING state. Players can join up to the configured
 * max-players.  When the admin triggers the start (or enough players queue up),
 * the state advances to STARTING then IN_PROGRESS.  When a winner is declared
 * the session moves to ENDED and the MinigameManager removes it.
 */
public class MinigameSession {

    private final SimpleEventsPlugin plugin;
    private final MinigameType type;
    private final String id; // unique ID for this session

    private MinigameState state = MinigameState.WAITING;

    /** All players currently participating (UUID → display name snapshot). */
    private final Map<UUID, String> participants = new LinkedHashMap<>();

    /** Tracks points earned in-game per player (for leaderboard). */
    private final Map<UUID, Integer> points = new HashMap<>();

    /** Countdown task ID, -1 if not running. */
    private int countdownTaskId = -1;
    private int secondsRemaining = 0;

    /** Multiplier that increases by 0.5 each time the game fails to start; resets to 1.0 when it does. */
    private double pointMultiplier = 1.0;

    private long nextMatchMillis = -1;

    public MinigameSession(SimpleEventsPlugin plugin, MinigameType type, String id) {
        this.plugin = plugin;
        this.type = type;
        this.id = id;
    }

    // ── Participants ───────────────────────────────────────────────────────────

    public boolean addParticipant(Player player) {
        if (state != MinigameState.WAITING && state != MinigameState.STARTING) return false;
        participants.put(player.getUniqueId(), player.getName());
        points.put(player.getUniqueId(), 0);
        return true;
    }

    public boolean removeParticipant(Player player) {
        boolean removed = participants.remove(player.getUniqueId()) != null;
        points.remove(player.getUniqueId());
        return removed;
    }

    public boolean hasParticipant(Player player) {
        return participants.containsKey(player.getUniqueId());
    }

    public Set<UUID> getParticipantIds() { return Collections.unmodifiableSet(participants.keySet()); }
    public int getParticipantCount()     { return participants.size(); }

    // ── Points ────────────────────────────────────────────────────────────────

    public void addPoints(UUID playerId, int amount) {
        points.merge(playerId, amount, Integer::sum);
    }

    public int getPoints(UUID playerId) {
        return points.getOrDefault(playerId, 0);
    }

    /**
     * Returns participants sorted by points descending.
     * Returns a new mutable list; safe to iterate even while the session lives.
     */
    public List<UUID> getRanking() {
        List<UUID> ranked = new ArrayList<>(participants.keySet());
        ranked.sort((a, b) -> Integer.compare(points.getOrDefault(b, 0), points.getOrDefault(a, 0)));
        return ranked;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    public MinigameState getState()    { return state; }
    public void setState(MinigameState s) { this.state = s; }

    public MinigameType getType()      { return type; }
    public String getId()              { return id; }

    public double getPointMultiplier()          { return pointMultiplier; }
    public void setPointMultiplier(double v)    { this.pointMultiplier = v; }

    public int getCountdownTaskId()             { return countdownTaskId; }
    public void setCountdownTaskId(int id)      { this.countdownTaskId = id; }

    public int getSecondsRemaining()            { return secondsRemaining; }
    public void setSecondsRemaining(int s)      { this.secondsRemaining = s; }

    public long getNextMatchMillis()            { return nextMatchMillis; }
    public void setNextMatchMillis(long ms)     { this.nextMatchMillis = ms; }

    /** Remaining seconds until next match (for display). -1 if unset. */
    public long getNextMatchSecondsRemaining() {
        if (nextMatchMillis < 0) return -1;
        return Math.max(0, (nextMatchMillis - System.currentTimeMillis()) / 1000L);
    }
}
