package local.simpleevents.minigame.bedwars;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Holds the per-team state for a Bed Wars game:
 *  - member list
 *  - bed alive/destroyed
 *  - island spawn, bed block, generator location, chest location
 *  - generator upgrade tier
 *  - team upgrades purchased
 *  - trap status
 */
public class BedWarsIsland {

    // ── Team info ─────────────────────────────────────────────────────────────
    private final BedWarsTeam team;
    private final Set<UUID> members        = new LinkedHashSet<>();
    private final Set<UUID> eliminated     = new HashSet<>(); // bed gone + dead
    private boolean bedAlive               = true;

    // ── Locations ─────────────────────────────────────────────────────────────
    private Location spawnLocation;
    private Location bedLocation;        // one block of the bed (head or foot)
    private Location islandGeneratorLoc; // island iron/gold generator
    private Location teamChestLocation;
    private Location shopNpcLocation;
    private Location upgraderNpcLocation;

    // ── Generator ─────────────────────────────────────────────────────────────
    private GeneratorTier generatorTier = GeneratorTier.TIER_0;

    // ── Team upgrades ─────────────────────────────────────────────────────────
    private int sharpnessLevel     = 0; // 1 = Sharpness I
    private int protectionLevel    = 0; // 1 = Protection I … up to 5
    private int efficiencyLevel    = 0;
    private boolean hasteActive    = false;
    private boolean hasRegenOnKill = false;
    private boolean hasIslandRegen = false;
    // 0 = none, 1 = blindness only, 2 = blindness+fatigue
    private int trapLevel          = 0;
    private boolean trapReady      = true; // false while on cooldown after use

    // ── Scheduler task IDs ────────────────────────────────────────────────────
    private int ironTaskId         = -1;
    private int goldTaskId         = -1;
    private int diamondTaskId      = -1;
    private int emeraldTaskId      = -1;

    // ── Upgrade cost definitions ──────────────────────────────────────────────
    // Generator tier costs: index = next tier index (1,2,3)
    public static final int[] GEN_UPGRADE_COST_IRON     = {0, 15, 0,  0};  // iron required
    public static final int[] GEN_UPGRADE_COST_GOLD     = {0,  0, 8,  0};  // gold required
    public static final int[] GEN_UPGRADE_COST_DIAMOND  = {0,  0, 0,  4};  // diamond required

    public BedWarsIsland(BedWarsTeam team) {
        this.team = team;
    }

    // ── Members ───────────────────────────────────────────────────────────────

    public void addMember(UUID id)          { members.add(id); }
    public void removeMember(UUID id)       { members.remove(id); }
    public boolean isMember(UUID id)        { return members.contains(id); }
    public Set<UUID> getMembers()           { return Collections.unmodifiableSet(members); }
    public int getMemberCount()             { return members.size(); }

    public void markEliminated(UUID id)     { eliminated.add(id); }
    public boolean isEliminated(UUID id)    { return eliminated.contains(id); }

    /** All members are eliminated (bed gone AND all dead). */
    public boolean isFullyEliminated() {
        if (bedAlive) return false;
        return eliminated.containsAll(members);
    }

    // ── Bed ───────────────────────────────────────────────────────────────────

    public boolean isBedAlive()             { return bedAlive; }
    public void setBedDestroyed()           { bedAlive = false; }

    // ── Locations ─────────────────────────────────────────────────────────────

    public Location getSpawnLocation()          { return spawnLocation; }
    public void setSpawnLocation(Location l)    { this.spawnLocation = l; }

    public Location getBedLocation()            { return bedLocation; }
    public void setBedLocation(Location l)      { this.bedLocation = l; }

    public Location getIslandGeneratorLoc()           { return islandGeneratorLoc; }
    public void setIslandGeneratorLoc(Location l)     { this.islandGeneratorLoc = l; }

    public Location getTeamChestLocation()            { return teamChestLocation; }
    public void setTeamChestLocation(Location l)      { this.teamChestLocation = l; }

    public Location getShopNpcLocation()              { return shopNpcLocation; }
    public void setShopNpcLocation(Location l)        { this.shopNpcLocation = l; }

    public Location getUpgraderNpcLocation()          { return upgraderNpcLocation; }
    public void setUpgraderNpcLocation(Location l)    { this.upgraderNpcLocation = l; }

    // ── Generator ─────────────────────────────────────────────────────────────

    public GeneratorTier getGeneratorTier()           { return generatorTier; }
    public void setGeneratorTier(GeneratorTier t)     { this.generatorTier = t; }

    public int getIronTaskId()                        { return ironTaskId; }
    public void setIronTaskId(int id)                 { this.ironTaskId = id; }

    public int getGoldTaskId()                        { return goldTaskId; }
    public void setGoldTaskId(int id)                 { this.goldTaskId = id; }

    public int getDiamondTaskId()                     { return diamondTaskId; }
    public void setDiamondTaskId(int id)              { this.diamondTaskId = id; }

    public int getEmeraldTaskId()                     { return emeraldTaskId; }
    public void setEmeraldTaskId(int id)              { this.emeraldTaskId = id; }

    // ── Team upgrades ─────────────────────────────────────────────────────────

    public int getSharpnessLevel()                    { return sharpnessLevel; }
    public void setSharpnessLevel(int l)              { this.sharpnessLevel = l; }

    public int getProtectionLevel()                   { return protectionLevel; }
    public void setProtectionLevel(int l)             { this.protectionLevel = l; }

    public int getEfficiencyLevel()                   { return efficiencyLevel; }
    public void setEfficiencyLevel(int l)             { this.efficiencyLevel = l; }

    public boolean hasHaste()                         { return hasteActive; }
    public void setHaste(boolean v)                   { this.hasteActive = v; }

    public boolean hasRegenOnKill()                   { return hasRegenOnKill; }
    public void setRegenOnKill(boolean v)             { this.hasRegenOnKill = v; }

    public boolean hasIslandRegen()                   { return hasIslandRegen; }
    public void setIslandRegen(boolean v)             { this.hasIslandRegen = v; }

    public int getTrapLevel()                         { return trapLevel; }
    public void setTrapLevel(int l)                   { this.trapLevel = l; }

    public boolean isTrapReady()                      { return trapReady && trapLevel > 0; }
    public void setTrapReady(boolean v)               { this.trapReady = v; }

    /**
     * Fires the trap on a nearby enemy.  Applies effects and degrades trap level.
     */
    public void fireTrap(Player enemy) {
        if (!isTrapReady()) return;
        trapReady = false;

        // Blindness always
        enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 7, 2, false, true));

        if (trapLevel == 2) {
            enemy.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 20 * 7, 2, false, true));
        }

        // Degrade: level 2 → level 1 (needs re-purchase of level 2); level 1 → gone
        trapLevel = Math.max(0, trapLevel - 1);
        trapReady = trapLevel > 0;
    }

    // ── Getters for team ──────────────────────────────────────────────────────

    public BedWarsTeam getTeam()                      { return team; }
}
