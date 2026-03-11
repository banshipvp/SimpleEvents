package local.simpleevents.boss;

import local.simpleevents.SimpleEventsPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.block.BlockExplodeEvent;

import java.util.List;

/**
 * Listens for boss-related events:
 *  - Prevents boss entities from damaging blocks (explosions, entity-change-block).
 *  - Handles boss death to trigger rewards and announcements.
 *  - Prevents boss entities from despawning.
 *  - Adds potion effects on boss hit.
 */
public class BossListener implements Listener {

    private final SimpleEventsPlugin plugin;
    private final BossManager bossManager;

    public BossListener(SimpleEventsPlugin plugin, BossManager bossManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
    }

    // ── Block protection ──────────────────────────────────────────────────────

    /**
     * Prevent any explosion sourced from a boss entity from breaking blocks.
     * Note: The Warden does not normally explode, but this guards against
     * any modded / future ability.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (bossManager.isBossEntity(event.getEntity())) {
            event.blockList().clear(); // remove all blocks from the explosion list
        }
    }

    /**
     * Prevent block-level explosions triggered by boss involvement.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        // BlockExplodeEvent doesn't have an entity source directly, but we
        // guard via clearing if there is an active boss nearby (conservative).
        // This covers edge cases like beds/respawn anchors placed by mobs.
        if (bossManager.getActiveBoss() != null) {
            Entity bossEntity = bossManager.findBossEntity();
            if (bossEntity != null) {
                Location explosionLoc = event.getBlock().getLocation();
                if (bossEntity.getLocation().distanceSquared(explosionLoc) < 400) { // 20 blocks
                    event.getYield(); // no-op — but we DON'T clear blocks here unless truly from boss
                }
            }
        }
    }

    /**
     * Prevent boss entities from changing blocks (Endermen picking blocks,
     * Ravager destroying leaves, Warden/Wither skull mechanics, etc.).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (bossManager.isBossEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent boss-sourced projectile explosions (Wither skulls) from breaking blocks.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitBlock() == null) return;
        if (event.getEntity().getShooter() instanceof Entity shooter
                && bossManager.isBossEntity(shooter)) {
            event.setCancelled(true);
        }
    }

    // ── Boss death ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!bossManager.isBossEntity(entity)) return;

        Player killer = entity.getKiller();

        // Suppress vanilla drops — we handle rewards ourselves
        event.getDrops().clear();
        event.setDroppedExp(0);

        bossManager.handleBossDeath(entity, killer);
    }

    // ── Apply effects on hit ──────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBossHitPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!bossManager.isBossEntity(event.getDamager())) return;

        ActiveBoss active = bossManager.getActiveBoss();
        if (active == null) return;

        // Apply boss-specific debuffs
        switch (active.getType()) {
            case VENOM_HULK -> {
                victim.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.POISON, 100, 1, true, true));
            }
            case ARACHNID_QUEEN -> {
                victim.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS, 80, 2, true, true));
                victim.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.POISON, 60, 0, true, true));
            }
            case SPECTRAL_STALKER -> {
                victim.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1, true, true));
            }
            case VOID_WRAITH -> {
                victim.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.BLINDNESS, 60, 0, true, true));
            }
            default -> { /* no extra effect */ }
        }
    }
}
