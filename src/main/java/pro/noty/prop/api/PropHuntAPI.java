package pro.noty.prop.api;

import org.bukkit.entity.Player;
import pro.noty.prop.arena.Arena;

public interface PropHuntAPI {

    boolean isInGame(Player player);
    boolean isHunter(Player player);
    boolean isSeeker(Player player);
    boolean isSpectator(Player player);

    void joinGame(Player player, Arena arena);
    void leaveGame(Player player);

    void disguisePlayer(Player player);
    void undisguisePlayer(Player player);

    void startGame(Arena arena);
    void stopGame();

    Arena getPlayerArena(Player player);
}
