package pro.noty.prop.disguise;

import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.*;

import java.util.*;

public class DisguiseManager {

    private final JavaPlugin plugin;
    private final Map<UUID, BlockDisplay> disguises = new HashMap<>();

    public DisguiseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        startFollowTask();
    }

    public void disguise(Player player, Material mat) {
        if (!mat.isBlock()) return;

        removeDisguise(player);

        BlockDisplay display = player.getWorld().spawn(player.getLocation(), BlockDisplay.class);
        display.setBlock(mat.createBlockData());

        // PERFECT CENTER FIX
        display.setTransformation(new Transformation(
                new Vector3f(-0.5f, 0f, -0.5f),
                new Quaternionf(),
                new Vector3f(1f,1f,1f),
                new Quaternionf()
        ));

        display.setInterpolationDuration(1);
        display.setBrightness(new Display.Brightness(15, 15));

        disguises.put(player.getUniqueId(), display);

        player.setInvisible(true);
        player.setCollidable(false);
    }

    public void removeDisguise(Player p) {
        BlockDisplay d = disguises.remove(p.getUniqueId());
        if (d != null) d.remove();

        p.setInvisible(false);
        p.setCollidable(true);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, p);
        }
    }

    private void startFollowTask() {
        new BukkitRunnable() {
            public void run() {
                disguises.forEach((uuid, display) -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null || !p.isOnline()) return;
                    display.teleport(p.getLocation());
                });
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void cleanupAll() {
        disguises.keySet().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) removeDisguise(p);
        });
        disguises.clear();
    }
}
