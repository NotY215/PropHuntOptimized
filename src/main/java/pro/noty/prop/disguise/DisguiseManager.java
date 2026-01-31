package pro.noty.prop.disguise;

import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public class DisguiseManager {

    private final JavaPlugin plugin;

    // NEW SYSTEM
    private final Map<UUID, BlockDisplay> disguises = new HashMap<>();
    private final Map<UUID, Long> morphCooldown = new HashMap<>();

    public DisguiseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        startFollowTask();
    }

    /* ========================================================= */
    /* ===================== NEW MORPH SYSTEM =================== */
    /* ========================================================= */

    public void disguise(Player player, Material material) {
        if (!material.isBlock()) return;

        long now = System.currentTimeMillis();
        if (morphCooldown.getOrDefault(player.getUniqueId(), 0L) > now - 1500) {
            player.sendMessage("§cWait before morphing again!");
            return;
        }
        morphCooldown.put(player.getUniqueId(), now);

        removeDisguise(player);

        BlockDisplay display = player.getWorld().spawn(player.getLocation(), BlockDisplay.class);
        display.setBlock(material.createBlockData());

        // Perfect block alignment
        Transformation transform = new Transformation(
                new Vector3f(-0.5f, 0f, -0.5f),
                new Quaternionf(),
                new Vector3f(1f, 1f, 1f),
                new Quaternionf()
        );

        display.setTransformation(transform);
        display.setInterpolationDuration(2);
        display.setBrightness(new Display.Brightness(15, 15));

        disguises.put(player.getUniqueId(), display);

        player.setInvisible(true);
        player.setCollidable(false);

        // Force hide player model from others
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.hidePlayer(plugin, player);
        }
    }

    /* ========================================================= */
    /* =================== FOLLOW PLAYER TASK =================== */
    /* ========================================================= */

    private void startFollowTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, BlockDisplay>> it = disguises.entrySet().iterator();

                while (it.hasNext()) {
                    Map.Entry<UUID, BlockDisplay> entry = it.next();
                    Player player = Bukkit.getPlayer(entry.getKey());
                    BlockDisplay display = entry.getValue();

                    if (player == null || !player.isOnline() || display.isDead()) {
                        if (display != null) display.remove();
                        it.remove();
                        continue;
                    }

                    display.teleport(player.getLocation());
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /* ========================================================= */
    /* ================= OLD API COMPATIBILITY ================== */
    /* ========================================================= */

    public void removeDisguise(Player player) {
        BlockDisplay display = disguises.remove(player.getUniqueId());
        if (display != null) display.remove();

        player.setInvisible(false);
        player.setCollidable(true);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
    }

    public void undisguise(Player player) { // alias for newer calls
        removeDisguise(player);
    }

    public void cleanupAll() {
        for (UUID uuid : disguises.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) removeDisguise(p);
        }
        disguises.clear();
    }

    public void removeAll() { cleanupAll(); } // new alias
}
