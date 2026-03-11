package local.simpleevents.dungeon;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates and detects dungeon key items used to unlock dungeon portals.
 */
public class DungeonKey {

    static final String PDC_KEY = "simpleevents_dungeon_key";
    static final String PDC_HEROIC = "simpleevents_dungeon_heroic";

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private DungeonKey() {}

    public static ItemStack create(Plugin plugin, DungeonType type, boolean heroic) {
        ItemStack item = new ItemStack(heroic ? Material.NETHER_STAR : Material.TRIPWIRE_HOOK);
        ItemMeta meta = item.getItemMeta();

        String prefix = heroic ? "§c§l" : "§6§l";
        String mode = heroic ? " §c[HEROIC]" : "";
        meta.displayName(LEGACY.deserialize(prefix + stripColor(type.getDisplayName()) + " Key" + mode));

        List<Component> lore = new ArrayList<>();
        lore.add(LEGACY.deserialize("§7Grants access to:"));
        lore.add(LEGACY.deserialize("  §r" + type.getDisplayName()));
        if (heroic) {
            lore.add(LEGACY.deserialize("  §c§lHeroic Mode"));
        }
        lore.add(LEGACY.deserialize(""));
        lore.add(LEGACY.deserialize("§8» §7Tier " + type.ordinal() + " Dungeon"));
        if (type.getPrerequisite() != null) {
            lore.add(LEGACY.deserialize("§8» §7Unlocked via: §r" + type.getPrerequisite().getDisplayName()));
        }
        lore.add(LEGACY.deserialize(""));
        lore.add(LEGACY.deserialize("§eUse §7this key to enter the dungeon."));
        meta.lore(lore);

        // Glow effect on heroic keys
        if (heroic) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        NamespacedKey typeKey = new NamespacedKey(plugin, PDC_KEY);
        NamespacedKey heroicKey = new NamespacedKey(plugin, PDC_HEROIC);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.getKey());
        meta.getPersistentDataContainer().set(heroicKey, PersistentDataType.BYTE, heroic ? (byte) 1 : (byte) 0);

        item.setItemMeta(meta);
        return item;
    }

    /** Returns the DungeonType key string embedded in this item, or null if it isn't a dungeon key. */
    public static String getDungeonTypeKey(Plugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        NamespacedKey nk = new NamespacedKey(plugin, PDC_KEY);
        return item.getItemMeta().getPersistentDataContainer().get(nk, PersistentDataType.STRING);
    }

    public static boolean isHeroicKey(Plugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey nk = new NamespacedKey(plugin, PDC_HEROIC);
        Byte val = item.getItemMeta().getPersistentDataContainer().get(nk, PersistentDataType.BYTE);
        return val != null && val == 1;
    }

    public static boolean isDungeonKey(Plugin plugin, ItemStack item) {
        return getDungeonTypeKey(plugin, item) != null;
    }

    private static String stripColor(String s) {
        return s.replaceAll("§.", "");
    }
}
