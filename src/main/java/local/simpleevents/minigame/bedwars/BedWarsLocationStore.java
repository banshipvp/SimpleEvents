package local.simpleevents.minigame.bedwars;

import local.simpleevents.SimpleEventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Persists per-colour Bed Wars locations (spawn, generator, shop NPC,
 * upgrader NPC, bed, chest) plus the two central generator locations
 * to the plugin's config.yml under the {@code bedwars.colors.<COLOR>.*}
 * and {@code bedwars.diamond / bedwars.emerald} paths.
 *
 * Locations are loaded on construction and saved atomically after every set.
 */
public class BedWarsLocationStore {

    private static final String ROOT = "bedwars.colors.";

    private final SimpleEventsPlugin plugin;

    /** Inner key → Location per team colour. */
    private final Map<BedWarsTeam, Map<String, Location>> colorLocs = new EnumMap<>(BedWarsTeam.class);

    private Location diamondLoc;
    private Location emeraldLoc;

    public BedWarsLocationStore(SimpleEventsPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setSpawn(BedWarsTeam t, Location l)     { put(t, "spawn", l); }
    public void setGenerator(BedWarsTeam t, Location l) { put(t, "generator", l); }
    public void setShop(BedWarsTeam t, Location l)      { put(t, "shop", l); }
    public void setUpgrader(BedWarsTeam t, Location l)  { put(t, "upgrader", l); }
    public void setBed(BedWarsTeam t, Location l)       { put(t, "bed", l); }
    public void setChest(BedWarsTeam t, Location l)     { put(t, "chest", l); }

    public void setDiamond(Location l) { this.diamondLoc = l; save(); }
    public void setEmerald(Location l) { this.emeraldLoc = l; save(); }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Location getSpawn(BedWarsTeam t)     { return get(t, "spawn"); }
    public Location getGenerator(BedWarsTeam t) { return get(t, "generator"); }
    public Location getShop(BedWarsTeam t)      { return get(t, "shop"); }
    public Location getUpgrader(BedWarsTeam t)  { return get(t, "upgrader"); }
    public Location getBed(BedWarsTeam t)       { return get(t, "bed"); }
    public Location getChest(BedWarsTeam t)     { return get(t, "chest"); }
    public Location getDiamond()                { return diamondLoc; }
    public Location getEmerald()                { return emeraldLoc; }

    /**
     * A colour is "configured" if at least a spawn location has been set —
     * the minimum needed to be usable as a team slot.
     */
    public boolean isConfigured(BedWarsTeam t) { return getSpawn(t) != null; }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void put(BedWarsTeam t, String key, Location l) {
        colorLocs.computeIfAbsent(t, k -> new HashMap<>()).put(key, l);
        save();
    }

    private Location get(BedWarsTeam t, String key) {
        Map<String, Location> m = colorLocs.get(t);
        return m == null ? null : m.get(key);
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    public void save() {
        for (Map.Entry<BedWarsTeam, Map<String, Location>> e : colorLocs.entrySet()) {
            String base = ROOT + e.getKey().name();
            for (Map.Entry<String, Location> le : e.getValue().entrySet()) {
                writeLoc(base + "." + le.getKey(), le.getValue());
            }
        }
        if (diamondLoc != null) writeLoc("bedwars.diamond", diamondLoc);
        if (emeraldLoc != null) writeLoc("bedwars.emerald", emeraldLoc);
        plugin.saveConfig();
    }

    private void load() {
        ConfigurationSection colors = plugin.getConfig().getConfigurationSection("bedwars.colors");
        if (colors != null) {
            for (String teamName : colors.getKeys(false)) {
                BedWarsTeam team;
                try { team = BedWarsTeam.valueOf(teamName); }
                catch (IllegalArgumentException e) { continue; }

                ConfigurationSection ts = colors.getConfigurationSection(teamName);
                if (ts == null) continue;
                for (String key : ts.getKeys(false)) {
                    Location loc = readLoc(ts.getConfigurationSection(key));
                    if (loc != null) {
                        colorLocs.computeIfAbsent(team, k -> new HashMap<>()).put(key, loc);
                    }
                }
            }
        }
        diamondLoc = readLoc(plugin.getConfig().getConfigurationSection("bedwars.diamond"));
        emeraldLoc = readLoc(plugin.getConfig().getConfigurationSection("bedwars.emerald"));
    }

    private void writeLoc(String path, Location loc) {
        plugin.getConfig().set(path + ".world",  loc.getWorld().getName());
        plugin.getConfig().set(path + ".x",      loc.getX());
        plugin.getConfig().set(path + ".y",      loc.getY());
        plugin.getConfig().set(path + ".z",      loc.getZ());
        plugin.getConfig().set(path + ".yaw",    (double) loc.getYaw());
        plugin.getConfig().set(path + ".pitch",  (double) loc.getPitch());
    }

    private Location readLoc(ConfigurationSection s) {
        if (s == null) return null;
        String wn = s.getString("world");
        if (wn == null) return null;
        World w = Bukkit.getWorld(wn);
        if (w == null) return null;
        return new Location(w,
                s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
    }
}
