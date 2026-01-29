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
    private final Map<UUID, BlockDisplay> disguises = new HashMap<>();
    private final Map<UUID, Long> morphCooldown = new HashMap<>();

    public DisguiseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        startFollowTask();
    }

    public void disguise(Player player, Material material) {
        if (!material.isBlock()) return;

        long now = System.currentTimeMillis();
        if (morphCooldown.getOrDefault(player.getUniqueId(), 0L) > now - 1500) {
            player.sendMessage("§cWait before morphing!");
            return;
        }
        morphCooldown.put(player.getUniqueId(), now);

        removeDisguise(player);

        // Visual effects for morphing
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1f, 1f);

        BlockDisplay display = player.getWorld().spawn(player.getLocation(), BlockDisplay.class);
        display.setBlock(material.createBlockData());

        // FIX: Centered on player's legs (aligns with both feet center)
        // Translation of -0.5 on X and Z centers a 1.0 wide block on the player's 0,0 center
        Transformation transform = new Transformation(
                new Vector3f(-0.5f, 0.0f, -0.5f),
                new Quaternionf(),
                new Vector3f(1f, 1f, 1f),
                new Quaternionf()
        );
        display.setTransformation(transform);
        display.setInterpolationDuration(2);
        display.setBrightness(new Display.Brightness(15, 15));

        disguises.put(player.getUniqueId(), display);

        // FIX: Handle Invisibility properly
        player.setInvisible(true);
        // We do NOT hide the player from others entirely here, otherwise projectiles/hits won't register.
        // Invisibility effect is enough for the "ghostly" feel or model removal.
    }

    public void removeDisguise(Player player) {
        BlockDisplay display = disguises.remove(player.getUniqueId());
        if (display != null) display.remove();

        player.setInvisible(false);
        // FIX: Force visibility update for everyone
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }
    }

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

                    // Move to player feet location
                    display.teleport(player.getLocation());
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void cleanupAll() {
        disguises.values().forEach(BlockDisplay::remove);
        disguises.clear();
    }

    public boolean isDisguised(Player player) { return disguises.containsKey(player.getUniqueId()); }
}