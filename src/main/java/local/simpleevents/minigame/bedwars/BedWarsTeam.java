package local.simpleevents.minigame.bedwars;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;

/**
 * All thirteen wool-colour teams available in Bed Wars.
 *
 * When setting up a game, administrators add players to only the teams
 * they need — unused teams (no members) are skipped automatically.
 */
public enum BedWarsTeam {

    WHITE     (ChatColor.WHITE,        DyeColor.WHITE,      Material.WHITE_WOOL,      Material.WHITE_BED),
    RED       (ChatColor.RED,          DyeColor.RED,        Material.RED_WOOL,        Material.RED_BED),
    BLUE      (ChatColor.BLUE,         DyeColor.BLUE,       Material.BLUE_WOOL,       Material.BLUE_BED),
    GREEN     (ChatColor.GREEN,        DyeColor.LIME,       Material.LIME_WOOL,       Material.LIME_BED),
    ORANGE    (ChatColor.GOLD,         DyeColor.ORANGE,     Material.ORANGE_WOOL,     Material.ORANGE_BED),
    PINK      (ChatColor.LIGHT_PURPLE, DyeColor.PINK,       Material.PINK_WOOL,       Material.PINK_BED),
    PURPLE    (ChatColor.DARK_PURPLE,  DyeColor.PURPLE,     Material.PURPLE_WOOL,     Material.PURPLE_BED),
    BLACK     (ChatColor.DARK_GRAY,    DyeColor.BLACK,      Material.BLACK_WOOL,      Material.BLACK_BED),
    BROWN     (ChatColor.DARK_RED,     DyeColor.BROWN,      Material.BROWN_WOOL,      Material.BROWN_BED),
    YELLOW    (ChatColor.YELLOW,       DyeColor.YELLOW,     Material.YELLOW_WOOL,     Material.YELLOW_BED),
    DARK_GREEN(ChatColor.DARK_GREEN,   DyeColor.GREEN,      Material.GREEN_WOOL,      Material.GREEN_BED),
    LIGHT_GREY(ChatColor.GRAY,         DyeColor.LIGHT_GRAY, Material.LIGHT_GRAY_WOOL, Material.LIGHT_GRAY_BED),
    DARK_GREY (ChatColor.DARK_GRAY,    DyeColor.GRAY,       Material.GRAY_WOOL,       Material.GRAY_BED);

    private final ChatColor color;
    private final DyeColor dyeColor;
    private final Material woolMaterial;
    private final Material bedMaterial;

    BedWarsTeam(ChatColor color, DyeColor dyeColor, Material woolMaterial, Material bedMaterial) {
        this.color = color;
        this.dyeColor = dyeColor;
        this.woolMaterial = woolMaterial;
        this.bedMaterial = bedMaterial;
    }

    public ChatColor getColor()         { return color; }
    public DyeColor getDyeColor()       { return dyeColor; }
    public Material getWoolMaterial()   { return woolMaterial; }
    public Material getBedMaterial()    { return bedMaterial; }

    /** Returns a chat-coloured, human-readable name, e.g. "§2Dark Green". */
    public String getDisplayName() {
        String[] parts = name().split("_");
        StringBuilder sb = new StringBuilder(color.toString());
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            sb.append(parts[i].substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
