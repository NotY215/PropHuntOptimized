package pro.noty.prop.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import pro.noty.prop.PropHuntOptimized;
import pro.noty.prop.arena.Arena;
import pro.noty.prop.game.GameManager;

import java.util.ArrayList;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final PropHuntOptimized plugin;

    public MainCommand(PropHuntOptimized plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (args.length==0) { showHelp(p); return true; }

        if (args[0].equalsIgnoreCase("help")) { showHelp(p); return true; }

        Arena arena = plugin.getArenaManager().getArena(args[0]);
        if (arena==null && !args[1].equalsIgnoreCase("create")) { p.sendMessage("§cArena not found"); return true; }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!p.hasPermission("prophunt.admin")) {
                p.sendMessage("§cNo permission.");
                return true;
            }
            plugin.reloadPlugin();
            p.sendMessage("§aPropHunt config and arenas reloaded!");
            return true;
        }

        GameManager gm = plugin.getGameManager();


        if (args.length==2) {
            switch(args[1].toLowerCase()) {
                case "create": plugin.getArenaManager().createArena(args[0]); p.sendMessage("§aArena created"); return true;
                case "pos1": arena.setPos1(p.getLocation()); plugin.getArenaManager().saveArena(arena); p.sendMessage("§aPos1 set"); return true;
                case "pos2": arena.setPos2(p.getLocation()); plugin.getArenaManager().saveArena(arena); p.sendMessage("§aPos2 set"); return true;
                case "setlobby": arena.setLobby(p.getLocation()); plugin.getArenaManager().saveArena(arena); p.sendMessage("§aLobby set"); return true;
                case "setspawn": arena.setSpawn(p.getLocation()); plugin.getArenaManager().saveArena(arena); p.sendMessage("§aSpawn set"); return true;
                case "join": gm.joinArena(p, arena); return true;
                case "leave": gm.leaveGame(p); return true;
            }
        }

        showHelp(p);
        return true;
    }

    private void showHelp(Player p) {
        p.sendMessage("§6Prop Hunt Commands:");
        p.sendMessage("§e/mb <arena> create §7- Create arena");
        p.sendMessage("§e/mb <arena> pos1 §7- Set first position");
        p.sendMessage("§e/mb <arena> pos2 §7- Set second position");
        p.sendMessage("§e/mb <arena> setlobby §7- Set lobby");
        p.sendMessage("§e/mb <arena> setspawn §7- Set seeker spawn");
        p.sendMessage("§e/mb <arena> join §7- Join game");
        p.sendMessage("§e/mb <arena> leave §7- Leave game");
        p.sendMessage("§e/mb reload §7- Reload plugin files");

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("reload");
            plugin.getArenaManager().getArenaMap().keySet().forEach(completions::add);


        } else if (args.length==2) {
            completions.add("join");
            completions.add("leave");
            completions.add("pos1");
            completions.add("pos2");
            completions.add("setlobby");
            completions.add("setspawn");
            completions.add("create");
        }
        return completions;
    }
}
