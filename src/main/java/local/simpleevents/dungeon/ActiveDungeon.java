package local.simpleevents.dungeon;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a live dungeon run — players currently inside a dungeon instance.
 */
public class ActiveDungeon {

    private final DungeonType type;
    private final boolean heroic;
    private final Set<UUID> participants = new HashSet<>();
    private final long startTime;
    private final List<DungeonDebuff> activeDebuffs;

    public ActiveDungeon(DungeonType type, boolean heroic, List<DungeonDebuff> debuffs) {
        this.type = type;
        this.heroic = heroic;
        this.startTime = System.currentTimeMillis();
        this.activeDebuffs = Collections.unmodifiableList(debuffs);
    }

    public void addParticipant(Player player) {
        participants.add(player.getUniqueId());
    }

    public void removeParticipant(Player player) {
        participants.remove(player.getUniqueId());
    }

    public boolean hasParticipant(Player player) {
        return participants.contains(player.getUniqueId());
    }

    public int getParticipantCount() { return participants.size(); }
    public Set<UUID> getParticipants() { return Collections.unmodifiableSet(participants); }
    public DungeonType getType() { return type; }
    public boolean isHeroic() { return heroic; }
    public long getStartTime() { return startTime; }
    public List<DungeonDebuff> getActiveDebuffs() { return activeDebuffs; }
}
