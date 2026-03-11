package local.simpleevents.minigame.bedwars;

import local.simpleevents.SimpleEventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the island resource generator for one island.
 *
 * Resources are dropped as items at the generator location so players
 * can pick them up.  A separate set of scheduler tasks handles the
 * central diamond and emerald generators (shared across all islands).
 */
public class IslandGenerator {

    private final SimpleEventsPlugin plugin;
    private final BedWarsIsland island;
    private final List<Integer> activeTasks = new ArrayList<>();

    public IslandGenerator(SimpleEventsPlugin plugin, BedWarsIsland island) {
        this.plugin = plugin;
        this.island = island;
    }

    /** Cancels existing tasks and starts fresh ones for the current tier. */
    public void restart() {
        cancel();

        GeneratorTier tier = island.getGeneratorTier();
        Location loc = island.getIslandGeneratorLoc();
        if (loc == null) return;

        // Iron tick
        long ironPeriod = (long) tier.getIronTickSeconds() * 20L;
        int ironTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (loc.getWorld() == null) return;
            dropItem(loc, Material.IRON_INGOT, tier.getIronPerTick());
        }, ironPeriod, ironPeriod);
        activeTasks.add(ironTask);
        island.setIronTaskId(ironTask);

        // Gold tick (if enabled in this tier)
        if (tier.getGoldTickSeconds() > 0) {
            long goldPeriod = (long) tier.getGoldTickSeconds() * 20L;
            int goldTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (loc.getWorld() == null) return;
                dropItem(loc, Material.GOLD_INGOT, tier.getGoldPerTick());
                if (tier.getGoldExtra() > 0) dropItem(loc, Material.IRON_INGOT, tier.getGoldExtra());
            }, goldPeriod, goldPeriod);
            activeTasks.add(goldTask);
            island.setGoldTaskId(goldTask);
        } else {
            // Default tier: gold every 4 seconds, plus 1 iron
            long goldPeriod = 4 * 20L;
            int goldTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (loc.getWorld() == null) return;
                dropItem(loc, Material.GOLD_INGOT, 1);
                dropItem(loc, Material.IRON_INGOT, 1);
            }, goldPeriod, goldPeriod);
            activeTasks.add(goldTask);
            island.setGoldTaskId(goldTask);
        }

        // Diamond tick (tier 2+)
        if (tier.getDiamondTickSeconds() > 0) {
            long diamondPeriod = (long) tier.getDiamondTickSeconds() * 20L;
            int diamondTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (loc.getWorld() == null) return;
                dropItem(loc, Material.DIAMOND, 1);
            }, diamondPeriod, diamondPeriod);
            activeTasks.add(diamondTask);
            island.setDiamondTaskId(diamondTask);
        }

        // Emerald tick (tier 3 only)
        if (tier.getEmeraldTickSeconds() > 0) {
            long emeraldPeriod = (long) tier.getEmeraldTickSeconds() * 20L;
            int emeraldTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (loc.getWorld() == null) return;
                dropItem(loc, Material.EMERALD, tier.getEmeraldPerTick());
            }, emeraldPeriod, emeraldPeriod);
            activeTasks.add(emeraldTask);
            island.setEmeraldTaskId(emeraldTask);
        }
    }

    public void cancel() {
        for (int id : activeTasks) {
            Bukkit.getScheduler().cancelTask(id);
        }
        activeTasks.clear();
        island.setIronTaskId(-1);
        island.setGoldTaskId(-1);
        island.setDiamondTaskId(-1);
        island.setEmeraldTaskId(-1);
    }

    private void dropItem(Location loc, Material mat, int amount) {
        ItemStack stack = new ItemStack(mat, amount);
        Item dropped = loc.getWorld().dropItemNaturally(loc, stack);
        dropped.setPickupDelay(0);
    }
}
