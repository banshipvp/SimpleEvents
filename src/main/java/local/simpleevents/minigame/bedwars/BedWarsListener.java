package local.simpleevents.minigame.bedwars;

import local.simpleevents.SimpleEventsPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles all Bed Wars in-game events for a single BedWarsGame instance.
 *
 * Key responsibilities:
 *  - Death & respawn (with weapon/tool persistence)
 *  - Bed breaking
 *  - TNT auto-ignite & explosion whitelist
 *  - Chest access control
 *  - NPC interaction → open shop/upgrades GUI
 *  - Shop & upgrades GUI click handling
 *  - Kills → Regen on Kill effect
 *  - Food level lock (no starvation)
 */
public class BedWarsListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final SimpleEventsPlugin plugin;
    private final BedWarsGame        game;
    private final BedWarsNpcManager  npcManager;

    /** Blocks placed by players during the game (for cleanup and explosion checks). */
    private final Set<Location> placedBlocks = new HashSet<>();

    public BedWarsListener(SimpleEventsPlugin plugin, BedWarsGame game, BedWarsNpcManager npcManager) {
        this.plugin     = plugin;
        this.game       = game;
        this.npcManager = npcManager;
    }

    // ── Death / Respawn ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!game.isParticipant(player)) return;

        // Drop nothing on death — gear persists or is managed by handleDeath
        event.getDrops().clear();
        event.setKeepInventory(true); // we handle items ourselves
        event.setDeathMessage(null);

        // Regen on Kill for the killer's team
        Player killer = player.getKiller();
        if (killer != null && game.isParticipant(killer)) {
            BedWarsIsland killerIsland = game.getIslandFor(killer);
            if (killerIsland != null && killerIsland.hasRegenOnKill()) {
                killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 5, 1, false, true));
            }
        }

        game.handleDeath(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!game.isParticipant(player)) return;

        // BedWarsGame.handleDeath already schedules the spectator → respawn flow.
        // Here we just clear inventory on natural respawn to avoid duplicate items.
        // The giveKit is called by handleDeath after 3s, so we clear now.
        player.getInventory().clear();
    }

    // ── Food lock ─────────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevel(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!game.isParticipant(player)) return;
        if (event.getFoodLevel() < player.getFoodLevel()) {
            event.setCancelled(true); // prevent starvation
        }
    }

    // ── Damage bonuses ────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Team protection: no friendly fire
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) {
            // Check for projectile
            if (event.getDamager() instanceof Projectile proj
                    && proj.getShooter() instanceof Player shooter) {
                if (!game.isParticipant(shooter) || !game.isParticipant(victim)) return;
                if (game.getTeam(shooter) == game.getTeam(victim)) {
                    event.setCancelled(true); // no team damage
                }
            }
            return;
        }
        if (!game.isParticipant(attacker) || !game.isParticipant(victim)) return;
        if (game.getTeam(attacker) == game.getTeam(victim)) {
            event.setCancelled(true); // no friendly fire
        }
    }

    // ── Bed breaking ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Remove from placed-blocks tracking
        placedBlocks.remove(block.getLocation());

        // Only intercept bed blocks during active games
        if (!game.isParticipant(player)) return;
        if (!isBedMaterial(block.getType())) return;

        // Try to handle as a bed destruction
        boolean handled = game.handleBedBreak(player, block.getLocation());
        if (!handled) {
            event.setCancelled(true); // own bed or not a tracked bed
        }
        // Plugin will handle dropping the block (or not)
        event.setDropItems(false);
    }

    private boolean isBedMaterial(Material mat) {
        return mat.name().endsWith("_BED");
    }

    // ── Block placement (TNT auto-ignite) ─────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!game.isParticipant(player)) return;

        Block placed = event.getBlockPlaced();

        // Track placed blocks for cleanup
        placedBlocks.add(placed.getLocation());

        // Auto-ignite TNT
        if (placed.getType() == Material.TNT) {
            placed.setType(Material.AIR);
            placed.getWorld().spawn(placed.getLocation().add(0.5, 0, 0.5), TNTPrimed.class, tnt -> {
                tnt.setFuseTicks(40); // 2 second fuse
                tnt.setSource(player);
            });
            // Consume one TNT from hand already done by BlockPlaceEvent
        }
    }

    // ── TNT / Explosion — only destroys wood & wool ───────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> !isExplosionDestructible(block.getType()));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        event.blockList().removeIf(block -> !isExplosionDestructible(block.getType()));
    }

    /**
     * Only wood planks and wool are destroyable by TNT in Bed Wars.
     * Obsidian, glass, end stone, stone and all other blocks are immune.
     */
    private boolean isExplosionDestructible(Material mat) {
        String name = mat.name();
        return name.contains("_PLANKS") || name.endsWith("_WOOL")
               || mat == Material.OAK_PLANKS || mat == Material.SPRUCE_PLANKS
               || mat == Material.BIRCH_PLANKS || mat == Material.JUNGLE_PLANKS
               || mat == Material.ACACIA_PLANKS || mat == Material.DARK_OAK_PLANKS
               || mat == Material.MANGROVE_PLANKS || mat == Material.CHERRY_PLANKS;
    }

    // ── Chest access ──────────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!game.isParticipant(player)) return;

        Inventory inv = event.getInventory();
        if (inv.getType() != InventoryType.CHEST) return;

        // Find which team this chest belongs to
        if (!(inv.getHolder() instanceof Chest chest)) return;
        Location chestLoc = chest.getLocation();

        BedWarsTeam playerTeam = game.getTeam(player);
        BedWarsIsland playerIsland = game.getIslandFor(player);

        for (BedWarsIsland island : game.getAllIslands()) {
            Location tc = island.getTeamChestLocation();
            if (tc == null) continue;
            if (!sameBlock(tc, chestLoc)) continue;

            // It's a team chest
            if (island.getTeam() == playerTeam) {
                // Own chest — always OK
                return;
            } else {
                // Enemy chest — only accessible once that team's bed is destroyed
                if (island.isBedAlive()) {
                    event.setCancelled(true);
                    player.sendMessage("§c[Bed Wars] §7You cannot loot their chest while their bed stands!");
                }
                return;
            }
        }
    }

    private boolean sameBlock(Location a, Location b) {
        return a.getBlockX() == b.getBlockX()
            && a.getBlockY() == b.getBlockY()
            && a.getBlockZ() == b.getBlockZ()
            && a.getWorld().equals(b.getWorld());
    }

    // ── NPC interaction ───────────────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!game.isParticipant(player)) return;

        Entity target = event.getRightClicked();
        UUID eid = target.getUniqueId();

        if (!npcManager.isNpc(eid)) return;

        event.setCancelled(true); // prevent trading screen

        BedWarsNpcManager.NpcRole role = npcManager.getRoleOf(eid);
        if (role == null) return;

        switch (role) {
            case SHOP     -> BedWarsShopGui.openBlocks(player, game);
            case UPGRADER -> BedWarsUpgradesGui.open(player, game);
        }
    }

    // ── Shop GUI clicks ───────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!game.isParticipant(player)) return;

        String title = LEGACY.serialize(event.getView().title());
        int slot = event.getRawSlot();

        // ── Shop tabs navigation ──────────────────────────────────────────────
        if (isShopTitle(title)) {
            event.setCancelled(true);

            // Tab navigation
            if (slot == BedWarsShopGui.TAB_BLOCKS)  { BedWarsShopGui.openBlocks(player, game);  return; }
            if (slot == BedWarsShopGui.TAB_GEAR)     { BedWarsShopGui.openGear(player, game);    return; }
            if (slot == BedWarsShopGui.TAB_TOOLS)    { BedWarsShopGui.openTools(player, game);   return; }
            if (slot == BedWarsShopGui.TAB_MISC)     { BedWarsShopGui.openMisc(player, game);    return; }
            if (slot == BedWarsShopGui.TAB_POTIONS)  { BedWarsShopGui.openPotions(player, game); return; }

            // Attempt purchase
            BedWarsShopGui.ShopItem shopItem = BedWarsShopGui.getShopItem(title, slot);
            if (shopItem == null) return;

            if (shopItem.tag().equals("potion") || shopItem.category().equals("potions")) {
                handlePotionPurchase(player, shopItem);
            } else {
                game.processPurchase(player, shopItem);
            }
            return;
        }

        // ── Upgrades GUI clicks ───────────────────────────────────────────────
        if (BedWarsUpgradesGui.TITLE.equals(title)) {
            event.setCancelled(true);
            handleUpgradeClick(player, slot);
        }
    }

    private boolean isShopTitle(String title) {
        return title.equals(BedWarsShopGui.TITLE_BLOCKS)
            || title.equals(BedWarsShopGui.TITLE_GEAR)
            || title.equals(BedWarsShopGui.TITLE_TOOLS)
            || title.equals(BedWarsShopGui.TITLE_MISC)
            || title.equals(BedWarsShopGui.TITLE_POTIONS);
    }

    private void handleUpgradeClick(Player player, int slot) {
        String key = switch (slot) {
            case BedWarsUpgradesGui.SLOT_SHARPNESS    -> "sharpness";
            case BedWarsUpgradesGui.SLOT_PROTECTION   -> "protection";
            case BedWarsUpgradesGui.SLOT_EFFICIENCY   -> "efficiency";
            case BedWarsUpgradesGui.SLOT_HASTE        -> "haste";
            case BedWarsUpgradesGui.SLOT_REGEN_KILL   -> "regen_kill";
            case BedWarsUpgradesGui.SLOT_ISLAND_REGEN -> "island_regen";
            case BedWarsUpgradesGui.SLOT_TRAP         -> "trap";
            case BedWarsUpgradesGui.SLOT_GEN_UPGRADE  -> "generator";
            default -> null;
        };
        if (key == null) return;

        boolean success = game.processUpgrade(player, key);
        if (success) {
            // Refresh upgrades GUI
            BedWarsUpgradesGui.open(player, game);
        }
    }

    // ── Potion purchases ──────────────────────────────────────────────────────

    private void handlePotionPurchase(Player player, BedWarsShopGui.ShopItem item) {
        if (!game.hasEnough(player, item.currency(), item.cost())) {
            player.sendMessage("§c[Bed Wars] §7Not enough " + item.currency().name() + "!");
            return;
        }

        PotionType type;
        boolean splash = false;
        switch (item.tag()) {
            case "healing"     -> { type = PotionType.HEALING;       splash = true; }
            case "speed"       -> { type = PotionType.SWIFTNESS;     splash = false; }
            case "regen"       -> { type = PotionType.REGENERATION;  splash = false; }
            case "invisibility"-> { type = PotionType.INVISIBILITY;  splash = false; }
            case "jump"        -> { type = PotionType.LEAPING;       splash = false; }
            default            -> { player.sendMessage("§c[Bed Wars] Unknown potion."); return; }
        }

        Material mat = splash ? Material.SPLASH_POTION : Material.POTION;
        ItemStack potion = new ItemStack(mat);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(type);
        potion.setItemMeta(meta);

        game.deduct(player, item.currency(), item.cost());
        player.getInventory().addItem(potion);
    }

    // ── Player quit ───────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Remove from lobby if waiting
        game.leaveLobby(player);

        if (!game.isParticipant(player)) return;

        // Treat disconnect as death/elimination
        BedWarsIsland island = game.getIslandFor(player);
        if (island != null) {
            island.markEliminated(player.getUniqueId());
        }
    }

    // ── Prevent spectators from interacting ───────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!game.isParticipant(player)) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        // Nothing extra to cancel; normal interact allowed
    }

    // ── Prevent droppping game items out of the world ─────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!game.isParticipant(player)) return;
        // Items are allowed to drop; currency and such can be dropped naturally
        // Keep this handler if you want to lock drops (currently permissive)
    }

    // ── Getters for cleanup ───────────────────────────────────────────────────

    public Set<Location> getPlacedBlocks() { return placedBlocks; }
}
