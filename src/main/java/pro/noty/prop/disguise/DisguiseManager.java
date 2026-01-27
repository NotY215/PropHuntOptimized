package pro.noty.prop.disguise;

import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * DisguiseManager handles all player morphs into blocks
 * Includes:
 * - Advanced block morphs
 * - Smooth interpolation & delay
 * - Hitbox masking
 * - Wall/fence connection improvements
 */
public class DisguiseManager {

    private final JavaPlugin plugin;

    // Player UUID → Display Entity
    private final Map<UUID, BlockDisplay> disguises = new HashMap<>();

    // Morph cooldown to prevent spam
    private final Map<UUID, Long> morphCooldown = new HashMap<>();

    public DisguiseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        startFollowTask();
    }

    /* ========================================================= */
    /* =================== MORPH INTO BLOCK ===================== */
    /* ========================================================= */
    public void disguise(Player player, Material material) {
        if (!material.isBlock()) return;
        if (material == Material.AIR || material.name().contains("PORTAL")) return;

        long now = System.currentTimeMillis();
        if (morphCooldown.containsKey(player.getUniqueId())
                && now - morphCooldown.get(player.getUniqueId()) < 1500) {
            player.sendMessage("§cYou must wait before morphing again!");
            return;
        }
        morphCooldown.put(player.getUniqueId(), now);

        removeDisguise(player);

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

        BlockDisplay display = player.getWorld().spawn(player.getLocation(), BlockDisplay.class);
        display.setBlock(material.createBlockData());

        // Player-centered block transform
        Transformation transform = new Transformation(
                new Vector3f(-0.5f, 0f, -0.5f),
                new Quaternionf(), // No rotation ever
                new Vector3f(1f, 1f, 1f),
                new Quaternionf()
        );
        display.setTransformation(transform);

        // Interpolation for smooth morph
        display.setInterpolationDuration(3); // 3 ticks for smoothness
        display.setInterpolationDelay(1);

        display.setShadowRadius(0f);
        display.setShadowStrength(0f);
        display.setBrightness(new Display.Brightness(15, 15));

        disguises.put(player.getUniqueId(), display);

        // Hide player model but keep hitbox for server logic
        player.setInvisible(true);
        player.setCollidable(false);
        player.setSilent(true);
        player.setCustomNameVisible(false);

        player.sendMessage("§aYou morphed into §e" + material.name());
    }

    /* ========================================================= */
    /* ===================== REMOVE MORPH ======================= */
    /* ========================================================= */
    public void removeDisguise(Player player) {
        BlockDisplay display = disguises.remove(player.getUniqueId());
        if (display != null && !display.isDead()) {
            display.remove();
        }

        player.setInvisible(false);
        player.setCollidable(true);
        player.setSilent(false);

        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 15, 0.4, 0.4, 0.4);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);
    }

    /* ========================================================= */
    /* ================= FOLLOW PLAYER TASK ===================== */
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
                        if (display != null && !display.isDead()) display.remove();
                        it.remove();
                        continue;
                    }

                    Location loc = player.getLocation().clone();

                    // Player-centered Y-axis for inside-block POV
                    loc.setY(loc.getY() - 0.5);

                    display.teleport(loc);

                    // Optional: Advanced wall/fence connection logic
                    if (display.getBlock().getMaterial().name().contains("FENCE") ||
                            display.getBlock().getMaterial().name().contains("WALL")) {
                        display.setBlock(display.getBlock().getMaterial().createBlockData()); // refresh connection
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /* ========================================================= */
    /* =================== HITBOX CONCEAL TASK ================== */
    /* ========================================================= */
    public void concealHitbox(Player player) {
        player.setCollidable(false);
        player.setGlowing(false);
        player.setInvisible(false);
        player.setSilent(true);
        player.setCustomNameVisible(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!disguises.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }
                player.setCollidable(false);
                player.setGlowing(false);
            }
        }.runTaskTimer(plugin, 0, 40);
    }

    /* ========================================================= */
    /* =================== CLEANUP ALL ========================== */
    /* ========================================================= */
    public void cleanupAll() {
        for (BlockDisplay display : disguises.values()) {
            if (display != null && !display.isDead()) display.remove();
        }
        disguises.clear();
    }

    public boolean isDisguised(Player player) {
        return disguises.containsKey(player.getUniqueId());
    }

    public BlockDisplay getDisguise(Player player) {
        return disguises.get(player.getUniqueId());
    }

    public Set<UUID> getDisguisedPlayers() {
        return disguises.keySet();
    }
}
