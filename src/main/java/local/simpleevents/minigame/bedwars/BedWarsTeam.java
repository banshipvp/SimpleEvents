package local.simpleevents.minigame.bedwars;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;

/**
 * The four team colours used in Bed Wars.
 */
public enum BedWarsTeam {

    RED  (ChatColor.RED,    DyeColor.RED,    Material.RED_WOOL,    Material.RED_BED),
    BLUE (ChatColor.BLUE,   DyeColor.BLUE,   Material.BLUE_WOOL,   Material.BLUE_BED),
    GREEN(ChatColor.GREEN,  DyeColor.GREEN,  Material.GREEN_WOOL,  Material.GREEN_BED),
    YELLOW(ChatColor.YELLOW,DyeColor.YELLOW, Material.YELLOW_WOOL, Material.YELLOW_BED);

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
    public String getDisplayName()      { return color.toString() + name().charAt(0) + name().substring(1).toLowerCase(); }
}
