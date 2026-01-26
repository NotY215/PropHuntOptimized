package pro.noty.prop.arena;

import java.util.*;

public class ArenaManager {

    private final Map<String, Arena> arenas = new HashMap<>();

    public ArenaManager(Object plugin) {}

    public Arena createArena(String name) {
        Arena arena = new Arena(name);
        arenas.put(name.toLowerCase(), arena);
        return arena;
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }
}
