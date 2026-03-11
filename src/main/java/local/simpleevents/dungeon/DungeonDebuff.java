package local.simpleevents.dungeon;

/**
 * Possible debuffs that can be randomly applied during a Heroic dungeon run.
 * On Heroic mode, 2 of these are chosen at random.
 */
public enum DungeonDebuff {

    NO_ITEM_SKINS("No Item Skins"),
    NO_TELEPORT_BOWS("No Teleportation Bows"),
    NO_ARMOR_SET_BONUSES("No Armor Set Bonuses"),
    NO_INVENTORY_PETS("No Inventory Pets"),
    NO_ENDER_PEARLS("No Ender Pearls"),
    NO_POTIONS("No Potions"),
    NO_BOW_DAMAGE("No Bow Damage to Bosses"),
    MAX_HIT_CAP("Players Deal a Max of 30 Damage Per Hit");

    private final String label;

    DungeonDebuff(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
