package pro.noty.prop;

import org.bukkit.plugin.java.JavaPlugin;
import pro.noty.prop.arena.ArenaManager;
import pro.noty.prop.commands.MainCommand;
import pro.noty.prop.disguise.DisguiseManager;
import pro.noty.prop.game.GameManager;

public class PropHuntOptimized extends JavaPlugin {

    private static PropHuntOptimized instance;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private DisguiseManager disguiseManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        arenaManager = new ArenaManager(this);
        disguiseManager = new DisguiseManager(this);
        gameManager = new GameManager(this);

        // Commands
        MainCommand mainCommand = new MainCommand(this);
        getCommand("mb").setExecutor(mainCommand);
        getCommand("mb").setTabCompleter(mainCommand);

        // Events
        getServer().getPluginManager().registerEvents(gameManager, this);
    }
    @Override
    public void onDisable() {
        disguiseManager.cleanupAll();
    }


    public static PropHuntOptimized get() { return instance; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public GameManager getGameManager() { return gameManager; }
    public DisguiseManager getDisguiseManager() { return disguiseManager; }
    public void reloadPlugin() {
        reloadConfig();
        arenaManager.loadArenas();
    }

}
