package local.simpleevents.minigame;

import org.bukkit.Material;

/**
 * Each minigame available in SimpleEvents.
 *
 * teamSize     – number of players per team
 * tokensFirst  – Minigame Tokens awarded to 1st-place team
 * tokensSecond – tokens for 2nd place
 * tokensThird  – tokens for 3rd place
 * tokensOther  – tokens for everyone else (4th, 5th, …)
 */
public enum MinigameType {

    BED_WARS(
            "bed_wars",
            "§b§lBed Wars",
            Material.RED_BED,
            3,
            "In Bed Wars, you and 2 teammates must battle for\ncontrol of the Cosmic Dreamscape.\n\n"
                    + "Gather Gold, Iron, Diamonds and Emeralds to purchase\nitems or upgrades for you or your team.\n\n"
                    + "Every team has a respawn bed on their island. If your\nteam's bed is destroyed, you can no longer respawn.\n"
                    + "Defend your bed to survive and destroy other team's\nbeds then kill them to win!",
            10, 4, 3, 2, 1,
            106, 53, 26,
            new String[]{
                    "§a§lFinal Kills: §f25 Minigame Points",
                    "§a§lAssisting Final Kills: §f10 Minigame Points",
                    "§a§lHitting Players: §f1 Minigame Point",
                    "§7  (maximum 3 points per player hit)",
                    "§a§lBed Destruction: §f15 Minigame Points"
            }
    ),

    SKY_WARS(
            "sky_wars",
            "§e§lSky Wars",
            Material.ELYTRA,
            3,
            "A classic Minecraft minigame where teams battle\nto the death suspended on islands in the sky!\n\n"
                    + "Teams of 3 spawn on one of 16 starter islands.\nLoot chests located around the map, or falling\n"
                    + "from the sky every 15s on Envoy Sky Wars, in\norder to gain an advantage against other teams.\n"
                    + "Chests on the center island contain better loot!\n\n"
                    + "After 5 minutes, the border will begin to shrink\nforcing all players towards the center island.\n\n"
                    + "Be the last team standing in order to win!",
            10, 4, 3, 2, 1,
            106, 53, 26,
            new String[]{
                    "§a§lKills: §f25 Minigame Points",
                    "§a§lAssisting Kills: §f10 Minigame Points",
                    "§a§lHitting Players: §f1 Minigame Point",
                    "§7  (maximum 3 points per player hit)",
                    "§a§lLooting Chests: §f10 Minigame Points",
                    "§7  (Envoy Sky Wars: 1 Minigame Point)"
            }
    ),

    BATTLE_ROYALE(
            "battle_royale",
            "§c§lBattle Royale",
            Material.IRON_SWORD,
            3,
            "An adrenaline pumping PvP event that will\nput your MLG Minecraft skills to the test!\n\n"
                    + "All participants…\n"
                    + "§8» §7Are grouped into §cRANDOM §7teams of 3\n"
                    + "§8» §7Start with the same inventory/equipment\n"
                    + "§8» §7Loot envoy chests in the area for upgrades\n"
                    + "§8» §7Lose nothing on death!",
            10, 4, 3, 2, 1,
            106, 53, 26,
            new String[]{
                    "§a§lKills: §f25 Minigame Points",
                    "§a§lAssisting Kills: §f10 Minigame Points",
                    "§a§lHitting Players: §f1 Minigame Point",
                    "§7  (maximum 3 points per player hit)",
                    "§a§lLooting Envoy Chests: §f1 Minigame Point"
            }
    );

    private final String key;
    private final String displayName;
    private final Material icon;
    private final int teamSize;
    private final String description;

    // Token rewards (placed)
    private final int tokensFirst;
    private final int tokensSecond;
    private final int tokensThird;
    private final int tokensFourth;
    private final int tokensFifth;

    // Core Chunk Points for top 3
    private final int coreChunkFirst;
    private final int coreChunkSecond;
    private final int coreChunkThird;

    private final String[] pointRewards;

    MinigameType(String key, String displayName, Material icon, int teamSize,
                 String description,
                 int tokensFirst, int tokensSecond, int tokensThird, int tokensFourth, int tokensFifth,
                 int coreChunkFirst, int coreChunkSecond, int coreChunkThird,
                 String[] pointRewards) {
        this.key = key;
        this.displayName = displayName;
        this.icon = icon;
        this.teamSize = teamSize;
        this.description = description;
        this.tokensFirst = tokensFirst;
        this.tokensSecond = tokensSecond;
        this.tokensThird = tokensThird;
        this.tokensFourth = tokensFourth;
        this.tokensFifth = tokensFifth;
        this.coreChunkFirst = coreChunkFirst;
        this.coreChunkSecond = coreChunkSecond;
        this.coreChunkThird = coreChunkThird;
        this.pointRewards = pointRewards;
    }

    public String getKey()          { return key; }
    public String getDisplayName()  { return displayName; }
    public Material getIcon()       { return icon; }
    public int getTeamSize()        { return teamSize; }
    public String getDescription()  { return description; }
    public int getTokensFirst()     { return tokensFirst; }
    public int getTokensSecond()    { return tokensSecond; }
    public int getTokensThird()     { return tokensThird; }
    public int getTokensFourth()    { return tokensFourth; }
    public int getTokensFifth()     { return tokensFifth; }
    public int getCoreChunkFirst()  { return coreChunkFirst; }
    public int getCoreChunkSecond() { return coreChunkSecond; }
    public int getCoreChunkThird()  { return coreChunkThird; }
    public String[] getPointRewards() { return pointRewards; }

    /** Returns the token reward for the given placement (1-based). Returns 0 for placement > 5. */
    public int getTokensForPlacement(int place) {
        return switch (place) {
            case 1 -> tokensFirst;
            case 2 -> tokensSecond;
            case 3 -> tokensThird;
            case 4 -> tokensFourth;
            case 5 -> tokensFifth;
            default -> 0;
        };
    }

    /** Returns the Core Chunk Points for top-3 placements. 0 otherwise. */
    public int getCoreChunkForPlacement(int place) {
        return switch (place) {
            case 1 -> coreChunkFirst;
            case 2 -> coreChunkSecond;
            case 3 -> coreChunkThird;
            default -> 0;
        };
    }

    public static MinigameType fromKey(String key) {
        for (MinigameType t : values()) {
            if (t.key.equalsIgnoreCase(key)) return t;
        }
        return null;
    }
}
