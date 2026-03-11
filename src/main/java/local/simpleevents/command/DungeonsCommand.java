package local.simpleevents.command;

import local.simpleevents.dungeon.DungeonGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /dungeons — opens the dungeon browser GUI for any player with permission.
 */
public class DungeonsCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "simpleevents.dungeon.gui";

    private final DungeonGui gui;

    public DungeonsCommand(DungeonGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }
        gui.openBrowser(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
