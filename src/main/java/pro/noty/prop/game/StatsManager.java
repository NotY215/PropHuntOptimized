package pro.noty.prop.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import java.util.*;

public class StatsManager {

    private final Map<UUID, Integer> wins = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();

    public void addWin(UUID id) { wins.put(id, wins.getOrDefault(id, 0) + 1); }
    public void addKill(UUID id) { kills.put(id, kills.getOrDefault(id, 0) + 1); }
    public int getWins(UUID id) { return wins.getOrDefault(id, 0); }
    public int getKills(UUID id) { return kills.getOrDefault(id, 0); }

    public void updateScoreboard(Player player, String state, int time, int seekersLeft, String arenaName) {

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective obj = board.registerNewObjective("phunt", "dummy", "§6§lPROP HUNT");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.getScore("§7Arena: §e" + arenaName).setScore(8);
        obj.getScore("§7Status: §e" + state).setScore(7);
        obj.getScore("§7Time: §a" + formatTime(time)).setScore(6);
        obj.getScore("§fProps Left: §c" + seekersLeft).setScore(5);
        obj.getScore("§fKills: §e" + getKills(player.getUniqueId())).setScore(4);
        obj.getScore("§fWins: §e" + getWins(player.getUniqueId())).setScore(3);

        player.setScoreboard(board);
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
    /* ========================================================= */
    /* ===================== LEADERBOARDS ======================= */
    /* ========================================================= */

    public List<Map.Entry<UUID, Integer>> getTopWins(int limit) {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(wins.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Map.Entry<UUID, Integer>> getTopKills(int limit) {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(kills.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        return list.subList(0, Math.min(limit, list.size()));
    }

}
