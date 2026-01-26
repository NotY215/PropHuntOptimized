package pro.noty.prop.disguise;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public class DisguiseManager {

    private final JavaPlugin plugin;

    // Maps player UUID → BlockDisplay
    private final Map<UUID, BlockDisplay> disguises = new HashMap<>();
    private final Map<UUID, Long> morphCooldown = new HashMap<>();
    private final long COOLDOWN_MS = 1500;

    public DisguiseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /* ===================== DISGUISE PLAYER ===================== */
    public void disguise(Player player, Material material) {
        if (!material.isBlock()) return;

        long now = System.currentTimeMillis();
        if (morphCooldown.containsKey(player.getUniqueId())
                && now - morphCooldown.get(player.getUniqueId()) < COOLDOWN_MS) {
            player.sendActionBar("§cYou are morphing too fast!");
            return;
        }
        morphCooldown.put(player.getUniqueId(), now);

        removeDisguise(player);

        Location base = player.getLocation().add(0, 0.5, 0); // player center inside block
        BlockData data = Bukkit.createBlockData(material);

        BlockDisplay display = (BlockDisplay) player.getWorld().spawnEntity(base, EntityType.BLOCK_DISPLAY);
        display.setBlock(data);
        display.setInterpolationDuration(3);
        display.setTeleportDuration(3);

        // Player inside block POV
        Transformation transform = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 1, 0),
                new Vector3f(1, 1, 1),
                new AxisAngle4f(0, 0, 1, 0)
        );
        display.setTransformation(transform);
        display.setGravity(false);
        display.setPersistent(false);
        display.setBrightness(new Display.Brightness(15, 15));

        disguises.put(player.getUniqueId(), display);
        player.setInvisible(true);

        playMorphEffect(player);
        startTracking(player);
    }

    /* =================== PLAYER MOVEMENT TRACKING =================== */
    private void startTracking(Player player) {
        new BukkitRunnable() {
            public void run() {
                if (!player.isOnline() || !disguises.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }
                BlockDisplay display = disguises.get(player.getUniqueId());

                // Smooth interpolation: previous location + current
                Location target = player.getLocation().add(0, 0.5, 0); // center inside block
                Location current = display.getLocation();
                double x = current.getX() + (target.getX() - current.getX()) * 0.2;
                double y = current.getY() + (target.getY() - current.getY()) * 0.2;
                double z = current.getZ() + (target.getZ() - current.getZ()) * 0.2;
                display.teleport(new Location(player.getWorld(), x, y, z));

                updateConnections(player, display);
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /* ============= WALL / FENCE / CONNECTABLE BLOCKS ============= */
    private void updateConnections(Player player, BlockDisplay display) {
        Material mat = display.getBlock().getMaterial();
        if (mat.name().contains("FENCE") || mat.name().contains("WALL") || mat.name().contains("PANE")) {
            BlockData data = Bukkit.createBlockData(mat);

            // Simple connection logic: clone surroundings
            Block b = player.getLocation().getBlock();
            for (BlockFace face : BlockFace.values()) {
                Block relative = b.getRelative(face);
                if (relative.getType() == mat) {
                    data = relative.getBlockData().clone();
                    break;
                }
            }
            display.setBlock(data);
        }
    }

    /* ===================== REMOVE DISGUISE ===================== */
    public void removeDisguise(Player player) {
        UUID id = player.getUniqueId();
        if (!disguises.containsKey(id)) return;

        BlockDisplay display = disguises.remove(id);
        display.remove();
        player.setInvisible(false);

        playDemorphEffect(player);
    }

    public boolean isDisguised(Player player) {
        return disguises.containsKey(player.getUniqueId());
    }

    /* ===================== BLOCK HIT EFFECTS ===================== */
    public void playFakeBlockHit(Player disguised) {
        if (!isDisguised(disguised)) return;

        BlockDisplay display = disguises.get(disguised.getUniqueId());
        Location loc = display.getLocation().add(0, 0.5, 0);
        disguised.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, 20, display.getBlock().getMaterial().createBlockData());
        disguised.getWorld().playSound(loc, Sound.BLOCK_STONE_HIT, 1f, 1f);
    }

    /* ===================== VISUAL & AUDIO EFFECTS ===================== */
    private void playMorphEffect(Player player) {
        Location loc = player.getLocation().add(0, 0.5, 0);
        player.getWorld().spawnParticle(Particle.CLOUD, loc, 25, 0.3, 0.5, 0.3, 0.02);
        player.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1f, 1.2f);
    }

    private void playDemorphEffect(Player player) {
        Location loc = player.getLocation().add(0, 0.5, 0);
        player.getWorld().spawnParticle(Particle.SMOKE, loc, 20, 0.3, 0.5, 0.3, 0.02);
        player.getWorld().playSound(loc, Sound.ENTITY_SHULKER_TELEPORT, 1f, 1f);
    }

    /* ===================== CLEANUP ALL DISGUISES ===================== */
    public void cleanupAll() {
        disguises.values().forEach(Display::remove);
        disguises.clear();
    }
}
