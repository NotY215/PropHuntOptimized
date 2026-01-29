package pro.noty.prop.game;

import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pro.noty.prop.disguise.DisguiseManager;
import pro.noty.prop.arena.Arena;

import java.util.*;

/**
 * GameManager handles the core loop, combat, kits, and API for Prop Hunt.
 */
public class GameManager implements Listener {

    private final JavaPlugin plugin;
    private final DisguiseManager disguiseManager;
    private final StatsManager statsManager;

    private final Set<UUID> hunters = new HashSet<>();
    private final Set<UUID> seekers = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();
    private final Set<UUID> inGame = new HashSet<>();
    private Arena arena;

    private boolean gameRunning = false;
    private boolean lobbyCountdownActive = false;
    private boolean hidingPhase = false;

    private final int lobbyTime = 60;
    private final int hideTime = 30;
    private final int gameTime = 570;

    private BukkitTask timerTask;
    private Player hunterPlayer;
    private int elapsedSeconds = 0;

    public GameManager(JavaPlugin plugin, DisguiseManager disguiseManager) {
        this.plugin = plugin;
        this.disguiseManager = disguiseManager;
        this.statsManager = new StatsManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /* ========================================================= */
    /* =================== COMBAT & HEALTH FIX ================= */
    /* ========================================================= */

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim) || !(e.getDamager() instanceof Player attacker)) return;
        if (!isInGame(victim) || !isInGame(attacker)) return;

        // FIX: Ensure Seeker takes normal damage until death instead of dying in one hit
        if (isSeeker(victim) && isHunter(attacker)) {
            if (victim.getHealth() - e.getFinalDamage() <= 0) {
                e.setCancelled(true);
                handleDeath(victim);
            }
        }
    }

    /* ========================================================= */
    /* =================== UPDATED ROCKET LOGIC ================= */
    /* ========================================================= */

    @EventHandler
    public void onFireworkUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isInGame(p) || !isSeeker(p)) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.FIREWORK_ROCKET) return;

        // FIX: Allow placement on blocks, but blast in the air
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            return; // Minecraft handles placement naturally
        }

        if (e.getAction() == Action.RIGHT_CLICK_AIR) {
            e.setCancelled(true);

            // Spawn blast-style firework
            Firework fw = p.getWorld().spawn(p.getLocation().add(0, 1, 0), Firework.class);
            FireworkMeta fmeta = fw.getFireworkMeta();
            fmeta.addEffect(FireworkEffect.builder()
                    .withColor(Color.AQUA).withFade(Color.WHITE)
                    .with(FireworkEffect.Type.BURST).trail(true).build());
            fmeta.setPower(0);
            fw.setFireworkMeta(fmeta);
            fw.detonate(); // Immediate blast

            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);

            if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
            else p.getInventory().setItemInMainHand(null);
        }
    }

    /* ========================================================= */
    /* =================== JOIN & LOBBY LOGIC =================== */
    /* ========================================================= */

    public void joinArena(Player p, Arena arena) {
        if (gameRunning) {
            p.sendMessage("§cThis game is already in progress!");
            return;
        }

        this.arena = arena;
        if (!inGame.contains(p.getUniqueId())) {
            inGame.add(p.getUniqueId());
            p.teleport(arena.getLobby());
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            p.setHealth(20.0); // Reset Health
            pReset(p);
        }

        int minPlayers = plugin.getConfig().getInt("min-players", 2);
        broadcast("§e" + p.getName() + " §7joined! (§e" + inGame.size() + "§7/§e" + plugin.getConfig().getInt("max-players") + ")");

        if (inGame.size() >= minPlayers && !lobbyCountdownActive) {
            startLobbyCountdown();
        }
    }

    private void startLobbyCountdown() {
        lobbyCountdownActive = true;
        new BukkitRunnable() {
            int count = lobbyTime;
            @Override
            public void run() {
                if (inGame.size() < plugin.getConfig().getInt("min-players", 2)) {
                    broadcast("§cNot enough players! Countdown stopped.");
                    lobbyCountdownActive = false;
                    this.cancel();
                    return;
                }
                for (UUID id : inGame) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) updateScoreboard(p, "Lobby", count);
                }
                if (count <= 0) {
                    startGame();
                    lobbyCountdownActive = false;
                    this.cancel();
                    return;
                }
                if (count % 10 == 0 || count <= 5) broadcast("§6Game starting in §e" + count + "§6 seconds!");
                count--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    /* ========================================================= */
    /* ===================== GAME LIFECYCLE ===================== */
    /* ========================================================= */

    private void startGame() {
        gameRunning = true;
        hidingPhase = true;
        elapsedSeconds = 0;

        List<UUID> players = new ArrayList<>(inGame);
        Collections.shuffle(players);

        UUID hId = players.remove(0);
        hunterPlayer = Bukkit.getPlayer(hId);
        hunters.add(hId);
        for (UUID sId : players) seekers.add(sId);

        assignKits();
        startHidingPhase();
    }

    private void assignKits() {
        for (UUID id : seekers) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            p.teleport(arena.getSpawn());
            p.getInventory().clear();
            p.setHealth(20.0);
            p.getInventory().addItem(new ItemStack(Material.SPYGLASS, 1));
            p.getInventory().addItem(new ItemStack(Material.GOLDEN_CARROT, 64));
            p.getInventory().addItem(createNamedItem(Material.FIREWORK_ROCKET, "§bFlash Rocket", 64));
            pReset(p);
            p.sendTitle("§a§lSEEKER", "§7Hide quickly!", 10, 60, 10);
        }

        if (hunterPlayer != null) {
            hunterPlayer.teleport(arena.getLobby());
            hunterPlayer.getInventory().clear();
            hunterPlayer.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
            hunterPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, hideTime * 20, 1));
            hunterPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, hideTime * 20, 10));
            hunterPlayer.sendTitle("§c§lHUNTER", "§7Wait for seekers to hide...", 10, 60, 10);
        }
    }

    private void startHidingPhase() {
        new BukkitRunnable() {
            int count = hideTime;
            @Override
            public void run() {
                if (!gameRunning) { this.cancel(); return; }
                for (UUID id : inGame) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) updateScoreboard(p, "Hiding", count);
                }
                if (count <= 0) {
                    releaseHunter();
                    startMainTimer();
                    this.cancel();
                    return;
                }
                count--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void releaseHunter() {
        hidingPhase = false;
        if (hunterPlayer != null) {
            hunterPlayer.removePotionEffect(PotionEffectType.BLINDNESS);
            hunterPlayer.removePotionEffect(PotionEffectType.SLOWNESS);
            hunterPlayer.teleport(arena.getSpawn());
            hunterPlayer.sendTitle("§c§lGO!", "Find the props!", 10, 40, 10);
        }
    }

    private void startMainTimer() {
        timerTask = new BukkitRunnable() {
            int timeLeft = gameTime;
            @Override
            public void run() {
                if (!gameRunning) { this.cancel(); return; }
                elapsedSeconds++;
                for (UUID id : inGame) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) updateScoreboard(p, "Finding", timeLeft);
                }
                handleHunterUpgrades(timeLeft);
                if (timeLeft <= 0) { endGame(false); this.cancel(); }
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void handleHunterUpgrades(int timeLeft) {
        if (hunterPlayer == null) return;
        if (timeLeft == 400) hunterPlayer.getInventory().setItem(0, new ItemStack(Material.STONE_SWORD));
        else if (timeLeft == 200) hunterPlayer.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
        else if (timeLeft == 60) {
            hunterPlayer.getInventory().setItem(0, new ItemStack(Material.NETHERITE_SWORD));
            hunterPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 1));
        }
    }

    /* ========================================================= */
    /* ===================== END GAME LOGIC ===================== */
    /* ========================================================= */

    public void eliminateSeeker(Player seeker) {
        seekers.remove(seeker.getUniqueId());
        spectators.add(seeker.getUniqueId());
        seeker.setGameMode(GameMode.SPECTATOR);
        seeker.getInventory().clear();
        disguiseManager.removeDisguise(seeker);
        pReset(seeker);

        if (hunterPlayer != null) statsManager.addKill(hunterPlayer.getUniqueId());
        if (seekers.isEmpty()) endGame(true);
    }

    private void endGame(boolean hunterWon) {
        gameRunning = false;
        if (timerTask != null) timerTask.cancel();

        List<Player> winners = new ArrayList<>();
        List<Player> losers = new ArrayList<>();

        if (hunterWon) {
            if (hunterPlayer != null) { winners.add(hunterPlayer); statsManager.addWin(hunterPlayer.getUniqueId()); }
            for (UUID id : seekers) { Player p = Bukkit.getPlayer(id); if (p != null) losers.add(p); }
        } else {
            for (UUID id : seekers) { Player p = Bukkit.getPlayer(id); if (p != null) { winners.add(p); statsManager.addWin(id); } }
            if (hunterPlayer != null) losers.add(hunterPlayer);
        }

        executeEndCommands(winners, losers);
        broadcast(hunterWon ? "§c§lHUNTER WINS!" : "§a§lSEEKERS SURVIVED!");

        for (UUID id : inGame) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.getInventory().clear();
                p.setGameMode(GameMode.ADVENTURE);
                p.teleport(arena.getLobby());
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                pReset(p);
            }
        }
        disguiseManager.cleanupAll();
        inGame.clear(); hunters.clear(); seekers.clear(); spectators.clear();
    }

    private void pReset(Player p) {
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        p.removePotionEffect(PotionEffectType.BLINDNESS);
        p.removePotionEffect(PotionEffectType.SLOWNESS);
        // Force visibility fix
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, p);
        }
    }

    private void updateScoreboard(Player p, String status, int time) {
        // Dynamic Scoreboard API call
        statsManager.updateScoreboard(p, status, time, seekers.size(), (arena != null ? arena.getName() : "None"));
    }

    private void executeEndCommands(List<Player> winners, List<Player> losers) {
        List<String> commands = plugin.getConfig().getStringList("end-commands");
        for (String cmd : commands) {
            if (cmd.contains("%winner%")) for (Player w : winners) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%winner%", w.getName()));
            else if (cmd.contains("%looser%")) for (Player l : losers) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%looser%", l.getName()));
            else Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    /* ========================================================= */
    /* ======================== API/COMPAT ===================== */
    /* ========================================================= */

    public void leaveGame(Player p) {
        if (!isInGame(p)) return;
        UUID id = p.getUniqueId();
        inGame.remove(id); hunters.remove(id); seekers.remove(id); spectators.remove(id);
        disguiseManager.removeDisguise(p);
        p.getInventory().clear();
        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        pReset(p);
        if (arena != null) p.teleport(arena.getLobby());
        if (gameRunning && (hunters.isEmpty() || seekers.isEmpty())) endGame(seekers.isEmpty());
    }

    public void handleDeath(Player p) {
        if (!isInGame(p)) return;
        if (isHunter(p)) endGame(false);
        else if (isSeeker(p)) eliminateSeeker(p);
    }

    public void showLeaderboard(Player p) {
        p.sendMessage("§6§l=== PROP HUNT LEADERBOARD ===");
        p.sendMessage("§eYour Wins: §f" + statsManager.getWins(p.getUniqueId()));
        p.sendMessage("§eYour Kills: §f" + statsManager.getKills(p.getUniqueId()));
    }

    private ItemStack createNamedItem(Material mat, String name, int amt) {
        ItemStack item = new ItemStack(mat, amt);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); item.setItemMeta(meta); }
        return item;
    }

    public boolean isInGame(Player p) { return inGame.contains(p.getUniqueId()); }
    public boolean isHunter(Player p) { return hunters.contains(p.getUniqueId()); }
    public boolean isSeeker(Player p) { return seekers.contains(p.getUniqueId()); }
    public boolean isGameRunning() { return gameRunning; }
    private void broadcast(String msg) { for (UUID id : inGame) { Player p = Bukkit.getPlayer(id); if (p != null) p.sendMessage("§6[PropHunt] " + msg); } }
}