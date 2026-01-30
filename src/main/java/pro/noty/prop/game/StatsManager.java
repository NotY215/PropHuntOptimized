package pro.noty.prop.game;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StatsManager {

    private final Map<UUID, Integer> wins = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();

    private final File file = new File("plugins/PropHuntOptimized", "stats.yml");
    private final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

    public StatsManager() {
        loadStats();
    }

    /* ================= LOAD & SAVE ================= */

    private void loadStats() {
        if (!file.exists()) return;

        for (String key : cfg.getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            wins.put(uuid, cfg.getInt(key + ".wins"));
            kills.put(uuid, cfg.getInt(key + ".kills"));
        }
    }

    private void saveStats() {
        for (UUID uuid : wins.keySet()) {
            cfg.set(uuid.toString() + ".wins", wins.getOrDefault(uuid, 0));
            cfg.set(uuid.toString() + ".kills", kills.getOrDefault(uuid, 0));
        }
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    /* ================= ADD STATS ================= */

    public void addWin(UUID id) {
        wins.put(id, wins.getOrDefault(id, 0) + 1);
        saveStats();
    }

    public void addKill(UUID id) {
        kills.put(id, kills.getOrDefault(id, 0) + 1);
        saveStats();
    }

    public int getWins(UUID id) { return wins.getOrDefault(id, 0); }
    public int getKills(UUID id) { return kills.getOrDefault(id, 0); }

    /* ================= LEADERBOARD ================= */

    public List<Map.Entry<UUID, Integer>> getTopWins(int limit) {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(wins.entrySet());
        list.sort((a,b) -> b.getValue() - a.getValue());
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Map.Entry<UUID, Integer>> getTopKills(int limit) {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(kills.entrySet());
        list.sort((a,b) -> b.getValue() - a.getValue());
        return list.subList(0, Math.min(limit, list.size()));
    }

    /* ================= SCOREBOARD ================= */

    public void updateScoreboard(Player player, String state, int time, int seekersLeft, String arenaName) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective obj = board.registerNewObjective("phunt", "dummy", "§6§lPROP HUNT");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.getScore("§7Arena: §e" + arenaName).setScore(10);
        obj.getScore("§7State: §e" + state).setScore(9);
        obj.getScore("§7Time: §a" + formatTime(time)).setScore(8);
        obj.getScore("§7Props Left: §c" + seekersLeft).setScore(7);
        obj.getScore("§7Kills: §e" + getKills(player.getUniqueId())).setScore(6);
        obj.getScore("§7Wins: §e" + getWins(player.getUniqueId())).setScore(5);

        player.setScoreboard(board);
    }

    private String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
