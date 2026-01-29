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
    public int getWins(UUID id) { return wins.getOrDefault(id, 0); }
    public int getKills(UUID id) { return kills.getOrDefault(id, 0); }

    public void updateScoreboard(Player player, String state, int time, int seekersLeft, String arenaName) {
        Scoreboard board = player.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
        }

        Objective obj = board.getObjective("phunt");
        if (obj == null) {
            obj = board.registerNewObjective("phunt", "dummy", "§6§lPROP HUNT");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        board.getEntries().forEach(board::resetScores);

        obj.getScore("§7----------------").setScore(13);
        obj.getScore("§fArena: §e" + arenaName).setScore(12);
        obj.getScore("§fStatus: §e" + state).setScore(11);
        obj.getScore("§fTime: §a" + formatTime(time)).setScore(10);
        obj.getScore("§1 ").setScore(9);
        obj.getScore("§fProps Left: §c" + seekersLeft).setScore(8);
        obj.getScore("§fYour Health: §4" + (int)player.getHealth() + "❤").setScore(7);
        obj.getScore("§2 ").setScore(6);
        obj.getScore("§fKills: §e" + getKills(player.getUniqueId())).setScore(5);
        obj.getScore("§fWins: §e" + getWins(player.getUniqueId())).setScore(4);
        obj.getScore("§7---------------- ").setScore(3);

        player.setScoreboard(board);
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}