package pro.noty.prop.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import pro.noty.prop.PropHuntOptimized;
import pro.noty.prop.arena.Arena;

public class MainCommand implements CommandExecutor {

    private final PropHuntOptimized plugin;

    public MainCommand(PropHuntOptimized plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player p)) return true;

        if (args.length == 1 && args[0].equalsIgnoreCase("pos1")) {
            plugin.getArenaManager().createArena("setup").setPos1(p.getLocation());
            p.sendMessage("§aPos1 set.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("pos2")) {
            plugin.getArenaManager().getArena("setup").setPos2(p.getLocation());
            p.sendMessage("§aPos2 set.");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            plugin.getArenaManager().createArena(args[1]);
            p.sendMessage("§aArena created: " + args[1]);
            return true;
        }

        if (args.length == 2 && args[1].equalsIgnoreCase("setlobby")) {
            Arena arena = plugin.getArenaManager().getArena(args[0]);
            arena.setLobby(p.getLocation());
            p.sendMessage("§aLobby set.");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            plugin.getGameManager().joinArena(p, args[1]);
            return true;
        }

        return true;
    }
}
