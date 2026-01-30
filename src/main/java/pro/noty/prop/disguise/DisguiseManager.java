package pro.noty.prop.disguise;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DisguiseManager {

    private final JavaPlugin plugin;
    private final ProtocolManager protocolManager;

    private final Map<UUID, ArmorStand> disguiseStands = new HashMap<>();
    private final Map<UUID, Material> disguisedBlocks = new HashMap<>();

    public DisguiseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    /* ================= DISGUISE PLAYER ================= */

    public void disguise(Player player, Material blockMaterial) {
        undisguise(player);

        disguisedBlocks.put(player.getUniqueId(), blockMaterial);

        // Hide player
        for (Player other : Bukkit.getOnlinePlayers()) {
            other.hidePlayer(plugin, player);
        }

        // Spawn invisible armor stand
        ArmorStand stand = player.getWorld().spawn(player.getLocation(), ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setGravity(false);
            as.setMarker(true);
            as.setSmall(false);
            as.setInvulnerable(true);
            as.getEquipment().setHelmet(new ItemStack(blockMaterial));
        });

        disguiseStands.put(player.getUniqueId(), stand);

        // Keep stand synced with player
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !disguiseStands.containsKey(player.getUniqueId())) {
                    cancel();
                    return;
                }

                ArmorStand s = disguiseStands.get(player.getUniqueId());
                if (s == null || s.isDead()) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation().clone().add(0, -1.4, 0);
                s.teleport(loc);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /* ================= UNDISGUISE ================= */

    public void undisguise(Player player) {
        disguisedBlocks.remove(player.getUniqueId());


        ArmorStand stand = disguiseStands.remove(player.getUniqueId());
        if (stand != null && !stand.isDead()) stand.remove();

        // Show player again
        for (Player other : Bukkit.getOnlinePlayers()) {
            other.showPlayer(plugin, player);
        }
    }

    public boolean isDisguised(Player player) {
        return disguiseStands.containsKey(player.getUniqueId());
    }

    public Material getDisguisedBlock(Player player) {
        return disguisedBlocks.get(player.getUniqueId());
    }

    /* ================= CLEANUP ================= */

    public void removeAll() {
        for (ArmorStand stand : disguiseStands.values()) {
            if (stand != null && !stand.isDead()) stand.remove();
        }
        disguiseStands.clear();
        disguisedBlocks.clear();
    }
    // BACKWARD COMPATIBILITY METHODS

    public void removeDisguise(Player player) {
        undisguise(player); // call your new method
    }

    public void cleanupAll() {
        removeAll(); // call your new cleanup method
    }


}
