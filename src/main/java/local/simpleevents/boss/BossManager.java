package local.simpleevents.boss;

import local.simpleevents.SimpleEventsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages the warzone random boss — scheduling, spawning, boss-bar, and cleanup.
 */
public class BossManager {

    private static final String BOSS_METADATA_KEY = "simpleevents_boss";

    private final SimpleEventsPlugin plugin;
    private ActiveBoss activeBoss;
    private BossBar bossBar;
    private BukkitTask spawnTask;
    private BukkitTask countdownTask;
    private BukkitTask bossBarUpdateTask;

    /** Seconds until the next boss spawn. */
    private int secondsUntilNextSpawn;

    public BossManager(SimpleEventsPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    public void start() {
        int intervalSeconds = plugin.getConfig().getInt("bosses.spawn-interval-seconds", 3600);
        secondsUntilNextSpawn = intervalSeconds;

        // Countdown ticker — fires every second to update countdown and spawn when ready
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeBoss != null) return; // boss already active

            secondsUntilNextSpawn--;
            if (secondsUntilNextSpawn <= 0) {
                secondsUntilNextSpawn = plugin.getConfig().getInt("bosses.spawn-interval-seconds", 3600);
                spawnRandomBoss();
            }
        }, 20L, 20L); // every second

        // Boss-bar update task — fires every 10 ticks to update health display
        bossBarUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateBossBar, 10L, 10L);
    }

    public void stop() {
        if (countdownTask != null) countdownTask.cancel();
        if (bossBarUpdateTask != null) bossBarUpdateTask.cancel();
        if (spawnTask != null) spawnTask.cancel();
        cleanupBossBar();
        // Remove active boss entity if still alive
        if (activeBoss != null) {
            Entity entity = findBossEntity();
            if (entity != null) entity.remove();
            activeBoss = null;
        }
    }

    // ── Spawning ───────────────────────────────────────────────────────────────

    /**
     * Picks a random BossType and spawns it at a random warzone location.
     */
    public boolean spawnRandomBoss() {
        if (activeBoss != null) {
            plugin.getLogger().warning("[SimpleEvents] Cannot spawn boss — one is already active.");
            return false;
        }

        BossType[] types = BossType.values();
        BossType chosen = types[ThreadLocalRandom.current().nextInt(types.length)];
        return spawnBoss(chosen);
    }

    /**
     * Spawns the given boss type at a valid warzone location.
     */
    public boolean spawnBoss(BossType type) {
        Location loc = findWarzoneSpawnLocation();
        if (loc == null) {
            plugin.getLogger().warning("[SimpleEvents] Could not find a warzone spawn location for boss.");
            return false;
        }
        return spawnBossAt(type, loc);
    }

    /**
     * Spawns the given boss type at a specific location.
     */
    public boolean spawnBossAt(BossType type, Location loc) {
        if (activeBoss != null) return false;

        World world = loc.getWorld();
        if (world == null) return false;

        LivingEntity entity = (LivingEntity) world.spawnEntity(loc, type.getEntityType(),
                CreatureSpawnEvent.SpawnReason.CUSTOM);

        applyBossAttributes(entity, type);
        entity.setMetadata(BOSS_METADATA_KEY,
                new org.bukkit.metadata.FixedMetadataValue(plugin, type.getKey()));

        activeBoss = new ActiveBoss(entity, type);

        // Create / show boss bar
        createBossBar(type, entity.getHealth(), entity.getHealth());

        // Announce spawn
        broadcastBossSpawn(type, loc);

        plugin.getLogger().info("[SimpleEvents] Spawned boss: " + type.getDisplayName()
                + " at " + formatLocation(loc));
        return true;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void applyBossAttributes(LivingEntity entity, BossType type) {
        // Health
        AttributeInstance maxHealthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(type.getMaxHealth());
        }
        entity.setHealth(type.getMaxHealth());

        // Attack damage
        AttributeInstance damageAttr = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(type.getExtraDamage());
        }

        // Movement speed
        AttributeInstance speedAttr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(type.getMoveSpeed());
        }

        // Follow range — aggressively hunt players
        AttributeInstance followAttr = entity.getAttribute(Attribute.FOLLOW_RANGE);
        if (followAttr != null) {
            followAttr.setBaseValue(64.0);
        }

        // Custom name
        entity.customName(legacy(type.getDisplayName()));
        entity.setCustomNameVisible(true);
        entity.setRemoveWhenFarAway(false);

        // Special handling per entity type
        if (entity instanceof Slime slime) {
            slime.setSize(8);
        }
        if (entity instanceof Phantom phantom) {
            phantom.setSize(4);
        }
    }

    private Location findWarzoneSpawnLocation() {
        // Try to find a warzone chunk via SimpleFactions soft-depend
        org.bukkit.plugin.Plugin sfPlugin = Bukkit.getPluginManager().getPlugin("SimpleFactions");
        if (sfPlugin instanceof local.simplefactions.SimpleFactionsPlugin sf) {
            local.simplefactions.WarzoneManager wm = sf.getWarzoneManager();
            if (wm != null) {
                Location candidate = wm.getRandomWarzoneSpawnLocation();
                if (candidate != null) return candidate;
            }
        }

        // Fallback: use config-defined spawn center
        String worldName = plugin.getConfig().getString("bosses.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("[SimpleEvents] Boss world '" + worldName + "' not found.");
            return null;
        }
        double cx = plugin.getConfig().getDouble("bosses.center-x", 0);
        double cz = plugin.getConfig().getDouble("bosses.center-z", 0);
        int radius = plugin.getConfig().getInt("bosses.spawn-radius", 200);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 20; attempt++) {
            double x = cx + rng.nextDouble(-radius, radius);
            double z = cz + rng.nextDouble(-radius, radius);
            int y = world.getHighestBlockYAt((int) x, (int) z);
            Location loc = new Location(world, x, y + 1, z);
            if (!world.getBlockAt((int) x, y, (int) z).isLiquid()) {
                return loc;
            }
        }
        return null;
    }

    // ── Boss bar ───────────────────────────────────────────────────────────────

    private void createBossBar(BossType type, double currentHealth, double maxHealth) {
        cleanupBossBar();
        BarColor color = switch (type) {
            case VOID_WRAITH -> BarColor.PURPLE;
            case TITAN_SLIME -> BarColor.GREEN;
            case VENOM_HULK -> BarColor.GREEN;
            case ARACHNID_QUEEN -> BarColor.WHITE;
            case SPECTRAL_STALKER -> BarColor.WHITE;
            case ABYSS_HERALD -> BarColor.RED;
        };
        bossBar = Bukkit.createBossBar(
                ChatColor.stripColor(type.getDisplayName()) + " — Warzone Boss",
                color, BarStyle.SEGMENTED_10
        );
        bossBar.setProgress(Math.min(1.0, currentHealth / maxHealth));
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
    }

    private void updateBossBar() {
        if (bossBar == null || activeBoss == null) return;
        Entity entity = findBossEntity();
        if (entity instanceof LivingEntity le) {
            double max = le.getAttribute(Attribute.MAX_HEALTH) != null
                    ? le.getAttribute(Attribute.MAX_HEALTH).getValue()
                    : activeBoss.getType().getMaxHealth();
            double progress = Math.max(0.0, Math.min(1.0, le.getHealth() / max));
            bossBar.setProgress(progress);
            // Add any newly joined players
            Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
        }
    }

    private void cleanupBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    // ── Death handling ────────────────────────────────────────────────────────

    /**
     * Called by BossListener when a boss entity dies.
     */
    public void handleBossDeath(LivingEntity entity, Player killer) {
        if (activeBoss == null) return;
        if (entity != null && !entity.getUniqueId().equals(activeBoss.getEntityUUID())) return;

        BossType type = activeBoss.getType();

        // Rewards to killer
        if (killer != null) {
            rewardPlayer(killer, type);
        }

        // Announce death
        broadcastBossDeath(type, killer);

        // Drop loot at death location (only if we have an entity location)
        if (entity != null) {
            spawnLoot(entity.getLocation(), type);
        } else {
            spawnLoot(activeBoss.getSpawnLocation(), type);
        }

        cleanupBossBar();
        activeBoss = null;

        // Schedule next boss earlier if killed (grace period)
        int graceSeconds = plugin.getConfig().getInt("bosses.post-kill-delay-seconds", 1800);
        secondsUntilNextSpawn = graceSeconds;
    }

    private void rewardPlayer(Player player, BossType type) {
        // XP
        player.giveExp(type.getXpReward());

        // Economy via Vault if available
        org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp != null) {
            rsp.getProvider().depositPlayer(player, type.getMoneyReward());
            player.sendMessage("§6§l[Events] §eYou earned §a$" + type.getMoneyReward()
                    + " §efor killing the " + type.getDisplayName() + "§e!");
        }
    }

    private void spawnLoot(Location loc, BossType type) {
        World world = loc.getWorld();
        if (world == null) return;

        // Drop a chest of loot
        world.dropItemNaturally(loc,
                buildLootItem(type));
        // Drop XP orbs
        world.spawn(loc, ExperienceOrb.class, orb -> orb.setExperience(type.getXpReward() / 5));
    }

    private org.bukkit.inventory.ItemStack buildLootItem(BossType type) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.CHEST);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.displayName(legacy("§6§lBoss Loot Chest §7(" + ChatColor.stripColor(type.getDisplayName()) + ")"));
        List<Component> lore = new ArrayList<>();
        lore.add(legacy("§7Looted from the " + type.getDisplayName()));
        lore.add(legacy("§7Difficulty: " + type.getDifficulty()));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Broadcasts ─────────────────────────────────────────────────────────────

    private void broadcastBossSpawn(BossType type, Location loc) {
        String bar  = "§8§m                                        ";
        String msg1 = "  §c§l⚔ WARZONE BOSS SPAWNED ⚔";
        String msg2 = "  §f" + type.getDisplayName() + " §7has appeared!";
        String msg3 = "  §7Difficulty: " + type.getDifficulty();
        String msg4 = "  §7Location: §e" + formatLocation(loc);
        String msg5 = "  §eDefeat it for legendary rewards!";
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage("");
            p.sendMessage(bar);
            p.sendMessage(msg1);
            p.sendMessage(msg2);
            p.sendMessage(msg3);
            p.sendMessage(msg4);
            p.sendMessage(msg5);
            p.sendMessage(bar);
            p.sendMessage("");
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.0f);
        });
    }

    private void broadcastBossDeath(BossType type, Player killer) {
        String bar  = "§8§m                                        ";
        String msg1 = "  §a§l⚡ WARZONE BOSS DEFEATED ⚡";
        String msg2 = "  §f" + type.getDisplayName() + " §7has been slain!";
        String killerLine = killer != null
                ? "  §7Slain by: §e" + killer.getName()
                : "  §7The boss was slain!";
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage("");
            p.sendMessage(bar);
            p.sendMessage(msg1);
            p.sendMessage(msg2);
            p.sendMessage(killerLine);
            p.sendMessage(bar);
            p.sendMessage("");
            p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
        });
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    public ActiveBoss getActiveBoss() { return activeBoss; }

    public boolean isBossEntity(Entity entity) {
        return entity.hasMetadata(BOSS_METADATA_KEY);
    }

    public int getSecondsUntilNextSpawn() { return secondsUntilNextSpawn; }

    public void setSecondsUntilNextSpawn(int seconds) { this.secondsUntilNextSpawn = seconds; }

    public String getBossMetadataKey() { return BOSS_METADATA_KEY; }

    public Entity findBossEntity() {
        if (activeBoss == null) return null;
        return Bukkit.getEntity(activeBoss.getEntityUUID());
    }

    // ── Utilities ──────────────────────────────────────────────────────────────

    private static String formatLocation(Location loc) {
        return loc.getWorld().getName()
                + " §7(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }

    private static Component legacy(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s);
    }
}
