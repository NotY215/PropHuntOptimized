package pro.noty.prop.disguise;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class DisguiseManager {

    private final JavaPlugin plugin;

    private final Map<UUID, BlockDisplay> disguises = new HashMap<>();
    private final Map<UUID, Long> morphCooldown = new HashMap<>();

    private final long COOLDOWN_MS = 1500;

    public DisguiseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /* ================= DISGUISE ================= */

    public void disguise(Player player, Material material) {
        if (!material.isBlock()) return;

        long now = System.currentTimeMillis();
        if (morphCooldown.containsKey(player.getUniqueId())
                && now - morphCooldown.get(player.getUniqueId()) < COOLDOWN_MS) {

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent("§cYou are morphing too fast!"));
            return;
        }
        morphCooldown.put(player.getUniqueId(), now);

        removeDisguise(player);

        Location base = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
        BlockData data = Bukkit.createBlockData(material);

        BlockDisplay display = (BlockDisplay) player.getWorld().spawnEntity(base, EntityType.BLOCK_DISPLAY);
        display.setBlock(data);
        display.setInterpolationDuration(2);
        display.setTeleportDuration(2);

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

    /* ================= TRACK MOVEMENT ================= */

    private void startTracking(Player player) {
        new BukkitRunnable() {
            public void run() {
                if (!player.isOnline() || !disguises.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }

                BlockDisplay display = disguises.get(player.getUniqueId());
                Location snap = player.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
                display.teleport(snap);

                updateConnections(player, display);
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /* ================= WALL/FENCE CONNECTIONS ================= */

    private void updateConnections(Player player, BlockDisplay display) {
        Material mat = display.getBlock().getMaterial();
        if (!(mat.name().contains("FENCE") || mat.name().contains("WALL"))) return;

        Block block = player.getLocation().getBlock();
        BlockData data = block.getBlockData().clone();
        display.setBlock(data);
    }

    /* ================= REMOVE DISGUISE ================= */

    public void removeDisguise(Player player) {
        UUID id = player.getUniqueId();
        if (!disguises.containsKey(id)) return;

        BlockDisplay display = disguises.remove(id);
        display.remove();

        player.setInvisible(false);
        playDemorphEffect(player);
    }

    public boolean isDisguised(Player p) {
        return disguises.containsKey(p.getUniqueId());
    }

    /* ================= HIT PARTICLES ================= */

    public void playFakeBlockHit(Player disguised) {
        if (!isDisguised(disguised)) return;

        Location loc = disguised.getLocation().add(0, 1, 0);
        Material mat = disguises.get(disguised.getUniqueId()).getBlock().getMaterial();

        disguised.getWorld().spawnParticle(Particle.BLOCK, loc, 20,
                Bukkit.createBlockData(mat));
        disguised.getWorld().playSound(loc, Sound.BLOCK_STONE_HIT, 1f, 1f);
    }

    /* ================= EFFECTS ================= */

    private void playMorphEffect(Player p) {
        Location l = p.getLocation().add(0, 1, 0);
        p.getWorld().spawnParticle(Particle.CLOUD, l, 25, 0.3, 0.5, 0.3, 0.02);
        p.getWorld().playSound(l, Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1f, 1.2f);
    }

    private void playDemorphEffect(Player p) {
        Location l = p.getLocation().add(0, 1, 0);
        p.getWorld().spawnParticle(Particle.SMOKE, l, 20, 0.3, 0.5, 0.3, 0.02);
        p.getWorld().playSound(l, Sound.ENTITY_SHULKER_TELEPORT, 1f, 1f);
    }

    /* ================= CLEANUP ================= */

    public void cleanupAll() {
        disguises.values().forEach(Entity::remove);
        disguises.clear();
    }
}
