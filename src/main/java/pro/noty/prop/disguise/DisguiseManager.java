package pro.noty.prop.disguise;

import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;

import java.util.HashMap;
import java.util.Map;

public class DisguiseManager {

    private final Map<Player, BlockDisplay> disguises = new HashMap<>();
    private final JavaPlugin plugin;

    public DisguiseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void disguise(Player player, Material material) {
        removeDisguise(player);
        if (!material.isBlock()) return;

        player.setInvisible(true);
        player.setCollidable(false);

        BlockDisplay display = player.getWorld().spawn(player.getLocation(), BlockDisplay.class);
        display.setBlock(material.createBlockData());
        display.setInvulnerable(true);
        display.setGravity(false);

        Transformation t = display.getTransformation();
        t.getScale().set(1.0f, 1.0f, 1.0f); // block never rotates
        display.setTransformation(t);

        display.addPassenger(player);
        disguises.put(player, display);
    }

    public void removeDisguise(Player player) {
        BlockDisplay d = disguises.remove(player);
        if (d != null && !d.isDead()) d.remove();
        player.setInvisible(false);
        player.setCollidable(true);
    }

    public boolean isDisguised(Player p) { return disguises.containsKey(p); }
}
