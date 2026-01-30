package pro.noty.prop.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pro.noty.prop.PropHuntOptimized;
import pro.noty.prop.arena.Arena;
import pro.noty.prop.disguise.DisguiseManager;
import pro.noty.prop.game.GameManager;

import java.util.UUID;

public class PropHuntAPIImpl implements PropHuntAPI {

    private final GameManager gameManager;
    private final DisguiseManager disguiseManager;

    public PropHuntAPIImpl(PropHuntOptimized plugin) {
        this.gameManager = plugin.getGameManager();
        this.disguiseManager = plugin.getDisguiseManager();
    }

    @Override public boolean isInGame(Player p) { return gameManager.isInGame(p); }
    @Override public boolean isHunter(Player p) { return gameManager.isHunter(p); }
    @Override public boolean isSeeker(Player p) { return gameManager.isSeeker(p); }
    @Override public boolean isSpectator(Player p) { return !isHunter(p) && !isSeeker(p) && isInGame(p); }

    @Override public void joinGame(Player p, Arena arena) { gameManager.joinArena(p, arena); }
    @Override public void leaveGame(Player p) { gameManager.leaveGame(p); }

    @Override public void disguisePlayer(Player p) { /* external plugins must handle material */ }
    @Override public void undisguisePlayer(Player p) { disguiseManager.removeDisguise(p); }

    @Override
    public void startGame(Arena arena) {
        for (UUID uuid : arena.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                gameManager.joinArena(p, arena);
            }
        }
    }


    @Override public void stopGame() {
        gameManager.forceStop();
    }

    @Override public Arena getPlayerArena(Player player) {
        return gameManager.getArena();
    }
}
