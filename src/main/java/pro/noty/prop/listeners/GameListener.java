package pro.noty.prop.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pro.noty.prop.PropHuntOptimized;
import pro.noty.prop.game.GameManager;

public class GameListener implements Listener {

    private final PropHuntOptimized plugin;
    private final GameManager gameManager;

    public GameListener(PropHuntOptimized plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    /* ================= HUNTER TAG ================= */

    @EventHandler
    public void onHunterHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player hunter)) return;
        if (!(e.getEntity() instanceof Player seeker)) return;

        if (!gameManager.isHunter(hunter)) return;
        if (!gameManager.isSeeker(seeker)) return;

        e.setCancelled(true);

        plugin.getDisguiseManager().removeDisguise(seeker);
        gameManager.eliminateSeeker(seeker);
    }

    /* ================= DEATH ================= */

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        plugin.getDisguiseManager().removeDisguise(p);

        if (gameManager.isInGame(p)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                p.spigot().respawn();
                p.setGameMode(GameMode.SPECTATOR);
                gameManager.handleDeath(p);
            });
        }
    }

    /* ================= QUIT ================= */

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.getDisguiseManager().removeDisguise(p);

        if (gameManager.isInGame(p)) {
            gameManager.leaveGame(p);
        }
    }
}
