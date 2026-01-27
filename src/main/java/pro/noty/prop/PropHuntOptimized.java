package pro.noty.prop;

import org.bukkit.plugin.java.JavaPlugin;
import pro.noty.prop.arena.ArenaManager;
import pro.noty.prop.disguise.DisguiseManager;
import pro.noty.prop.game.GameManager;
import pro.noty.prop.listeners.*;

public final class PropHuntOptimized extends JavaPlugin {

    private ArenaManager arenaManager;
    private DisguiseManager disguiseManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.arenaManager = new ArenaManager(this);
        this.disguiseManager = new DisguiseManager(this);
        this.gameManager = new GameManager(this, disguiseManager);

        getServer().getPluginManager().registerEvents(new SpyglassMorphListener(this, gameManager), this);
        getServer().getPluginManager().registerEvents(new GameListener(this, gameManager), this);
        getServer().getPluginManager().registerEvents(new ArenaProtectionListener(gameManager), this);

        getCommand("mb").setExecutor(new pro.noty.prop.commands.MainCommand(this));
        getCommand("mb").setTabCompleter(new pro.noty.prop.commands.MainCommand(this));
    }

    @Override
    public void onDisable() {
        disguiseManager.cleanupAll();
    }

    public ArenaManager getArenaManager() { return arenaManager; }
    public GameManager getGameManager() { return gameManager; }
    public DisguiseManager getDisguiseManager() { return disguiseManager; }

    public void reloadPlugin() {
        reloadConfig();
        arenaManager.loadArenas();
    }
}
