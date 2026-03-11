package local.simpleevents;

import local.simpleevents.boss.BossListener;
import local.simpleevents.boss.BossManager;
import local.simpleevents.command.DungeonsCommand;
import local.simpleevents.command.EventsCommand;
import local.simpleevents.dungeon.DungeonGui;
import local.simpleevents.dungeon.DungeonListener;
import local.simpleevents.dungeon.DungeonManager;
import local.simpleevents.minigame.MinigameCommand;
import local.simpleevents.minigame.MinigameGui;
import local.simpleevents.minigame.MinigameListener;
import local.simpleevents.minigame.MinigameManager;
import local.simpleevents.minigame.VoidChunkGenerator;
import org.bukkit.generator.ChunkGenerator;
import local.simpleevents.minigame.bedwars.BedWarsCommand;
import local.simpleevents.minigame.bedwars.BedWarsGame;
import local.simpleevents.minigame.bedwars.BedWarsListener;
import local.simpleevents.minigame.bedwars.BedWarsLocationStore;
import local.simpleevents.minigame.bedwars.BedWarsNpcManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleEventsPlugin extends JavaPlugin {

    private static SimpleEventsPlugin instance;

    private BossManager bossManager;
    private DungeonManager dungeonManager;
    private DungeonGui dungeonGui;
    private MinigameManager minigameManager;
    private MinigameGui minigameGui;
    private BedWarsGame bedWarsGame;
    private BedWarsNpcManager bedWarsNpcManager;
    private BedWarsLocationStore bedWarsLocationStore;

    public static SimpleEventsPlugin getInstance() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Boss system
        bossManager = new BossManager(this);
        bossManager.start();
        getServer().getPluginManager().registerEvents(
                new BossListener(this, bossManager), this);

        // Dungeon system
        dungeonManager = new DungeonManager(this);
        dungeonGui = new DungeonGui(dungeonManager);
        getServer().getPluginManager().registerEvents(
                new DungeonListener(this, dungeonManager, dungeonGui), this);

        // Minigame system
        minigameManager = new MinigameManager(this);
        minigameGui = new MinigameGui(minigameManager);
        getServer().getPluginManager().registerEvents(
                new MinigameListener(this, minigameManager, minigameGui), this);

        // Commands
        EventsCommand eventsCommand = new EventsCommand(this, bossManager, dungeonManager, dungeonGui);
        getCommand("events").setExecutor(eventsCommand);
        getCommand("events").setTabCompleter(eventsCommand);

        DungeonsCommand dungeonsCommand = new DungeonsCommand(dungeonGui);
        getCommand("dungeons").setExecutor(dungeonsCommand);
        getCommand("dungeons").setTabCompleter(dungeonsCommand);

        MinigameCommand minigameCommand = new MinigameCommand(this, minigameManager, minigameGui);
        getCommand("minigame").setExecutor(minigameCommand);
        getCommand("minigame").setTabCompleter(minigameCommand);

        // Bed Wars system
        bedWarsLocationStore = new BedWarsLocationStore(this);
        bedWarsGame = new BedWarsGame(this, bedWarsLocationStore);
        bedWarsNpcManager = new BedWarsNpcManager(this, bedWarsGame);
        BedWarsListener bedWarsListener = new BedWarsListener(this, bedWarsGame, bedWarsNpcManager);
        getServer().getPluginManager().registerEvents(bedWarsListener, this);
        BedWarsCommand bedWarsCommand = new BedWarsCommand(this, bedWarsGame, bedWarsNpcManager, bedWarsLocationStore);
        getCommand("bw").setExecutor(bedWarsCommand);
        getCommand("bw").setTabCompleter(bedWarsCommand);

        getLogger().info("SimpleEvents enabled — Boss, Dungeon, Minigame & Bed Wars systems active.");
    }

    @Override
    public void onDisable() {
        if (bossManager != null) bossManager.stop();
        if (dungeonManager != null) dungeonManager.shutdown();
        if (minigameManager != null) minigameManager.shutdown();
        if (bedWarsGame != null) bedWarsGame.stop();
        if (bedWarsNpcManager != null) bedWarsNpcManager.removeAll();
        getLogger().info("SimpleEvents disabled.");
    }

    public BossManager getBossManager()          { return bossManager; }
    public DungeonManager getDungeonManager()    { return dungeonManager; }
    public DungeonGui getDungeonGui()            { return dungeonGui; }
    public MinigameManager getMinigameManager()  { return minigameManager; }
    public MinigameGui getMinigameGui()          { return minigameGui; }
    public BedWarsGame getBedWarsGame()                   { return bedWarsGame; }
    public BedWarsNpcManager getBedWarsNpcManager()       { return bedWarsNpcManager; }
    public BedWarsLocationStore getBedWarsLocationStore() { return bedWarsLocationStore; }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new VoidChunkGenerator();
    }
}
