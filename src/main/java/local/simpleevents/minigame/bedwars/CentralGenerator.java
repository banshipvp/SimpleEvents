package local.simpleevents.minigame.bedwars;

import local.simpleevents.SimpleEventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Central diamond and emerald generators — shared by all teams.
 *
 * Diamond: every 30 seconds (speeds up to 15s at 5 minutes)
 * Emerald: every 60 seconds (speeds up to 30s at 10 minutes)
 */
public class CentralGenerator {

    private final SimpleEventsPlugin plugin;
    private Location diamondLocation;
    private Location emeraldLocation;

    private int diamondTaskId  = -1;
    private int emeraldTaskId  = -1;
    private int timerTaskId    = -1;  // tracks elapsed seconds for speed-ups

    // Phase flags
    private boolean diamondSpedup  = false;
    private boolean emeraldSpedup  = false;

    private int elapsedSeconds = 0;

    public CentralGenerator(SimpleEventsPlugin plugin, Location diamondLoc, Location emeraldLoc) {
        this.plugin          = plugin;
        this.diamondLocation = diamondLoc;
        this.emeraldLocation = emeraldLoc;
    }

    public void start() {
        // Game-clock – tracks seconds to trigger speed-ups
        timerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            elapsedSeconds++;

            // 5 minutes → diamond speeds up
            if (!diamondSpedup && elapsedSeconds >= 300) {
                diamondSpedup = true;
                restartDiamond(15);
                Bukkit.broadcastMessage("§b[Bed Wars] §7Diamond generators are now faster! (15s)");
            }

            // 10 minutes → emerald speeds up
            if (!emeraldSpedup && elapsedSeconds >= 600) {
                emeraldSpedup = true;
                restartEmerald(30);
                Bukkit.broadcastMessage("§a[Bed Wars] §7Emerald generators are now faster! (30s)");
            }
        }, 20L, 20L);

        restartDiamond(30);
        restartEmerald(60);
    }

    private void restartDiamond(int seconds) {
        if (diamondTaskId != -1) Bukkit.getScheduler().cancelTask(diamondTaskId);
        long period = seconds * 20L;
        diamondTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (diamondLocation != null && diamondLocation.getWorld() != null)
                dropAt(diamondLocation, Material.DIAMOND, 1);
        }, period, period);
    }

    private void restartEmerald(int seconds) {
        if (emeraldTaskId != -1) Bukkit.getScheduler().cancelTask(emeraldTaskId);
        long period = seconds * 20L;
        emeraldTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (emeraldLocation != null && emeraldLocation.getWorld() != null)
                dropAt(emeraldLocation, Material.EMERALD, 1);
        }, period, period);
    }

    public void stop() {
        if (diamondTaskId != -1) { Bukkit.getScheduler().cancelTask(diamondTaskId); diamondTaskId = -1; }
        if (emeraldTaskId != -1) { Bukkit.getScheduler().cancelTask(emeraldTaskId); emeraldTaskId = -1; }
        if (timerTaskId   != -1) { Bukkit.getScheduler().cancelTask(timerTaskId);   timerTaskId   = -1; }
    }

    private void dropAt(Location loc, Material mat, int amount) {
        Item item = loc.getWorld().dropItemNaturally(loc, new org.bukkit.inventory.ItemStack(mat, amount));
        item.setPickupDelay(0);
    }

    public void setDiamondLocation(Location l) { this.diamondLocation = l; }
    public void setEmeraldLocation(Location l) { this.emeraldLocation = l; }
    public Location getDiamondLocation()       { return diamondLocation; }
    public Location getEmeraldLocation()       { return emeraldLocation; }
    public int getElapsedSeconds()             { return elapsedSeconds; }
}
