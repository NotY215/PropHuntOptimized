// ==========================
// ArenaProtectionListener.java
// ==========================
package pro.noty.prop.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pro.noty.prop.game.GameManager;
import org.bukkit.entity.Player;

public class ArenaProtectionListener implements Listener {

    private final GameManager gameManager;

    public ArenaProtectionListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (gameManager.isInGame(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (gameManager.isInGame(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent e) {
        if (e.getLocation() != null) {
            e.setCancelled(true); // prevent mobs in arena
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (gameManager.isInGame(p)) e.setCancelled(true);
    }

    @EventHandler
    public void onPickupItem(PlayerPickupItemEvent e) {
        Player p = e.getPlayer();
        if (gameManager.isInGame(p)) e.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!gameManager.isInGame(e.getPlayer())) return;
        if (e.getItem() != null && e.getItem().getType() == Material.SPYGLASS) return; // Allow spyglass
        e.setCancelled(true);
    }
}
