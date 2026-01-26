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

    public DisguiseManager(JavaPlugin plugin) { this.plugin = plugin; }

    // Player moves with block, block never rotates, player invisible
    public void disguise(Player player, Material material) {
        removeDisguise(player);
        if (!material.isBlock()) return;

        player.setInvisible(true);
        BlockDisplay block = player.getWorld().spawn(player.getLocation(), BlockDisplay.class);
        block.setBlock(material.createBlockData());
        block.setInvulnerable(true);
        block.setGravity(false);

        // Keep block at player location on move
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline() && !player.isDead()) {
                block.teleport(player.getLocation());
            }
        }, 0L, 1L);

        disguises.put(player, block);
    }

    public void removeDisguise(Player player) {
        BlockDisplay block = disguises.remove(player);
        if (block != null && !block.isDead()) block.remove();
        player.setInvisible(false);
    }

    public boolean isDisguised(Player player) { return disguises.containsKey(player); }
}
