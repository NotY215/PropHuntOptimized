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
import org.bukkit.inventory.ItemStack;

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
            ItemStack itemInHand = e.getItemInHand();

            // Allow placing the rocket item, but only if it's the specific material.
            // I'm assuming your rocket item is a FIREWORK_ROCKET.
            if (itemInHand != null && itemInHand.getType() == Material.FIREWORK_ROCKET) {
                // Do NOT cancel the event, allowing the rocket to be used/placed.
                return;
            }

            // For any other material in the game, cancel the place event.
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
        Player p = e.getPlayer();
        if (!gameManager.isInGame(p)) return;

        Material m = e.getClickedBlock() != null ? e.getClickedBlock().getType() : null;

        if (m != null && (
                m.name().contains("DOOR") ||
                        m.name().contains("TRAPDOOR") ||
                        m.name().contains("BUTTON") ||
                        m.name().contains("LEVER") ||
                        m.name().contains("GATE"))) {
            return; // allow openable blocks
        }

        if (e.getItem() != null && e.getItem().getType() == Material.SPYGLASS) return;

        e.setCancelled(true);
    }

}