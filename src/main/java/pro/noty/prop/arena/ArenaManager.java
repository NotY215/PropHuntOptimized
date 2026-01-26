package pro.noty.prop.arena;

import pro.noty.prop.PropHuntOptimized;

import java.util.HashMap;
import java.util.Map;

public class ArenaManager {

    private final Map<String, Arena> arenaMap = new HashMap<>();
    private final PropHuntOptimized plugin;

    public ArenaManager(PropHuntOptimized plugin) {
        this.plugin = plugin;
    }

    public Arena createArena(String name) {
        Arena arena = new Arena(name);
        arenaMap.put(name.toLowerCase(), arena);
        return arena;
    }

    public Arena getArena(String name) {
        return arenaMap.get(name.toLowerCase());
    }
}
