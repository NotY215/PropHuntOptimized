package pro.noty.prop.arena;

import org.bukkit.Location;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Arena {

    private final String name;
    private Location lobby;
    private Location spawn;
    private Location pos1, pos2;
    private final Set<UUID> players = new HashSet<>();

    public Arena(String name) { this.name = name; }

    public String getName() { return name; }
    public void setPos1(Location l) { pos1 = l; }
    public void setPos2(Location l) { pos2 = l; }
    public void setLobby(Location l) { lobby = l; }
    public void setSpawn(Location l) { spawn = l; }
    public Location getLobby() { return lobby; }
    public Location getSpawn() { return spawn; }
    public Set<UUID> getPlayers() { return players; }

    public boolean isInside(Location loc) {
        if (pos1 == null || pos2 == null) return false;
        return loc.getWorld().equals(pos1.getWorld()) &&
                loc.getBlockX() >= Math.min(pos1.getBlockX(), pos2.getBlockX()) &&
                loc.getBlockX() <= Math.max(pos1.getBlockX(), pos2.getBlockX()) &&
                loc.getBlockY() >= Math.min(pos1.getBlockY(), pos2.getBlockY()) &&
                loc.getBlockY() <= Math.max(pos1.getBlockY(), pos2.getBlockY()) &&
                loc.getBlockZ() >= Math.min(pos1.getBlockZ(), pos2.getBlockZ()) &&
                loc.getBlockZ() <= Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }
}
