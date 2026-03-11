package local.simpleevents.minigame.bedwars;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Utility class that builds all Bed Wars items with correct unbreakable flag,
 * enchants, and custom names.
 */
public final class BedWarsItems {

    private BedWarsItems() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Sword tiers  (player keeps until death; loses on death)
    // ─────────────────────────────────────────────────────────────────────────

    public static ItemStack woodSword(int sharpnessBonus) {
        return makeSword(org.bukkit.Material.WOODEN_SWORD, sharpnessBonus);
    }
    public static ItemStack stoneSword(int sharpnessBonus) {
        return makeSword(org.bukkit.Material.STONE_SWORD, sharpnessBonus);
    }
    public static ItemStack ironSword(int sharpnessBonus) {
        return makeSword(org.bukkit.Material.IRON_SWORD, sharpnessBonus);
    }
    public static ItemStack diamondSword(int sharpnessBonus) {
        return makeSword(org.bukkit.Material.DIAMOND_SWORD, sharpnessBonus);
    }

    private static ItemStack makeSword(org.bukkit.Material mat, int sharpnessBonus) {
        ItemStack item = new ItemStack(mat);
        applyUnbreakable(item);
        if (sharpnessBonus > 0) item.addUnsafeEnchantment(Enchantment.SHARPNESS, sharpnessBonus);
        return item;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pickaxe tiers  (player keeps best they had before death; downgrade on death)
    //   diamond → iron → stone → wooden (persistent after purchase)
    // ─────────────────────────────────────────────────────────────────────────

    public static ItemStack woodPickaxe(int effBonus) {
        return makePick(org.bukkit.Material.WOODEN_PICKAXE, effBonus);
    }
    public static ItemStack stonePickaxe(int effBonus) {
        return makePick(org.bukkit.Material.STONE_PICKAXE, effBonus);
    }
    public static ItemStack ironPickaxe(int effBonus) {
        return makePick(org.bukkit.Material.IRON_PICKAXE, effBonus);
    }
    public static ItemStack diamondPickaxe(int effBonus) {
        return makePick(org.bukkit.Material.DIAMOND_PICKAXE, effBonus);
    }

    private static ItemStack makePick(org.bukkit.Material mat, int effBonus) {
        ItemStack item = new ItemStack(mat);
        applyUnbreakable(item);
        if (effBonus > 0) item.addUnsafeEnchantment(Enchantment.EFFICIENCY, effBonus);
        return item;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Axe tiers  (same persistence rules as pickaxe)
    // ─────────────────────────────────────────────────────────────────────────

    public static ItemStack woodAxe(int effBonus) {
        return makeAxe(org.bukkit.Material.WOODEN_AXE, effBonus);
    }
    public static ItemStack stoneAxe(int effBonus) {
        return makeAxe(org.bukkit.Material.STONE_AXE, effBonus);
    }
    public static ItemStack ironAxe(int effBonus) {
        return makeAxe(org.bukkit.Material.IRON_AXE, effBonus);
    }
    public static ItemStack diamondAxe(int effBonus) {
        return makeAxe(org.bukkit.Material.DIAMOND_AXE, effBonus);
    }

    private static ItemStack makeAxe(org.bukkit.Material mat, int effBonus) {
        ItemStack item = new ItemStack(mat);
        applyUnbreakable(item);
        if (effBonus > 0) item.addUnsafeEnchantment(Enchantment.EFFICIENCY, effBonus);
        return item;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gear (armour) — player keeps permanently; upgrades replace old tier
    // ─────────────────────────────────────────────────────────────────────────

    public static ItemStack leatherHelmet(int protBonus) {
        return makeArmour(org.bukkit.Material.LEATHER_HELMET, protBonus);
    }
    public static ItemStack leatherChestplate(int protBonus) {
        return makeArmour(org.bukkit.Material.LEATHER_CHESTPLATE, protBonus);
    }
    public static ItemStack leatherLeggings(int protBonus) {
        return makeArmour(org.bukkit.Material.LEATHER_LEGGINGS, protBonus);
    }
    public static ItemStack leatherBoots(int protBonus) {
        return makeArmour(org.bukkit.Material.LEATHER_BOOTS, protBonus);
    }

    public static ItemStack chainHelmet(int protBonus)      { return makeArmour(org.bukkit.Material.CHAINMAIL_HELMET, protBonus); }
    public static ItemStack chainChestplate(int protBonus)  { return makeArmour(org.bukkit.Material.CHAINMAIL_CHESTPLATE, protBonus); }
    public static ItemStack chainLeggings(int protBonus)    { return makeArmour(org.bukkit.Material.CHAINMAIL_LEGGINGS, protBonus); }
    public static ItemStack chainBoots(int protBonus)       { return makeArmour(org.bukkit.Material.CHAINMAIL_BOOTS, protBonus); }

    public static ItemStack ironHelmet(int protBonus)       { return makeArmour(org.bukkit.Material.IRON_HELMET, protBonus); }
    public static ItemStack ironChestplate(int protBonus)   { return makeArmour(org.bukkit.Material.IRON_CHESTPLATE, protBonus); }
    public static ItemStack ironLeggings(int protBonus)     { return makeArmour(org.bukkit.Material.IRON_LEGGINGS, protBonus); }
    public static ItemStack ironBoots(int protBonus)        { return makeArmour(org.bukkit.Material.IRON_BOOTS, protBonus); }

    public static ItemStack diamondHelmet(int protBonus)    { return makeArmour(org.bukkit.Material.DIAMOND_HELMET, protBonus); }
    public static ItemStack diamondChestplate(int protBonus){ return makeArmour(org.bukkit.Material.DIAMOND_CHESTPLATE, protBonus); }
    public static ItemStack diamondLeggings(int protBonus)  { return makeArmour(org.bukkit.Material.DIAMOND_LEGGINGS, protBonus); }
    public static ItemStack diamondBoots(int protBonus)     { return makeArmour(org.bukkit.Material.DIAMOND_BOOTS, protBonus); }

    private static ItemStack makeArmour(org.bukkit.Material mat, int protBonus) {
        ItemStack item = new ItemStack(mat);
        applyUnbreakable(item);
        if (protBonus > 0) item.addUnsafeEnchantment(Enchantment.PROTECTION, protBonus);
        return item;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shears
    // ─────────────────────────────────────────────────────────────────────────

    public static ItemStack shears() {
        ItemStack item = new ItemStack(org.bukkit.Material.SHEARS);
        applyUnbreakable(item);
        item.addUnsafeEnchantment(Enchantment.EFFICIENCY, 1);
        return item;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bow
    // ─────────────────────────────────────────────────────────────────────────

    public static ItemStack bow(int powerLevel) {
        ItemStack item = new ItemStack(org.bukkit.Material.BOW);
        applyUnbreakable(item);
        if (powerLevel > 0) item.addUnsafeEnchantment(Enchantment.POWER, powerLevel);
        return item;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    public static void applyUnbreakable(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
    }

    /** Returns the "tier index" for a weapon/tool so we can compare levels. */
    public static int swordTier(org.bukkit.Material mat) {
        return switch (mat) {
            case WOODEN_SWORD  -> 0;
            case STONE_SWORD   -> 1;
            case IRON_SWORD    -> 2;
            case DIAMOND_SWORD -> 3;
            default            -> -1;
        };
    }

    public static int pickaxeTier(org.bukkit.Material mat) {
        return switch (mat) {
            case WOODEN_PICKAXE  -> 0;
            case STONE_PICKAXE   -> 1;
            case IRON_PICKAXE    -> 2;
            case DIAMOND_PICKAXE -> 3;
            default              -> -1;
        };
    }

    public static int axeTier(org.bukkit.Material mat) {
        return switch (mat) {
            case WOODEN_AXE  -> 0;
            case STONE_AXE   -> 1;
            case IRON_AXE    -> 2;
            case DIAMOND_AXE -> 3;
            default          -> -1;
        };
    }

    public static boolean isSword(org.bukkit.Material mat)   { return swordTier(mat) >= 0; }
    public static boolean isPickaxe(org.bukkit.Material mat) { return pickaxeTier(mat) >= 0; }
    public static boolean isAxe(org.bukkit.Material mat)     { return axeTier(mat) >= 0; }
    public static boolean isArmour(org.bukkit.Material mat) {
        String name = mat.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
            || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }
}
