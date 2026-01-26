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
    public void setLobby(Location lobby) { this.lobby = lobby; }
    public void setSpawn(Location spawn) { this.spawn = spawn; }
    public void setPos1(Location pos1) { this.pos1 = pos1; }
    public void setPos2(Location pos2) { this.pos2 = pos2; }

    public Location getLobby() { return lobby; }
    public Location getSpawn() { return spawn; }
    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public Set<UUID> getPlayers() { return players; }

    public boolean isInside(Location loc) {
        if (pos1 == null || pos2 == null) return false;
        return loc.getWorld().equals(pos1.getWorld())
                && loc.getBlockX() >= Math.min(pos1.getBlockX(), pos2.getBlockX())
                && loc.getBlockX() <= Math.max(pos1.getBlockX(), pos2.getBlockX())
                && loc.getBlockY() >= Math.min(pos1.getBlockY(), pos2.getBlockY())
                && loc.getBlockY() <= Math.max(pos1.getBlockY(), pos2.getBlockY())
                && loc.getBlockZ() >= Math.min(pos1.getBlockZ(), pos2.getBlockZ())
                && loc.getBlockZ() <= Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }
}
