package pro.noty.prop;

import org.bukkit.plugin.java.JavaPlugin;
import pro.noty.prop.api.PropHuntAPI;
import pro.noty.prop.api.PropHuntAPIImpl;
import pro.noty.prop.arena.ArenaManager;
import pro.noty.prop.disguise.DisguiseManager;
import pro.noty.prop.game.GameManager;
import pro.noty.prop.game.HunterVoteManager;
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
        this.propHuntAPI = new PropHuntAPIImpl(this);


        getServer().getPluginManager().registerEvents(new SpyglassMorphListener(this, gameManager), this);
        getServer().getPluginManager().registerEvents(new GameListener(this, gameManager), this);
        getServer().getPluginManager().registerEvents(new ArenaProtectionListener(gameManager), this);

        getCommand("mb").setExecutor(new pro.noty.prop.commands.MainCommand(this));
        getCommand("mb").setTabCompleter(new pro.noty.prop.commands.MainCommand(this));
        getServer().getPluginManager().registerEvents(new HunterVoteManager(this), this);

    }

    @Override
    public void onDisable() {
        disguiseManager.cleanupAll();
    }

    public ArenaManager getArenaManager() { return arenaManager; }
    public GameManager getGameManager() { return this.gameManager; }
   // public GameManager getGameManager() { return gameManager; }
    public DisguiseManager getDisguiseManager() { return disguiseManager; }
    private PropHuntAPI propHuntAPI;


    public void reloadPlugin() {
        reloadConfig();
        arenaManager.loadArenas();
    }
    public PropHuntAPI getAPI() {
        return propHuntAPI;
    }

}
