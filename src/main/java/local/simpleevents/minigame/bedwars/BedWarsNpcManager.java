package local.simpleevents.minigame.bedwars;

import local.simpleevents.SimpleEventsPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages Bed Wars NPC shopkeepers and upgraders.
 *
 * NPCs are standard Villagers (not invincible).
 * They are tracked by UUID so they can be removed cleanly when the game ends.
 *
 * Roles:
 *   SHOP     — opens BedWarsShopGui
 *   UPGRADER — opens BedWarsUpgradesGui
 */
public class BedWarsNpcManager {

    public enum NpcRole { SHOP, UPGRADER }

    private record NpcEntry(UUID entityId, NpcRole role, BedWarsTeam team) {}

    private final SimpleEventsPlugin plugin;
    private final BedWarsGame game;

    /** entityId → NpcEntry */
    private final Map<UUID, NpcEntry> npcs = new HashMap<>();

    public BedWarsNpcManager(SimpleEventsPlugin plugin, BedWarsGame game) {
        this.plugin = plugin;
        this.game   = game;
    }

    // ── Spawn ──────────────────────────────────────────────────────────────────

    /**
     * Spawns a shop NPC at the configured location for the given team (reads from active island).
     * Does nothing if the island has no shop location set.
     */
    public void spawnShopNpc(BedWarsTeam team) {
        BedWarsIsland island = game.getIsland(team);
        if (island == null || island.getShopNpcLocation() == null) return;
        spawnNpc(island.getShopNpcLocation(), team, NpcRole.SHOP,
                team.getColor() + team.getDisplayName() + " §eItem Shop");
    }

    /**
     * Spawns a shop NPC at an explicit location (used by /bw set teamshop outside a live game).
     */
    public void spawnShopNpc(BedWarsTeam team, Location loc) {
        spawnNpc(loc, team, NpcRole.SHOP,
                team.getColor() + team.getDisplayName() + " §eItem Shop");
    }

    /**
     * Spawns an upgrader NPC at the configured location for the given team.
     */
    public void spawnUpgraderNpc(BedWarsTeam team) {
        BedWarsIsland island = game.getIsland(team);
        if (island == null || island.getUpgraderNpcLocation() == null) return;
        spawnNpc(island.getUpgraderNpcLocation(), team, NpcRole.UPGRADER,
                team.getColor() + team.getDisplayName() + " §5Team Upgrades");
    }

    /**
     * Spawns an upgrader NPC at an explicit location (used by /bw set teamupgrade outside a live game).
     */
    public void spawnUpgraderNpc(BedWarsTeam team, Location loc) {
        spawnNpc(loc, team, NpcRole.UPGRADER,
                team.getColor() + team.getDisplayName() + " §5Team Upgrades");
    }

    private void spawnNpc(Location loc, BedWarsTeam team, NpcRole role, String name) {
        Villager v = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        v.setCustomName(name);
        v.setCustomNameVisible(true);
        v.setAI(false);
        v.setInvulnerable(false); // NOT unkillable — standard behaviour
        v.setSilent(true);
        v.getPathfinder().stopPathfinding();

        NpcEntry entry = new NpcEntry(v.getUniqueId(), role, team);
        npcs.put(v.getUniqueId(), entry);
    }

    // ── Lookup ─────────────────────────────────────────────────────────────────

    /** Returns the role for the entity, or null if not a BW NPC. */
    public NpcRole getRoleOf(UUID entityId) {
        NpcEntry e = npcs.get(entityId);
        return e == null ? null : e.role();
    }

    /** Returns the team for the entity, or null. */
    public BedWarsTeam getTeamOf(UUID entityId) {
        NpcEntry e = npcs.get(entityId);
        return e == null ? null : e.team();
    }

    public boolean isNpc(UUID entityId) {
        return npcs.containsKey(entityId);
    }

    // ── Cleanup ────────────────────────────────────────────────────────────────

    /** Removes all spawned NPCs from the world. Call on game end. */
    public void removeAll() {
        for (UUID id : npcs.keySet()) {
            // Iterate all worlds to find the entity
            for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                for (Entity e : world.getLivingEntities()) {
                    if (e.getUniqueId().equals(id)) {
                        e.remove();
                        break;
                    }
                }
            }
        }
        npcs.clear();
    }

    // ── Spawn all ─────────────────────────────────────────────────────────────

    /** Convenience — spawns shop & upgrader NPCs for all active (assigned) teams. */
    public void spawnAll() {
        for (BedWarsIsland island : game.getAllIslands()) {
            spawnShopNpc(island.getTeam());
            spawnUpgraderNpc(island.getTeam());
        }
    }
}
