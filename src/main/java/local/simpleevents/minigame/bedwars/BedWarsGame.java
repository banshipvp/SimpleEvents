package local.simpleevents.minigame.bedwars;

import local.simpleevents.SimpleEventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Manages one live Bed Wars match.
 *
 * Responsibilities:
 *  - Team assignment & island tracking
 *  - Player spawn kits
 *  - Death / respawn logic (weapon/tool persistence on death)
 *  - Bed destruction logic
 *  - Elimination tracking
 *  - Generator startup & cleanup
 *  - Island regen & haste effects (applied every 2s to players in their area)
 */
public class BedWarsGame {

    private final SimpleEventsPlugin plugin;

    // Islands indexed by team
    private final Map<BedWarsTeam, BedWarsIsland> islands = new EnumMap<>(BedWarsTeam.class);

    // Players → their team
    private final Map<UUID, BedWarsTeam> playerTeams = new HashMap<>();

    // Tracks the highest pickaxe/axe tier the player has ever owned in this game
    // (persists through death so they respawn with the tier below their best)
    private final Map<UUID, Integer> bestPickaxeTier = new HashMap<>();
    private final Map<UUID, Integer> bestAxeTier     = new HashMap<>();
    // Whether the player has purchased shears (persists)
    private final Set<UUID> hasShears = new HashSet<>();
    // Whether the player has a bow (resets on death like sword)
    private final Set<UUID> hasBow    = new HashSet<>();

    // Island generators
    private final Map<BedWarsTeam, IslandGenerator> generators = new EnumMap<>(BedWarsTeam.class);

    // Central generator (diamond + emerald)
    private CentralGenerator centralGenerator;

    // Repeating task for passive island effects (regen, haste, trap detection)
    private int effectTaskId = -1;

    // Players currently in 3-second spectator respawn cooldown
    private final Set<UUID> respawning = new HashSet<>();

    private boolean ended = false;

    public BedWarsGame(SimpleEventsPlugin plugin) {
        this.plugin = plugin;
        for (BedWarsTeam team : BedWarsTeam.values()) {
            islands.put(team, new BedWarsIsland(team));
        }
    }

    // ── Game start ─────────────────────────────────────────────────────────────

    /**
     * Called once all teams are assigned and locations are configured.
     * Starts island generators, effects loop, and teleports players to spawns.
     */
    public void start() {
        // Start island generators
        for (Map.Entry<BedWarsTeam, BedWarsIsland> entry : islands.entrySet()) {
            IslandGenerator gen = new IslandGenerator(plugin, entry.getValue());
            generators.put(entry.getKey(), gen);
            gen.restart();
        }

        // Give kits to all players
        for (UUID uid : playerTeams.keySet()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) giveKit(p, false);
        }

        // Passive effects loop (every 2 seconds)
        effectTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tickEffects, 40L, 40L);
    }

    /** Clean up all tasks. */
    public void stop() {
        ended = true;
        if (effectTaskId != -1) { Bukkit.getScheduler().cancelTask(effectTaskId); effectTaskId = -1; }
        generators.values().forEach(IslandGenerator::cancel);
        if (centralGenerator != null) centralGenerator.stop();
    }

    // ── Player management ──────────────────────────────────────────────────────

    public void addPlayer(Player player, BedWarsTeam team) {
        playerTeams.put(player.getUniqueId(), team);
        islands.get(team).addMember(player.getUniqueId());
        bestPickaxeTier.put(player.getUniqueId(), 0); // starts at wood (tier 0, but not given until purchased)
        bestAxeTier.put(player.getUniqueId(), 0);
    }

    public BedWarsTeam getTeam(Player player) {
        return playerTeams.get(player.getUniqueId());
    }

    public BedWarsIsland getIslandFor(Player player) {
        BedWarsTeam team = playerTeams.get(player.getUniqueId());
        return team == null ? null : islands.get(team);
    }

    public BedWarsIsland getIsland(BedWarsTeam team) {
        return islands.get(team);
    }

    public Collection<BedWarsIsland> getAllIslands() {
        return islands.values();
    }

    public boolean isParticipant(Player player) {
        return playerTeams.containsKey(player.getUniqueId());
    }

    // ── Kit giving ─────────────────────────────────────────────────────────────

    /**
     * Gives the player their respawn kit.
     *
     * @param afterDeath  true = this is a respawn (sword lost, tools downgraded)
     */
    public void giveKit(Player player, boolean afterDeath) {
        BedWarsIsland island = getIslandFor(player);
        if (island == null) return;

        int protBonus  = island.getProtectionLevel();
        int sharpBonus = island.getSharpnessLevel();
        int effBonus   = island.getEfficiencyLevel();
        UUID uid       = player.getUniqueId();

        // ── Armour (always kept — check what they currently wear, don't downgrade) ──
        applyArmourKit(player, island, protBonus);

        // ── Sword (lost on death) ──
        boolean hasSwordInInv = inventoryContainsSword(player);
        if (!hasSwordInInv) {
            // After death or no sword: give wooden sword
            player.getInventory().addItem(BedWarsItems.woodSword(sharpBonus));
        }

        // ── Pickaxe (downgrade one tier on death, but keep above wood if ever purchased) ──
        applyPickaxeKit(player, uid, effBonus, afterDeath);

        // ── Axe (same rule as pickaxe) ──
        applyAxeKit(player, uid, effBonus, afterDeath);

        // ── Shears (permanent after purchase) ──
        if (hasShears.contains(uid) && !inventoryHas(player, Material.SHEARS)) {
            player.getInventory().addItem(BedWarsItems.shears());
        }

        // ── No food starvation ──
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0f);
        player.setHealthScaled(false);

        // Remove armour durability concerns: already unbreakable
    }

    private void applyArmourKit(Player player, BedWarsIsland island, int protBonus) {
        // Gear is never downgraded — only upgraded when purchased.
        // On respawn we just ensure the gear is still there.
        ItemStack[] armour = player.getInventory().getArmorContents();

        // Determine current armour tier
        int tier = getArmourTier(armour);

        if (tier == -1) {
            // No armour at all — give starting leather
            player.getInventory().setHelmet(BedWarsItems.leatherHelmet(protBonus));
            player.getInventory().setChestplate(BedWarsItems.leatherChestplate(protBonus));
            player.getInventory().setLeggings(BedWarsItems.leatherLeggings(protBonus));
            player.getInventory().setBoots(BedWarsItems.leatherBoots(protBonus));
        }
        // If armour exists: re-apply the correct protection enchant to reflect new island level
        // (protection upgrades benefit existing gear too)
        if (tier >= 0) {
            reenchantArmour(player, protBonus);
        }
    }

    private int getArmourTier(ItemStack[] armour) {
        for (ItemStack piece : armour) {
            if (piece == null) continue;
            String name = piece.getType().name();
            if (name.contains("DIAMOND"))    return 3;
            if (name.contains("IRON"))       return 2;
            if (name.contains("CHAINMAIL"))  return 1;
            if (name.contains("LEATHER"))    return 0;
        }
        return -1; // no armour
    }

    private void reenchantArmour(Player player, int protBonus) {
        ItemStack[] slots = player.getInventory().getArmorContents();
        for (ItemStack piece : slots) {
            if (piece == null) continue;
            piece.removeEnchantment(Enchantment.PROTECTION);
            if (protBonus > 0) piece.addUnsafeEnchantment(Enchantment.PROTECTION, protBonus);
        }
        player.getInventory().setArmorContents(slots);
    }

    private void applyPickaxeKit(Player player, UUID uid, int effBonus, boolean afterDeath) {
        int cur = currentPickaxeTier(player);
        int best = bestPickaxeTier.getOrDefault(uid, -1); // -1 = never purchased

        if (best < 0) return; // never purchased a pickaxe — don't give one

        // On death: downgrade one tier (but not below wood=0)
        if (afterDeath) {
            int respawnTier = Math.max(0, best - 1);
            bestPickaxeTier.put(uid, respawnTier);
            replacePickaxe(player, respawnTier, effBonus);
            return;
        }

        // Living respawn (game start or kit re-application): ensure they have their best tier
        if (cur < best) {
            replacePickaxe(player, best, effBonus);
        }
    }

    private void applyAxeKit(Player player, UUID uid, int effBonus, boolean afterDeath) {
        int best = bestAxeTier.getOrDefault(uid, -1);
        if (best < 0) return;

        if (afterDeath) {
            int respawnTier = Math.max(0, best - 1);
            bestAxeTier.put(uid, respawnTier);
            replaceAxe(player, respawnTier, effBonus);
            return;
        }
        int cur = currentAxeTier(player);
        if (cur < best) {
            replaceAxe(player, best, effBonus);
        }
    }

    private void replacePickaxe(Player player, int tier, int effBonus) {
        removeAllPickaxes(player);
        player.getInventory().addItem(switch (tier) {
            case 0 -> BedWarsItems.woodPickaxe(effBonus);
            case 1 -> BedWarsItems.stonePickaxe(effBonus);
            case 2 -> BedWarsItems.ironPickaxe(effBonus);
            default -> BedWarsItems.diamondPickaxe(effBonus);
        });
    }

    private void replaceAxe(Player player, int tier, int effBonus) {
        removeAllAxes(player);
        player.getInventory().addItem(switch (tier) {
            case 0 -> BedWarsItems.woodAxe(effBonus);
            case 1 -> BedWarsItems.stoneAxe(effBonus);
            case 2 -> BedWarsItems.ironAxe(effBonus);
            default -> BedWarsItems.diamondAxe(effBonus);
        });
    }

    // ── Tool/weapon inventory helpers ──────────────────────────────────────────

    public boolean inventoryContainsSword(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && BedWarsItems.isSword(item.getType())) return true;
        }
        return false;
    }

    public boolean inventoryHas(Player player, Material mat) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) return true;
        }
        return false;
    }

    /** Removes all sword items from player inventory. */
    public void removeAllSwords(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && BedWarsItems.isSword(contents[i].getType())) {
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);
    }

    /** Removes all pickaxes from inventory. */
    public void removeAllPickaxes(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && BedWarsItems.isPickaxe(contents[i].getType())) {
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);
    }

    /** Removes all axes from inventory. */
    public void removeAllAxes(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && BedWarsItems.isAxe(contents[i].getType())) {
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);
    }

    private int currentPickaxeTier(Player player) {
        int best = -1;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) best = Math.max(best, BedWarsItems.pickaxeTier(item.getType()));
        }
        return best;
    }

    private int currentAxeTier(Player player) {
        int best = -1;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) best = Math.max(best, BedWarsItems.axeTier(item.getType()));
        }
        return best;
    }

    // ── Death & respawn ─────────────────────────────────────────────────────────

    /**
     * Called when a Bed Wars player dies.
     *  - If their bed is alive: 3-second spectator → respawn at island
     *  - If their bed is gone: eliminated
     */
    public void handleDeath(Player player) {
        UUID uid = player.getUniqueId();
        BedWarsTeam team = playerTeams.get(uid);
        if (team == null) return;

        BedWarsIsland island = islands.get(team);

        // Clean up inventory before storing what should persist
        preserveOnDeath(player, uid, island);

        if (!island.isBedAlive()) {
            // Eliminated
            island.markEliminated(uid);
            player.sendMessage("§c[Bed Wars] §7Your bed has been destroyed! You are eliminated.");
            checkGameEnd();
            return;
        }

        // Spectator mode, then respawn
        respawning.add(uid);
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage("§e[Bed Wars] §7You will respawn in 3 seconds...");

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            respawning.remove(uid);
            Player p = Bukkit.getPlayer(uid);
            if (p == null || !p.isOnline() || !playerTeams.containsKey(uid)) return;
            Location spawn = island.getSpawnLocation();
            if (spawn != null) p.teleport(spawn);
            p.setGameMode(GameMode.SURVIVAL);
            giveKit(p, true);

            // No food starvation
            p.setFoodLevel(20);
            p.setSaturation(20f);
        }, 60L); // 3 seconds
    }

    /**
     * Before clearing inventory on death:
     * - note best pickaxe/axe tier
     * - note if player has shears/bow
     * Sword and bow are lost.
     */
    private void preserveOnDeath(Player player, UUID uid, BedWarsIsland island) {
        int pickTier = currentPickaxeTier(player);
        if (pickTier >= 0) {
            bestPickaxeTier.put(uid, pickTier);
        }
        int axeTier = currentAxeTier(player);
        if (axeTier >= 0) {
            bestAxeTier.put(uid, axeTier);
        }
        // Shears persistent
        if (inventoryHas(player, Material.SHEARS)) hasShears.add(uid);
    }

    // ── Bed destruction ────────────────────────────────────────────────────────

    /**
     * Called when a bed block is broken by a player.
     * Returns true if this was a valid bed destruction.
     */
    public boolean handleBedBreak(Player breaker, Location bedLoc) {
        BedWarsTeam breakerTeam = playerTeams.get(breaker.getUniqueId());
        if (breakerTeam == null) return false;

        // Find which island owns this bed
        for (BedWarsIsland island : islands.values()) {
            if (island.getBedLocation() == null) continue;
            if (!island.isBedAlive()) continue;
            Location stored = island.getBedLocation();
            if (stored.getBlockX() == bedLoc.getBlockX()
                    && stored.getBlockY() == bedLoc.getBlockY()
                    && stored.getBlockZ() == bedLoc.getBlockZ()) {

                // Prevent breaking own bed
                if (island.getTeam() == breakerTeam) {
                    breaker.sendMessage("§c[Bed Wars] §7You cannot break your own bed!");
                    return false;
                }

                island.setBedDestroyed();
                BedWarsTeam victim = island.getTeam();
                Bukkit.broadcastMessage(victim.getColor() + island.getTeam().name()
                        + " §7team's bed was destroyed by "
                        + breakerTeam.getColor() + breaker.getName() + "§7!");

                // Notify the victim team
                for (UUID uid : island.getMembers()) {
                    Player p = Bukkit.getPlayer(uid);
                    if (p != null) {
                        p.sendMessage("§c[Bed Wars] §7Your bed has been destroyed! Fight to stay alive!");
                    }
                }
                checkGameEnd();
                return true;
            }
        }
        return false;
    }

    // ── Game end check ─────────────────────────────────────────────────────────

    private void checkGameEnd() {
        List<BedWarsIsland> alive = new ArrayList<>();
        for (BedWarsIsland island : islands.values()) {
            if (!island.isFullyEliminated() && island.getMemberCount() > 0) {
                alive.add(island);
            }
        }
        if (alive.size() <= 1) {
            stop();
            if (!alive.isEmpty()) {
                BedWarsIsland winner = alive.get(0);
                Bukkit.broadcastMessage("§6[Bed Wars] §e" + winner.getTeam().getDisplayName()
                        + " §6team wins! Congratulations!");
            }
        }
    }

    // ── Shop purchase handling ─────────────────────────────────────────────────

    /**
     * Processes a shop purchase for the given player.
     * Returns true if successful.
     */
    public boolean processPurchase(Player player, BedWarsShopGui.ShopItem item) {
        if (!hasEnough(player, item.currency(), item.cost())) {
            player.sendMessage("§c[Bed Wars] §7Not enough " + item.currency().name() + "!");
            return false;
        }

        UUID uid = player.getUniqueId();
        BedWarsIsland island = getIslandFor(player);
        int effBonus  = island != null ? island.getEfficiencyLevel() : 0;
        int sharpBonus = island != null ? island.getSharpnessLevel() : 0;

        String tag = item.tag();
        Material give = item.give();

        // Handle special cases
        if (tag.endsWith("_sword")) {
            int tier = BedWarsItems.swordTier(give);
            removeAllSwords(player);
            ItemStack sword = switch (tier) {
                case 1 -> BedWarsItems.stoneSword(sharpBonus);
                case 2 -> BedWarsItems.ironSword(sharpBonus);
                case 3 -> BedWarsItems.diamondSword(sharpBonus);
                default -> BedWarsItems.woodSword(sharpBonus);
            };
            deduct(player, item.currency(), item.cost());
            player.getInventory().addItem(sword);
            return true;
        }

        if (tag.endsWith("_pickaxe")) {
            int tier = BedWarsItems.pickaxeTier(give);
            bestPickaxeTier.put(uid, tier);
            replacePickaxe(player, tier, effBonus);
            deduct(player, item.currency(), item.cost());
            return true;
        }

        if (tag.endsWith("_axe")) {
            int tier = BedWarsItems.axeTier(give);
            bestAxeTier.put(uid, tier);
            replaceAxe(player, tier, effBonus);
            deduct(player, item.currency(), item.cost());
            return true;
        }

        if (tag.equals("shears")) {
            hasShears.add(uid);
            deduct(player, item.currency(), item.cost());
            player.getInventory().addItem(BedWarsItems.shears());
            return true;
        }

        if (tag.equals("bow")) {
            // Replace existing bow
            removeAllBows(player);
            hasBow.add(uid);
            deduct(player, item.currency(), item.cost());
            player.getInventory().addItem(BedWarsItems.bow(0));
            return true;
        }

        if (tag.equals("chain_armour")) {
            int prot = island != null ? island.getProtectionLevel() : 0;
            deduct(player, item.currency(), item.cost());
            replaceArmourSet(player, "CHAINMAIL", prot);
            return true;
        }
        if (tag.equals("iron_armour")) {
            int prot = island != null ? island.getProtectionLevel() : 0;
            deduct(player, item.currency(), item.cost());
            replaceArmourSet(player, "IRON", prot);
            return true;
        }
        if (tag.equals("diamond_armour")) {
            int prot = island != null ? island.getProtectionLevel() : 0;
            deduct(player, item.currency(), item.cost());
            replaceArmourSet(player, "DIAMOND", prot);
            return true;
        }

        // Generic: potions handled by caller via tag
        deduct(player, item.currency(), item.cost());
        player.getInventory().addItem(new ItemStack(give, item.qty()));
        return true;
    }

    private void replaceArmourSet(Player player, String prefix, int protBonus) {
        player.getInventory().setHelmet(makeArmourPiece(Material.valueOf(prefix + "_HELMET"), protBonus));
        player.getInventory().setChestplate(makeArmourPiece(Material.valueOf(prefix + "_CHESTPLATE"), protBonus));
        player.getInventory().setLeggings(makeArmourPiece(Material.valueOf(prefix + "_LEGGINGS"), protBonus));
        player.getInventory().setBoots(makeArmourPiece(Material.valueOf(prefix + "_BOOTS"), protBonus));
    }

    private ItemStack makeArmourPiece(Material mat, int protBonus) {
        ItemStack item = new ItemStack(mat);
        BedWarsItems.applyUnbreakable(item);
        if (protBonus > 0) item.addUnsafeEnchantment(Enchantment.PROTECTION, protBonus);
        return item;
    }

    private void removeAllBows(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() == Material.BOW) contents[i] = null;
        }
        player.getInventory().setContents(contents);
    }

    // ── Currency helpers ───────────────────────────────────────────────────────

    public boolean hasEnough(Player player, Material currency, int amount) {
        return countCurrency(player, currency) >= amount;
    }

    private int countCurrency(Player player, Material currency) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == currency) total += item.getAmount();
        }
        return total;
    }

    public void deduct(Player player, Material currency, int amount) {
        int toRemove = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && toRemove > 0; i++) {
            if (contents[i] != null && contents[i].getType() == currency) {
                int take = Math.min(contents[i].getAmount(), toRemove);
                toRemove -= take;
                contents[i].setAmount(contents[i].getAmount() - take);
                if (contents[i].getAmount() <= 0) contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);
    }

    // ── Upgrade purchase handling ──────────────────────────────────────────────

    public boolean processUpgrade(Player player, String upgradeKey) {
        BedWarsIsland island = getIslandFor(player);
        if (island == null) return false;

        return switch (upgradeKey) {
            case "sharpness" -> {
                if (island.getSharpnessLevel() >= 1) { player.sendMessage("§c[Bed Wars] Already purchased!"); yield false; }
                if (!hasEnough(player, Material.DIAMOND, 8)) { player.sendMessage("§c[Bed Wars] Need 8 Diamonds!"); yield false; }
                deduct(player, Material.DIAMOND, 8);
                island.setSharpnessLevel(1);
                // Re-apply sharpness to all online team members' swords
                applySharpnessToTeam(island);
                yield true;
            }
            case "protection" -> {
                int cur = island.getProtectionLevel();
                if (cur >= 5) { player.sendMessage("§c[Bed Wars] Max protection!"); yield false; }
                if (!hasEnough(player, Material.DIAMOND, 4)) { player.sendMessage("§c[Bed Wars] Need 4 Diamonds!"); yield false; }
                deduct(player, Material.DIAMOND, 4);
                island.setProtectionLevel(cur + 1);
                applyProtectionToTeam(island);
                yield true;
            }
            case "efficiency" -> {
                if (island.getEfficiencyLevel() >= 1) { player.sendMessage("§c[Bed Wars] Already purchased!"); yield false; }
                if (!hasEnough(player, Material.DIAMOND, 6)) { player.sendMessage("§c[Bed Wars] Need 6 Diamonds!"); yield false; }
                deduct(player, Material.DIAMOND, 6);
                island.setEfficiencyLevel(1);
                applyEfficiencyToTeam(island);
                yield true;
            }
            case "haste" -> {
                if (island.hasHaste()) { player.sendMessage("§c[Bed Wars] Already purchased!"); yield false; }
                if (!hasEnough(player, Material.DIAMOND, 4)) { player.sendMessage("§c[Bed Wars] Need 4 Diamonds!"); yield false; }
                deduct(player, Material.DIAMOND, 4);
                island.setHaste(true);
                yield true;
            }
            case "regen_kill" -> {
                if (island.hasRegenOnKill()) { player.sendMessage("§c[Bed Wars] Already purchased!"); yield false; }
                if (!hasEnough(player, Material.DIAMOND, 4)) { player.sendMessage("§c[Bed Wars] Need 4 Diamonds!"); yield false; }
                deduct(player, Material.DIAMOND, 4);
                island.setRegenOnKill(true);
                yield true;
            }
            case "island_regen" -> {
                if (island.hasIslandRegen()) { player.sendMessage("§c[Bed Wars] Already purchased!"); yield false; }
                if (!hasEnough(player, Material.DIAMOND, 6)) { player.sendMessage("§c[Bed Wars] Need 6 Diamonds!"); yield false; }
                deduct(player, Material.DIAMOND, 6);
                island.setIslandRegen(true);
                yield true;
            }
            case "trap" -> {
                int lvl = island.getTrapLevel();
                if (lvl == 0) {
                    if (!hasEnough(player, Material.DIAMOND, 5)) { player.sendMessage("§c[Bed Wars] Need 5 Diamonds!"); yield false; }
                    deduct(player, Material.DIAMOND, 5);
                    island.setTrapLevel(1);
                    island.setTrapReady(true);
                } else if (lvl == 1) {
                    if (!hasEnough(player, Material.DIAMOND, 10)) { player.sendMessage("§c[Bed Wars] Need 10 Diamonds!"); yield false; }
                    deduct(player, Material.DIAMOND, 10);
                    island.setTrapLevel(2);
                    island.setTrapReady(true);
                } else {
                    // already level 2: re-arm
                    if (!island.isTrapReady()) {
                        if (!hasEnough(player, Material.DIAMOND, 10)) { player.sendMessage("§c[Bed Wars] Need 10 Diamonds to re-arm!"); yield false; }
                        deduct(player, Material.DIAMOND, 10);
                        island.setTrapReady(true);
                    } else {
                        player.sendMessage("§c[Bed Wars] Trap is already armed!");
                        yield false;
                    }
                }
                yield true;
            }
            case "generator" -> {
                GeneratorTier cur = island.getGeneratorTier();
                int next = cur.ordinal() + 1;
                if (next >= GeneratorTier.values().length) { player.sendMessage("§c[Bed Wars] Generator maxed!"); yield false; }

                int ironCost    = BedWarsIsland.GEN_UPGRADE_COST_IRON[next];
                int goldCost    = BedWarsIsland.GEN_UPGRADE_COST_GOLD[next];
                int diamondCost = BedWarsIsland.GEN_UPGRADE_COST_DIAMOND[next];

                if (!hasEnough(player, Material.IRON_INGOT, ironCost)
                    || !hasEnough(player, Material.GOLD_INGOT, goldCost)
                    || !hasEnough(player, Material.DIAMOND, diamondCost)) {
                    player.sendMessage("§c[Bed Wars] Insufficient resources!");
                    yield false;
                }
                if (ironCost > 0)    deduct(player, Material.IRON_INGOT, ironCost);
                if (goldCost > 0)    deduct(player, Material.GOLD_INGOT, goldCost);
                if (diamondCost > 0) deduct(player, Material.DIAMOND, diamondCost);

                island.setGeneratorTier(GeneratorTier.values()[next]);
                IslandGenerator gen = generators.get(island.getTeam());
                if (gen != null) gen.restart();
                player.sendMessage("§a[Bed Wars] Generator upgraded to " + GeneratorTier.values()[next].getDisplayName() + "!");
                yield true;
            }
            default -> false;
        };
    }

    // ── Apply upgrades to team weapons ────────────────────────────────────────

    private void applySharpnessToTeam(BedWarsIsland island) {
        for (UUID uid : island.getMembers()) {
            Player p = Bukkit.getPlayer(uid);
            if (p == null) continue;
            for (ItemStack item : p.getInventory().getContents()) {
                if (item != null && BedWarsItems.isSword(item.getType())) {
                    item.removeEnchantment(Enchantment.SHARPNESS);
                    item.addUnsafeEnchantment(Enchantment.SHARPNESS, island.getSharpnessLevel());
                }
            }
        }
    }

    private void applyProtectionToTeam(BedWarsIsland island) {
        for (UUID uid : island.getMembers()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) reenchantArmour(p, island.getProtectionLevel());
        }
    }

    private void applyEfficiencyToTeam(BedWarsIsland island) {
        for (UUID uid : island.getMembers()) {
            Player p = Bukkit.getPlayer(uid);
            if (p == null) continue;
            for (ItemStack item : p.getInventory().getContents()) {
                if (item != null && (BedWarsItems.isPickaxe(item.getType()) || BedWarsItems.isAxe(item.getType()))) {
                    item.removeEnchantment(Enchantment.EFFICIENCY);
                    item.addUnsafeEnchantment(Enchantment.EFFICIENCY, island.getEfficiencyLevel());
                }
            }
        }
    }

    // ── Passive effects tick ───────────────────────────────────────────────────

    private void tickEffects() {
        for (BedWarsIsland island : islands.values()) {
            Location spawn = island.getSpawnLocation();

            for (UUID uid : island.getMembers()) {
                Player p = Bukkit.getPlayer(uid);
                if (p == null || p.getGameMode() == GameMode.SPECTATOR) continue;

                // No food starvation
                if (p.getFoodLevel() < 20) p.setFoodLevel(20);

                // Island regen
                if (island.hasIslandRegen() && spawn != null
                        && p.getWorld().equals(spawn.getWorld())
                        && p.getLocation().distanceSquared(spawn) <= 625) { // 25 block radius
                    p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false));
                }

                // Haste
                if (island.hasHaste()) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 0, false, false));
                }

                // Sword monitor: if a player stowed their sword in a chest,
                // they should get a wooden sword back (if they have no sword at all)
                if (!inventoryContainsSword(p)) {
                    BedWarsIsland pi = getIslandFor(p);
                    if (pi != null) {
                        p.getInventory().addItem(BedWarsItems.woodSword(pi.getSharpnessLevel()));
                    }
                }

                // Similarly, if they no longer have a pickaxe but had purchased one:
                UUID puid = p.getUniqueId();
                if (bestPickaxeTier.getOrDefault(puid, -1) >= 0 && currentPickaxeTier(p) < 0) {
                    replacePickaxe(p, bestPickaxeTier.get(puid), island.getEfficiencyLevel());
                }

                // Axe:
                if (bestAxeTier.getOrDefault(puid, -1) >= 0 && currentAxeTier(p) < 0) {
                    replaceAxe(p, bestAxeTier.get(puid), island.getEfficiencyLevel());
                }
            }

            // Trap: check for enemies near the bed
            if (island.isTrapReady() && island.getBedLocation() != null) {
                Location bed = island.getBedLocation();
                for (Map.Entry<UUID, BedWarsTeam> entry : playerTeams.entrySet()) {
                    if (entry.getValue() == island.getTeam()) continue; // skip friendlies
                    Player enemy = Bukkit.getPlayer(entry.getKey());
                    if (enemy == null) continue;
                    if (!enemy.getWorld().equals(bed.getWorld())) continue;
                    if (enemy.getLocation().distanceSquared(bed) <= 100) { // 10 blocks
                        island.fireTrap(enemy);
                        // Notify team
                        for (UUID uid : island.getMembers()) {
                            Player tp = Bukkit.getPlayer(uid);
                            if (tp != null) tp.sendMessage("§e[Bed Wars] §7A trap was triggered near your bed!");
                        }
                        break;
                    }
                }
            }
        }
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public boolean isEnded()                    { return ended; }
    public boolean isRespawning(UUID uid)        { return respawning.contains(uid); }
    public Map<BedWarsTeam, BedWarsIsland> getIslands() { return Collections.unmodifiableMap(islands); }

    public void setCentralGenerator(CentralGenerator gen) { this.centralGenerator = gen; }
    public CentralGenerator getCentralGenerator()         { return centralGenerator; }

    public Map<UUID, BedWarsTeam> getPlayerTeams()       { return Collections.unmodifiableMap(playerTeams); }
}
