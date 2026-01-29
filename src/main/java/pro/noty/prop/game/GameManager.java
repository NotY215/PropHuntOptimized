package pro.noty.prop.game;

import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import pro.noty.prop.disguise.DisguiseManager;
import pro.noty.prop.arena.Arena;

import java.util.*;

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

    private BukkitRunnable timerTask;
    private Player hunterPlayer;
    private int elapsedSeconds = 0;

    public GameManager(JavaPlugin plugin, DisguiseManager disguiseManager) {
        this.plugin = plugin;
        this.disguiseManager = disguiseManager;
        this.statsManager = new StatsManager();
        Bukkit.getPluginManager().registerEvents(this, plugin); // Register internal firework listener
    }

    /* ========================================================= */
    /* =================== FIREWORK HANDLER ==================== */
    /* ========================================================= */

    @EventHandler
    public void onFireworkUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isInGame(p) || !isSeeker(p)) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = e.getItem();
            if (item != null && item.getType() == Material.FIREWORK_ROCKET) {
                e.setCancelled(true); // Stop standard placing

                // Launch functional firework
                Firework fw = p.getWorld().spawn(p.getLocation(), Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder().withColor(Color.RED).withFade(Color.ORANGE).with(FireworkEffect.Type.BALL_LARGE).build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);

                // Sound effect for hunter to hear
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);

                // Consume item
                if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                else p.getInventory().setItemInMainHand(null);
            }
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
            // Visibility fix: Ensure they are visible when joining lobby
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
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
            public void run() {
                if (inGame.size() < plugin.getConfig().getInt("min-players", 2)) {
                    broadcast("§cNot enough players! Countdown stopped.");
                    lobbyCountdownActive = false;
                    this.cancel();
                    return;
                }
                for (UUID id : inGame) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) statsManager.updateScoreboard(p, "Lobby", count, 0);
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
            p.getInventory().addItem(new ItemStack(Material.SPYGLASS, 1));
            p.getInventory().addItem(new ItemStack(Material.GOLDEN_CARROT, 64));
            p.getInventory().addItem(createNamedItem(Material.FIREWORK_ROCKET, "§bSound Maker", 64));
            p.getInventory().addItem(createNamedItem(Material.FIREWORK_ROCKET, "§bSound Maker", 64));
            p.sendTitle("§a§lSEEKER", "§7Hide quickly!", 10, 60, 10);
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
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
            public void run() {
                if (!gameRunning) { this.cancel(); return; }
                for (UUID id : inGame) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) statsManager.updateScoreboard(p, "Hiding", count, seekers.size());
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
            hunterPlayer.playSound(hunterPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        }
    }

    private void startMainTimer() {
        timerTask = new BukkitRunnable() {
            int timeLeft = gameTime;
            public void run() {
                if (!gameRunning) { this.cancel(); return; }
                elapsedSeconds++;
                for (UUID id : inGame) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) statsManager.updateScoreboard(p, "Finding", timeLeft, seekers.size());
                }
                handleHunterUpgrades(timeLeft);
                if (timeLeft <= 0) {
                    endGame(false);
                    this.cancel();
                }
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
        disguiseManager.removeDisguise(seeker); // Ensure disguise is removed on death

        if (hunterPlayer != null) {
            statsManager.addKill(hunterPlayer.getUniqueId());
            broadcast("§e" + seeker.getName() + " §7was caught! (§c" + seekers.size() + " §7left)");
        }
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
                p.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }

        disguiseManager.cleanupAll();
        inGame.clear();
        hunters.clear();
        seekers.clear();
        spectators.clear();
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
    /* ======================== API ============================ */
    /* ========================================================= */

    public boolean isInGame(Player p) { return inGame.contains(p.getUniqueId()); }
    public boolean isHunter(Player p) { return hunters.contains(p.getUniqueId()); }
    public boolean isSeeker(Player p) { return seekers.contains(p.getUniqueId()); }
    public void leaveGame(Player p) {
        if (!isInGame(p)) return;
        UUID id = p.getUniqueId();
        inGame.remove(id);
        hunters.remove(id);
        seekers.remove(id);
        spectators.remove(id);
        disguiseManager.removeDisguise(p);
        p.getInventory().clear();
        p.setGameMode(GameMode.ADVENTURE);
        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        if (arena != null) p.teleport(arena.getLobby());
        if (gameRunning && (hunters.isEmpty() || seekers.isEmpty())) endGame(seekers.isEmpty());
    }

    public void handleDeath(Player p) {
        if (!isInGame(p)) return;
        if (isHunter(p)) endGame(false);
        else if (isSeeker(p)) eliminateSeeker(p);
    }

    private void broadcast(String msg) {
        for (UUID id : inGame) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage("§6[PropHunt] " + msg);
        }
    }

    private ItemStack createNamedItem(Material mat, String name, int amt) {
        ItemStack item = new ItemStack(mat, amt);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); item.setItemMeta(meta); }
        return item;
    }

    public void showLeaderboard(Player p) {
        p.sendMessage("§6§l=== PROP HUNT LEADERBOARD ===");
        p.sendMessage("§eYour Wins: §f" + statsManager.getWins(p.getUniqueId()));
        p.sendMessage("§eYour Kills: §f" + statsManager.getKills(p.getUniqueId()));
    }
}