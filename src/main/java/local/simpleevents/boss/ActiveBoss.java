package local.simpleevents.boss;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

/**
 * Represents a currently-spawned warzone boss.
 */
public class ActiveBoss {

    private final UUID entityUUID;
    private final BossType type;
    private final Location spawnLocation;
    private final long spawnTime;

    public ActiveBoss(LivingEntity entity, BossType type) {
        this.entityUUID = entity.getUniqueId();
        this.type = type;
        this.spawnLocation = entity.getLocation().clone();
        this.spawnTime = System.currentTimeMillis();
    }

    public UUID getEntityUUID() { return entityUUID; }
    public BossType getType() { return type; }
    public Location getSpawnLocation() { return spawnLocation; }
    public long getSpawnTime() { return spawnTime; }
}
