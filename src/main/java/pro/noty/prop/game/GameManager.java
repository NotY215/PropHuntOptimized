package pro.noty.prop.game;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import pro.noty.prop.PropHuntOptimized;
import pro.noty.prop.arena.Arena;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public class GameManager implements Listener {

    private final PropHuntOptimized plugin;
    private final GameSession session = new GameSession();

    public GameManager(PropHuntOptimized plugin) {
        this.plugin = plugin;
    }

    /* ========================================================= */
    /* ======================= JOIN ============================== */
    /* ========================================================= */

    public void joinArena(Player p, Arena arena) {
        if (session.inGame) {
            p.sendMessage("§cGame already running!");
            return;
        }
        if (session.players.contains(p)) {
            p.sendMessage("§cYou already joined!");
            return;
        }

        session.players.add(p);
        session.arena = arena;

        p.teleport(arena.getLobby());
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().addItem(createLeaveDiamond());

        p.sendMessage("§aJoined arena §e" + arena.getName());

        if (session.players.size() >= plugin.getConfig().getInt("min-players")) {
            startCountdown();
        }
    }

    private ItemStack createLeaveDiamond() {
        ItemStack d = new ItemStack(Material.DIAMOND);
        ItemMeta m = d.getItemMeta();
        m.setDisplayName("§cLeave Game");
        d.setItemMeta(m);
        return d;
    }

    /* ========================================================= */
    /* ======================= LEAVE ============================= */
    /* ========================================================= */

    public void leaveGame(Player p) {
        if (!session.players.contains(p)) return;

        plugin.getDisguiseManager().removeDisguise(p);
        session.players.remove(p);
        session.seekers.remove(p);

        p.getInventory().clear();
        p.teleport(session.arena.getLobby());
        p.setGameMode(GameMode.ADVENTURE);

        if (!session.inGame && session.players.size() < plugin.getConfig().getInt("min-players")) {
            if (session.countdownTask != null) session.countdownTask.cancel();
        }
    }

    /* ========================================================= */
    /* ==================== COUNTDOWN ============================ */
    /* ========================================================= */

    private void startCountdown() {
        session.countdownTime = plugin.getConfig().getInt("countdown-seconds");

        session.countdownTask = new BukkitRunnable() {
            public void run() {
                if (session.players.size() < plugin.getConfig().getInt("min-players")) {
                    cancel();
                    return;
                }
                if (session.countdownTime <= 0) {
                    cancel();
                    startGame();
                    return;
                }
                broadcast("§eGame starts in §c" + session.countdownTime);
                session.countdownTime--;
            }
        };
        session.countdownTask.runTaskTimer(plugin, 0, 20);
    }

    /* ========================================================= */
    /* ===================== GAME START ========================== */
    /* ========================================================= */

    private void startGame() {
        session.inGame = true;

        List<Player> players = new ArrayList<>(session.players);
        session.hunter = players.get(new Random().nextInt(players.size()));

        for (Player p : players) {
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);

            if (p.equals(session.hunter)) {
                p.teleport(session.arena.getLobby());
                giveHunterKit(p);
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 30, 1));
                p.sendTitle("§cYou are the Hunter!", "§7Wait...", 10, 60, 10);
            } else {
                session.seekers.add(p);
                p.teleport(session.arena.getSpawn());
                giveSeekerKit(p);
                p.sendTitle("§aYou are a Seeker!", "§7Hide!", 10, 60, 10);
            }
        }

        startHidingPhase();
    }

    /* ========================================================= */
    /* ==================== HIDING PHASE ========================= */
    /* ========================================================= */

    private void startHidingPhase() {
        int hidingSeconds = plugin.getConfig().getInt("hiding-seconds");

        new BukkitRunnable() {
            int t = hidingSeconds;

            public void run() {
                if (t <= 0) {
                    session.hunter.teleport(session.arena.getSpawn());
                    session.hunter.removePotionEffect(PotionEffectType.BLINDNESS);
                    startMainTimer();
                    cancel();
                    return;
                }

                for (Player s : session.seekers) {
                    s.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent("§eHiding: §c" + t));
                }
                t--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    /* ========================================================= */
    /* ==================== MAIN GAME TIMER ====================== */
    /* ========================================================= */

    private void startMainTimer() {
        int gameTime = plugin.getConfig().getInt("game-time-seconds");

        session.gameBar = Bukkit.createBossBar("Time Left", BarColor.RED, BarStyle.SOLID);
        session.getAll().forEach(session.gameBar::addPlayer);

        new BukkitRunnable() {
            int t = gameTime;

            public void run() {
                if (t <= 0) {
                    endGame(false);
                    cancel();
                    return;
                }
                session.gameBar.setTitle("Seekers: " + session.seekers.size() + " | " + t + "s");
                session.gameBar.setProgress(t / (double) gameTime);
                t--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    /* ========================================================= */
    /* ======================== KITS ============================= */
    /* ========================================================= */

    private void giveHunterKit(Player p) {
        p.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
    }

    private void giveSeekerKit(Player p) {
        p.getInventory().addItem(new ItemStack(Material.SPYGLASS));
        p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
        ItemStack rocket = new ItemStack(Material.FIREWORK_ROCKET, 5);
        FireworkMeta meta = (FireworkMeta) rocket.getItemMeta();
        meta.setPower(1);
        rocket.setItemMeta(meta);
        p.getInventory().addItem(rocket);
    }

    /* ========================================================= */
    /* ======================== MORPH ============================ */
    /* ========================================================= */

    @EventHandler
    public void onSpyglass(PlayerInteractEvent e) {
        if (e.getItem() == null || e.getItem().getType() != Material.SPYGLASS) return;
        Player p = e.getPlayer();
        Block target = p.getTargetBlockExact(10);

        if (target == null || target.getType() == Material.AIR) {
            plugin.getDisguiseManager().removeDisguise(p);
            return;
        }
        plugin.getDisguiseManager().disguise(p, target.getType());
    }

    /* ========================================================= */
    /* ==================== ARENA PROTECTION ===================== */
    /* ========================================================= */

    @EventHandler public void onBlockBreak(BlockBreakEvent e) { if (session.inGame) e.setCancelled(true); }
    @EventHandler public void onPortal(PlayerPortalEvent e) { if (session.inGame) e.setCancelled(true); }
    @EventHandler public void onMobSpawn(CreatureSpawnEvent e) {
        if (session.arena != null && session.arena.isInside(e.getLocation())) e.setCancelled(true);
    }

    /* ========================================================= */
    /* ======================= COMBAT ============================ */
    /* ========================================================= */

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player hunter)) return;
        if (!(e.getEntity() instanceof Player seeker)) return;

        if (plugin.getDisguiseManager().isDisguised(seeker)) {
            plugin.getDisguiseManager().playFakeBlockHit(seeker);
        }
    }


    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        plugin.getDisguiseManager().removeDisguise(p);

        if (p.equals(session.hunter)) endGame(false);
        if (session.seekers.contains(p)) {
            session.seekers.remove(p);
            if (session.seekers.isEmpty()) endGame(true);
        }
    }

    /* ========================================================= */
    /* ======================= END GAME ========================== */
    /* ========================================================= */

    private void endGame(boolean hunterWon) {
        for (Player p : session.getAll()) {
            plugin.getDisguiseManager().removeDisguise(p);
            p.getInventory().clear();
            p.teleport(session.arena.getLobby());
        }

        if (session.gameBar != null) session.gameBar.removeAll();
        session.reset();
    }

    private void broadcast(String msg) { session.getAll().forEach(p -> p.sendMessage(msg)); }

    /* ========================================================= */
    /* ===================== SESSION DATA ======================== */
    /* ========================================================= */

    private static class GameSession {
        Arena arena;
        Player hunter;
        List<Player> seekers = new ArrayList<>();
        List<Player> players = new ArrayList<>();
        boolean inGame = false;
        BossBar gameBar;
        BukkitRunnable countdownTask;
        int countdownTime;

        void reset() {
            inGame = false;
            players.clear();
            seekers.clear();
            hunter = null;
            countdownTask = null;
        }

        List<Player> getAll() {
            List<Player> all = new ArrayList<>(players);
            if (hunter != null) all.add(hunter);
            return all;
        }
    }
}
