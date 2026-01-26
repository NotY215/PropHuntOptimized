package pro.noty.prop.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import pro.noty.prop.PropHuntOptimized;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ArenaManager {

    private final Map<String, Arena> arenaMap = new HashMap<>();
    private final PropHuntOptimized plugin;
    private final File arenaFolder;

    public ArenaManager(PropHuntOptimized plugin) {
        this.plugin = plugin;
        arenaFolder = new File(plugin.getDataFolder(), "arenas");
        if (!arenaFolder.exists()) arenaFolder.mkdirs();
        loadArenas();
    }

    public Arena createArena(String name) {
        Arena arena = new Arena(name);
        arenaMap.put(name.toLowerCase(), arena);
        saveArena(arena);
        return arena;
    }

    public Arena getArena(String name) {
        return arenaMap.get(name.toLowerCase());
    }

    public void saveArena(Arena arena) {
        File file = new File(arenaFolder, arena.getName() + ".yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (arena.getLobby() != null) cfg.set("lobby", locationToString(arena.getLobby()));
        if (arena.getSpawn() != null) cfg.set("spawn", locationToString(arena.getSpawn()));
        if (arena.getPos1() != null) cfg.set("pos1", locationToString(arena.getPos1()));
        if (arena.getPos2() != null) cfg.set("pos2", locationToString(arena.getPos2()));
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public void loadArenas() {
        if (!arenaFolder.exists()) return;
        for (File file : arenaFolder.listFiles()) {
            if (!file.getName().endsWith(".yml")) continue;
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String name = file.getName().replace(".yml", "");
            Arena arena = new Arena(name);
            if (cfg.contains("lobby")) arena.setLobby(stringToLocation(cfg.getString("lobby")));
            if (cfg.contains("spawn")) arena.setSpawn(stringToLocation(cfg.getString("spawn")));
            if (cfg.contains("pos1")) arena.setPos1(stringToLocation(cfg.getString("pos1")));
            if (cfg.contains("pos2")) arena.setPos2(stringToLocation(cfg.getString("pos2")));
            arenaMap.put(name.toLowerCase(), arena);
        }
    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    private Location stringToLocation(String str) {
        String[] split = str.split(",");
        return new Location(Bukkit.getWorld(split[0]),
                Double.parseDouble(split[1]),
                Double.parseDouble(split[2]),
                Double.parseDouble(split[3]),
                Float.parseFloat(split[4]),
                Float.parseFloat(split[5]));
    }
}
