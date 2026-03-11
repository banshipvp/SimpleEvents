package local.simpleevents.minigame.bedwars;

/**
 * Upgrade tiers for island resource generators.
 *
 * TIER_0 – default: 1 iron/s, 1 gold/4s
 * TIER_1 – 2 iron/s, 2 iron+1 gold/2s
 * TIER_2 – 4 iron/s, 2 gold/s, 1 diamond/6s
 * TIER_3 – 8 iron/s, 4 gold/s, 1 diamond/3s, 1 emerald/16s
 */
public enum GeneratorTier {

    TIER_0("Default",  1,  0, 4,  0,  0,  0, -1,  0, -1),
    TIER_1("Tier I",   2,  0, 2,  1,  2, -1, -1,  0, -1),
    TIER_2("Tier II",  4,  0, 1,  2,  1, -1,  6,  0, -1),
    TIER_3("Tier III", 8,  0, 1,  4,  1, -1,  3,  1, 16);

    // iron per iron-tick, iron-tick period (seconds), gold per gold-tick, gold-tick period,
    // diamonds per diamond-tick, diamond-tick period (-1 = disabled),
    // emeralds per emerald-tick, emerald-tick period (-1 = disabled)
    private final String displayName;
    private final int ironPerTick;
    private final int ironExtra;          // extra iron added alongside gold tick
    private final int ironTickSeconds;
    private final int goldPerTick;
    private final int goldExtra;          // extra iron added alongside gold tick (tier1)
    private final int goldTickSeconds;
    private final int diamondTickSeconds; // -1 disabled
    private final int emeraldPerTick;
    private final int emeraldTickSeconds; // -1 disabled

    GeneratorTier(String displayName,
                  int ironPerTick, int ironExtra, int ironTickSeconds,
                  int goldPerTick, int goldExtra, int goldTickSeconds,
                  int diamondTickSeconds,
                  int emeraldPerTick, int emeraldTickSeconds) {
        this.displayName        = displayName;
        this.ironPerTick        = ironPerTick;
        this.ironExtra          = ironExtra;
        this.ironTickSeconds    = ironTickSeconds;
        this.goldPerTick        = goldPerTick;
        this.goldExtra          = goldExtra;
        this.goldTickSeconds    = goldTickSeconds;
        this.diamondTickSeconds = diamondTickSeconds;
        this.emeraldPerTick     = emeraldPerTick;
        this.emeraldTickSeconds = emeraldTickSeconds;
    }

    public String getDisplayName()          { return displayName; }
    public int getIronPerTick()             { return ironPerTick; }
    public int getIronExtra()               { return ironExtra; }
    public int getIronTickSeconds()         { return ironTickSeconds; }
    public int getGoldPerTick()             { return goldPerTick; }
    public int getGoldExtra()               { return goldExtra; }
    public int getGoldTickSeconds()         { return goldTickSeconds; }
    public int getDiamondTickSeconds()      { return diamondTickSeconds; }
    public int getEmeraldPerTick()          { return emeraldPerTick; }
    public int getEmeraldTickSeconds()      { return emeraldTickSeconds; }
}
