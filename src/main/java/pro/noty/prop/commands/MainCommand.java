package pro.noty.prop.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import pro.noty.prop.PropHuntOptimized;
import pro.noty.prop.arena.Arena;
import pro.noty.prop.game.GameManager;
import pro.noty.prop.game.StatsManager;

import java.util.ArrayList;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final PropHuntOptimized plugin;

    public MainCommand(PropHuntOptimized plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;

        if (args.length == 0) {
            showHelp(p);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!p.hasPermission("prophunt.admin")) {
                p.sendMessage("§cNo permission.");
                return true;
            }
            plugin.reloadPlugin();
            p.sendMessage("§aPropHunt reloaded!");
            return true;
        }

        if (args.length < 2) {
            showHelp(p);
            return true;
        }

        String arenaName = args[0];
        Arena arena = plugin.getArenaManager().getArena(arenaName);

        if (arena == null && !args[1].equalsIgnoreCase("create")) {
            p.sendMessage("§cArena not found.");
            return true;
        }

        GameManager gm = plugin.getGameManager();

        switch (args[1].toLowerCase()) {
            case "create":
                plugin.getArenaManager().createArena(arenaName);
                p.sendMessage("§aArena created!");
                break;

            case "pos1":
                arena.setPos1(p.getLocation());
                plugin.getArenaManager().saveArena(arena);
                p.sendMessage("§aPos1 set.");
                break;

            case "pos2":
                arena.setPos2(p.getLocation());
                plugin.getArenaManager().saveArena(arena);
                p.sendMessage("§aPos2 set.");
                break;

            case "setlobby":
                arena.setLobby(p.getLocation());
                plugin.getArenaManager().saveArena(arena);
                p.sendMessage("§aLobby set.");
                break;

            case "setspawn":
                arena.setSpawn(p.getLocation());
                plugin.getArenaManager().saveArena(arena);
                p.sendMessage("§aSpawn set.");
                break;

            case "join":
                gm.joinArena(p, arena);
                break;

            case "leave":
                gm.leaveGame(p);
                p.sendMessage("§eYou left the game.");
                break;

            case "leaderboard":
                showLeaderboard(p);
                break;

            default:
                showHelp(p);
                break;
        }

        return true;
    }

    /* ========================================================= */
    /* ===================== LEADERBOARD ======================== */
    /* ========================================================= */

    private void showLeaderboard(Player p) {
        StatsManager stats = plugin.getGameManager().getStatsManager();

        p.sendMessage("§6§l=== PROP HUNT LEADERBOARD ===");

        int place = 1;
        for (var entry : stats.getTopWins(10)) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            p.sendMessage("§e#" + place++ + " §f" + name + " §7- §a" + entry.getValue() + " Wins");
        }

        p.sendMessage("§7--- Top Killers ---");

        place = 1;
        for (var entry : stats.getTopKills(10)) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            p.sendMessage("§c#" + place++ + " §f" + name + " §7- §e" + entry.getValue() + " Kills");
        }
    }

    private void showHelp(Player p) {
        p.sendMessage("§6Prop Hunt Commands:");
        p.sendMessage("§e/mb <arena> create");
        p.sendMessage("§e/mb <arena> pos1");
        p.sendMessage("§e/mb <arena> pos2");
        p.sendMessage("§e/mb <arena> setlobby");
        p.sendMessage("§e/mb <arena> setspawn");
        p.sendMessage("§e/mb <arena> join");
        p.sendMessage("§e/mb <arena> leave");
        p.sendMessage("§e/mb <arena> leaderboard");
        p.sendMessage("§e/mb reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            list.add("reload");
            list.addAll(plugin.getArenaManager().getArenaMap().keySet());
        }
        else if (args.length == 2) {
            list.add("join");
            list.add("leave");
            list.add("leaderboard");
            list.add("pos1");
            list.add("pos2");
            list.add("setlobby");
            list.add("setspawn");
            list.add("create");
        }

        return list;
    }
}
