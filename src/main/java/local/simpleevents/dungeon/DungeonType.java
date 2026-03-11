package local.simpleevents.dungeon;

import org.bukkit.Material;

/**
 * Defines each dungeon available in SimpleEvents.
 * Progressive unlock chain: SUNKEN_CITADEL → CORRUPTED_KEEP → VOID_SANCTUM → TEMPORAL_RIFT
 */
public enum DungeonType {

    SUNKEN_CITADEL(
            "sunken_citadel",
            "§e§lSunken Citadel",
            Material.MOSSY_COBBLESTONE,
            "A distress signal was received from\nan ancient fortress buried beneath\nthe warzone's bedrock layer.\n\nYou are being dispatched as part of\nan elite team to investigate the ruins\nand eliminate any remaining hostiles.",
            4_875_000L, 4_875_000L,
            9_750_000L, 9_750_000L,
            null,
            null
    ),

    CORRUPTED_KEEP(
            "corrupted_keep",
            "§c§lCorrupted Keep",
            Material.CRYING_OBSIDIAN,
            "After deciphering the runes recovered\nfrom the Sunken Citadel and parsing\nthe recovered field reports, a chilling\nseries of events has been uncovered.\n\nThe source of the corruption has been\ntraced. There is a dark rift at the\ncore of this destroyed outpost.\n\nYou have been chosen to lead the\nassault to rid the warzone of this\ndemonic force of destruction.",
            24_375_000L, 24_375_000L,
            48_750_000L, 48_750_000L,
            SUNKEN_CITADEL,
            "§7Warden's Seal §8(Corrupted Keep access)"
    ),

    VOID_SANCTUM(
            "void_sanctum",
            "§5§lVoid Sanctum",
            Material.OBSIDIAN,
            "Located on the edge of reality,\nVoid Sanctum is a mysterious and\ndark place that, until recently,\nheld no significance.\n\nHowever, new intel suggests one of\nthe most infamous warlords in the\nregion, known only as — The Hollow\nPrince, is hiding deep in an abandoned\nmilitary bunker accessible from within.\n\nAn elite team has been assembled, and\nyou have been chosen to launch a\ntactical assault with one simple\nobjective: Eliminate the Hollow Prince.",
            34_125_000L, 34_125_000L,
            68_250_000L, 68_250_000L,
            CORRUPTED_KEEP,
            "§7Void Fragment §8(Void Sanctum access)"
    ),

    TEMPORAL_RIFT(
            "temporal_rift",
            "§b§lTemporal Rift",
            Material.AMETHYST_SHARD,
            "Recent scans across the warzone\nhave detected unusual temporal\ndisturbances emanating from an\nancient artifact uncovered deep\nwithin the Void Sanctum.\n\nThis artifact, known as the\nChrono Shard, is believed to\npossess the power to manipulate\ntime itself.\n\nYour mission is to navigate through\nthe fractured time periods, uncover\nthe secrets of the Chrono Shard,\nand restore the temporal balance\nbefore the fabric of reality is torn apart.",
            58_500_000L, 58_500_000L,
            117_000_000L, 117_000_000L,
            VOID_SANCTUM,
            "§7Chrono Shard §8(Temporal Rift access)"
    );

    private final String key;
    private final String displayName;
    private final Material icon;
    private final String lore;
    private final long normalRewardMin;
    private final long normalRewardMax;
    private final long heroicRewardMin;
    private final long heroicRewardMax;
    private final DungeonType prerequisite;
    /** Null means this is the entry-level dungeon (no unlock item needed). */
    private final String unlockRewardDescription;

    DungeonType(String key, String displayName, Material icon, String lore,
                long normalRewardMin, long normalRewardMax,
                long heroicRewardMin, long heroicRewardMax,
                DungeonType prerequisite, String unlockRewardDescription) {
        this.key = key;
        this.displayName = displayName;
        this.icon = icon;
        this.lore = lore;
        this.normalRewardMin = normalRewardMin;
        this.normalRewardMax = normalRewardMax;
        this.heroicRewardMin = heroicRewardMin;
        this.heroicRewardMax = heroicRewardMax;
        this.prerequisite = prerequisite;
        this.unlockRewardDescription = unlockRewardDescription;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public String getLore() { return lore; }
    public long getNormalRewardMin() { return normalRewardMin; }
    public long getNormalRewardMax() { return normalRewardMax; }
    public long getHeroicRewardMin() { return heroicRewardMin; }
    public long getHeroicRewardMax() { return heroicRewardMax; }
    public DungeonType getPrerequisite() { return prerequisite; }
    public String getUnlockRewardDescription() { return unlockRewardDescription; }

    public static DungeonType fromKey(String key) {
        for (DungeonType type : values()) {
            if (type.key.equalsIgnoreCase(key) || type.name().equalsIgnoreCase(key)) return type;
        }
        return null;
    }
}
