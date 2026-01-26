package pro.noty.prop.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import pro.noty.prop.PropHuntOptimized;
import pro.noty.prop.arena.Arena;
import pro.noty.prop.game.GameManager;

public class MainCommand implements CommandExecutor {

    private final PropHuntOptimized plugin;

    public MainCommand(PropHuntOptimized plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        GameManager gm = plugin.getGameManager();
        if (args.length==0) { showHelp(p); return true; }

        Arena arena = plugin.getArenaManager().getArena(args[0]);
        if (arena==null) { p.sendMessage("§cArena not found!"); return true; }

        if (args.length==2) {
            switch(args[1].toLowerCase()) {
                case "join": gm.joinArena(p,arena); return true;
                case "leave": gm.leaveGame(p); return true;
                case "setlobby": arena.setLobby(p.getLocation()); p.sendMessage("§aLobby set!"); return true;
                case "setspawn": arena.setSpawn(p.getLocation()); p.sendMessage("§aSpawn set!"); return true;
            }
        }
        showHelp(p);
        return true;
    }

    private void showHelp(Player p) {
        p.sendMessage("§6Prop Hunt Commands:");
        p.sendMessage("§e/mb <arena> join §7- Join a game");
        p.sendMessage("§e/mb <arena> leave §7- Leave the game");
        p.sendMessage("§e/mb <arena> setlobby §7- Set lobby location");
        p.sendMessage("§e/mb <arena> setspawn §7- Set spawn for seekers");
        p.sendMessage("§e/mb help §7- Show help");
    }
}
