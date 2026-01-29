package pro.noty.prop.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {
    private final Map<UUID, Integer> wins = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();

    public void addWin(UUID id) { wins.put(id, wins.getOrDefault(id, 0) + 1); }
    public void addKill(UUID id) { kills.put(id, kills.getOrDefault(id, 0) + 1); }

    public void updateScoreboard(Player player, String state, int time, int seekersLeft) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("phunt", "dummy", "§6§lPROP HUNT");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.getScore("§7----------------").setScore(10);
        obj.getScore("§fStatus: §e" + state).setScore(9);
        obj.getScore("§fTime: §a" + formatTime(time)).setScore(8);
        obj.getScore("§fSeekers: §c" + seekersLeft).setScore(7);
        obj.getScore("§1 ").setScore(6);
        obj.getScore("§fKills: §e" + kills.getOrDefault(player.getUniqueId(), 0)).setScore(5);
        obj.getScore("§fWins: §e" + wins.getOrDefault(player.getUniqueId(), 0)).setScore(4);
        obj.getScore("§7---------------- ").setScore(3);

        player.setScoreboard(board);
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
    public int getWins(UUID id) { return wins.getOrDefault(id, 0); }
    public int getKills(UUID id) { return kills.getOrDefault(id, 0); }
}