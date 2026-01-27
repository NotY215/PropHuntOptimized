package pro.noty.prop.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import pro.noty.prop.PropHuntOptimized;
import pro.noty.prop.game.GameManager;

public class SpyglassMorphListener implements Listener {

    private final PropHuntOptimized plugin;
    private final GameManager gameManager;

    public SpyglassMorphListener(PropHuntOptimized plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onUseSpyglass(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getItem() == null || e.getItem().getType() != Material.SPYGLASS) return;

        Player player = e.getPlayer();

        if (!gameManager.isSeeker(player)) return;

        Block target = player.getTargetBlockExact(10);

        // Looking at sky = demorph
        if (target == null || target.getType() == Material.AIR) {
            plugin.getDisguiseManager().removeDisguise(player);
            player.sendMessage("§eReturned to normal form");
            return;
        }

        if (!target.getType().isBlock()) return;

        plugin.getDisguiseManager().disguise(player, target.getType());
    }
}
