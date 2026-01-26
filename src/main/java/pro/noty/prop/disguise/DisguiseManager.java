package pro.noty.prop.disguise;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;

import java.util.*;

public class DisguiseManager {

    private final Map<UUID, BlockDisplay> disguises = new HashMap<>();
    private final JavaPlugin plugin;

    public DisguiseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void disguise(Player player, Material material) {
        if (!material.isBlock()) return;

        removeDisguise(player);

        player.setInvisible(true);
        player.setCollidable(false);

        BlockDisplay display = player.getWorld().spawn(player.getLocation(), BlockDisplay.class);
        display.setBlock(material.createBlockData());
        display.setInvulnerable(true);
        display.setGravity(false);
        display.setBrightness(new Display.Brightness(15, 15));

        Transformation t = display.getTransformation();
        t.getScale().set(1.01f, 1.01f, 1.01f);
        display.setTransformation(t);

        display.addPassenger(player);
        disguises.put(player.getUniqueId(), display);
    }

    public void removeDisguise(Player player) {
        BlockDisplay d = disguises.remove(player.getUniqueId());
        if (d != null && !d.isDead()) d.remove();

        player.setInvisible(false);
        player.setCollidable(true);
    }

    public boolean isDisguised(Player p) {
        return disguises.containsKey(p.getUniqueId());
    }
}
