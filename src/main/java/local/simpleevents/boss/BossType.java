package local.simpleevents.boss;

import org.bukkit.entity.EntityType;

/**
 * Defines each warzone boss archetype — name, entity, stats, lore, and rewards.
 */
public enum BossType {

    VOID_WRAITH(
            "void_wraith",
            "§d§lVoid Wraith",
            EntityType.PHANTOM,
            400.0,   // maxHealth
            6.0,     // damagePerHit (extra attack damage attribute)
            0.45,    // moveSpeed
            "§5Legendary",
            "§7A spirit torn between dimensions,\nfeeding on the life force of any\nwarrior foolish enough to face it.",
            500,     // xpReward
            1000     // moneyReward
    ),

    TITAN_SLIME(
            "titan_slime",
            "§a§lTitan Slime",
            EntityType.SLIME,
            750.0,
            10.0,
            0.25,
            "§6Ultimate+",
            "§7A colossal slime that has absorbed\ntoxic sludge for centuries, growing\nto truly monstrous proportions.",
            750,
            1500
    ),

    VENOM_HULK(
            "venom_hulk",
            "§2§lVenom Hulk",
            EntityType.RAVAGER,
            600.0,
            14.0,
            0.30,
            "§4Legendary+",
            "§7A grotesque abomination whose every\nbreath releases clouds of lethal\npoison. Approach only if prepared.",
            1000,
            2000
    ),

    ARACHNID_QUEEN(
            "arachnid_queen",
            "§8§lArachnid Queen",
            EntityType.SPIDER,
            350.0,
            8.0,
            0.50,
            "§6Ultimate",
            "§7The apex predator of the spider world.\nHer venom is capable of dissolving\nsteel in seconds. None have survived.",
            600,
            1200
    ),

    SPECTRAL_STALKER(
            "spectral_stalker",
            "§f§lSpectral Stalker",
            EntityType.STRAY,
            300.0,
            6.5,
            0.40,
            "§5Legendary",
            "§7A wraith bound to the mortal plane,\nhunting the living purely for sport.\nIts arrows freeze the soul.",
            700,
            1400
    ),

    ABYSS_HERALD(
            "abyss_herald",
            "§0§lAbyss Herald",
            EntityType.WARDEN,
            2000.0,
            28.0,
            0.15,
            "§4§lLegendary++",
            "§7A harbinger from the void between\nstars. No recorded warrior has\never faced this evil and lived.",
            2000,
            5000
    );

    private final String key;
    private final String displayName;
    private final EntityType entityType;
    private final double maxHealth;
    private final double extraDamage;
    private final double moveSpeed;
    private final String difficulty;
    private final String lore;
    private final int xpReward;
    private final int moneyReward;

    BossType(String key, String displayName, EntityType entityType,
             double maxHealth, double extraDamage, double moveSpeed,
             String difficulty, String lore, int xpReward, int moneyReward) {
        this.key = key;
        this.displayName = displayName;
        this.entityType = entityType;
        this.maxHealth = maxHealth;
        this.extraDamage = extraDamage;
        this.moveSpeed = moveSpeed;
        this.difficulty = difficulty;
        this.lore = lore;
        this.xpReward = xpReward;
        this.moneyReward = moneyReward;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public EntityType getEntityType() { return entityType; }
    public double getMaxHealth() { return maxHealth; }
    public double getExtraDamage() { return extraDamage; }
    public double getMoveSpeed() { return moveSpeed; }
    public String getDifficulty() { return difficulty; }
    public String getLore() { return lore; }
    public int getXpReward() { return xpReward; }
    public int getMoneyReward() { return moneyReward; }
}
