package pro.noty.prop.game;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import pro.noty.prop.disguise.DisguiseManager;
import pro.noty.prop.arena.Arena;

import java.util.*;

/**
 * Full-featured GameManager for PropHuntOptimized
 * Includes:
 * - Game lifecycle management
 * - Role assignment
 * - Hiding phase
 * - Hunter upgrades over time
 * - Seeker debuffs after 5 minutes
 * - Hitbox conceal for disguised players
 * - BossBar tracking
 * - API methods for external use
 */
public class GameManager implements Listener {

    private final JavaPlugin plugin;
    private final DisguiseManager disguiseManager;

    private final Set<UUID> hunters = new HashSet<>();
    private final Set<UUID> seekers = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();
    private final Set<UUID> inGame = new HashSet<>();
    private Arena arena;

    private BossBar bossBar;
    private boolean gameRunning = false;
    private boolean hidingPhase = false;

    private int gameTime;
    private int hideTime;
    private BukkitRunnable timerTask;
    private BukkitRunnable hunterUpgradeTask;

    private Player hunterPlayer;
    private int elapsedSeconds = 0;

    public GameManager(JavaPlugin plugin, DisguiseManager disguiseManager) {
        this.plugin = plugin;
        this.disguiseManager = disguiseManager;
    }

    /* ========================================================= */
    /* ===================== GAME START ========================= */
    /* ========================================================= */

    public void startGame(List<Player> players, Arena arena) {
        if (gameRunning) return;
        this.arena = arena;

        gameRunning = true;
        hidingPhase = true;
        elapsedSeconds = 0;

        gameTime = plugin.getConfig().getInt("game-time-seconds");
        hideTime = plugin.getConfig().getInt("hiding-seconds");

        bossBar = Bukkit.createBossBar("§aPreparing Game...", BarColor.GREEN, BarStyle.SEGMENTED_10);

        assignRoles(players);
        preparePlayers();
        startHidingCountdown();
    }

    /* ========================================================= */
    /* ===================== ROLE ASSIGN ======================== */
    /* ========================================================= */

    private void assignRoles(List<Player> players) {
        Collections.shuffle(players);
        hunterPlayer = players.get(0);
        hunters.add(hunterPlayer.getUniqueId());

        for (int i = 1; i < players.size(); i++) {
            seekers.add(players.get(i).getUniqueId());
        }
        inGame.addAll(hunters);
        inGame.addAll(seekers);
    }

    /* ========================================================= */
    /* ================= PREPARE PLAYERS ======================== */
    /* ========================================================= */

    private void preparePlayers() {
        for (UUID id : inGame) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;

            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            p.setFoodLevel(20);
            p.setSaturation(20);
            p.setInvisible(false);
            p.setGlowing(false);

            bossBar.addPlayer(p);

            // Hide name tags & collision
            p.setCollidable(false);
            p.setSilent(true);
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20, 1, false, false, false));
            p.removePotionEffect(PotionEffectType.INVISIBILITY);

            if (hunters.contains(id)) {
                setupHunter(p);
            } else {
                setupSeeker(p);
            }
        }
    }

    private void setupHunter(Player p) {
        p.teleport(arena.getLobby());
        p.sendTitle("§cYou are the HUNTER", "§7Wait for seekers to hide", 10, 60, 10);
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, hideTime * 20, 1));
        p.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
    }

    private void setupSeeker(Player p) {
        p.teleport(arena.getSpawn());
        p.sendTitle("§aYou are a SEEKER", "§7Hide as a block!", 10, 60, 10);
        p.getInventory().addItem(new ItemStack(Material.SPYGLASS));
        p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
    }
    /* ========================================================= */
    /* =================== JOIN LOGIC ========================== */
    /* ========================================================= */

    public void joinArena(Player p, Arena arena) {
        if (gameRunning) {
            p.sendMessage("§cThis game is already in progress!");
            return;
        }

        // Assign the arena if it's the first player
        if (this.arena == null) {
            this.arena = arena;
        }

        if (inGame.contains(p.getUniqueId())) {
            p.sendMessage("§cYou are already in the queue!");
            return;
        }

        inGame.add(p.getUniqueId());
        p.teleport(arena.getLobby());
        p.sendMessage("§aJoined arena: §f" + arena.getName());
        p.sendMessage("§7Players: §e" + inGame.size() + "§7/§e" + plugin.getConfig().getInt("max-players"));

        // Auto-start check
        if (inGame.size() >= plugin.getConfig().getInt("min-players")) {
            List<Player> players = new ArrayList<>();
            for (UUID id : inGame) {
                Player onlineP = Bukkit.getPlayer(id);
                if (onlineP != null) players.add(onlineP);
            }
            startGame(players, arena);
        }
    }

    /* ========================================================= */
    /* ================= HUNTER UPGRADES ======================= */
    /* ========================================================= */

    private void startHunterUpgrades() {
        hunterUpgradeTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameRunning) {
                    this.cancel();
                    return;
                }
                // Logic for periodic hunter buffs (e.g., every 2 minutes)
                if (elapsedSeconds > 0 && elapsedSeconds % 120 == 0) {
                    if (hunterPlayer != null && hunterPlayer.isOnline()) {
                        hunterPlayer.sendMessage("§b[!] You received a tracking boost!");
                        hunterPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
                    }
                }
            }
        };
        hunterUpgradeTask.runTaskTimer(plugin, 20, 20);
    }

    /* ========================================================= */
    /* ================== HIDING COUNTDOWN ====================== */
    /* ========================================================= */

    private void startHidingCountdown() {
        bossBar.setTitle("§eSeekers Hiding...");
        bossBar.setColor(BarColor.YELLOW);

        new BukkitRunnable() {
            int time = hideTime;

            public void run() {
                if (time <= 0) {
                    cancel();
                    releaseHunter();
                    startMainTimer();
                    startHunterUpgrades();
                    return;
                }
                bossBar.setProgress((double) time / hideTime);
                time--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void releaseHunter() {
        hidingPhase = false;
        hunterPlayer.removePotionEffect(PotionEffectType.BLINDNESS);
        hunterPlayer.teleport(arena.getSpawn());
        hunterPlayer.sendTitle("§cHUNT BEGINS!", "", 10, 40, 10);
    }

    /* ========================================================= */
    /* ===================== MAIN TIMER ========================= */
    /* ========================================================= */

    private void startMainTimer() {
        bossBar.setColor(BarColor.RED);

        timerTask = new BukkitRunnable() {
            int timeLeft = gameTime;

            public void run() {
                if (!gameRunning) {
                    cancel();
                    return;
                }

                elapsedSeconds++;
                bossBar.setTitle("§cSeekers Left: §f" + seekers.size() + " §7| §eTime: §f" + timeLeft);
                bossBar.setProgress((double) timeLeft / gameTime);

                // 5 MIN DIFFICULTY INCREASE
                if (elapsedSeconds == 300) {
                    increaseDifficulty();
                }

                // LAST MINUTE HUNTER UPGRADE
                if (timeLeft == 60) {
                    upgradeHunterFinal();
                }

                if (timeLeft <= 0) {
                    endGame(false);
                    cancel();
                }

                timeLeft--;
            }
        };
        timerTask.runTaskTimer(plugin, 20, 20);
    }

    /* ========================================================= */
    /* ================= DIFFICULTY SYSTEM ====================== */
    /* ========================================================= */

    private void increaseDifficulty() {
        for (UUID id : seekers) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;

            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));
            p.sendMessage("§cThe hunter is getting stronger...");
        }
    }

    private void upgradeHunterFinal() {
        hunterPlayer.getInventory().addItem(new ItemStack(Material.NETHERITE_SWORD));
        hunterPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
        hunterPlayer.sendTitle("§4FINAL MINUTE", "§cYou are now MAX POWER", 10, 60, 10);
    }

    /* ========================================================= */
    /* ================== PLAYER DEATH ========================== */
    /* ========================================================= */

    public void playerEliminated(Player p) {
        UUID id = p.getUniqueId();

        disguiseManager.removeDisguise(p);

        seekers.remove(id);
        hunters.remove(id);
        inGame.remove(id);

        spectators.add(id);
        p.setGameMode(GameMode.SPECTATOR);

        if (seekers.isEmpty()) {
            endGame(true);
        }
    }

    /* ========================================================= */
    /* ====================== END GAME ========================== */
    /* ========================================================= */

    private void endGame(boolean hunterWon) {
        gameRunning = false;
        if (timerTask != null) timerTask.cancel();
        if (hunterUpgradeTask != null) hunterUpgradeTask.cancel();

        // 1. Identify Winners and Losers for command placeholders
        List<Player> winners = new ArrayList<>();
        List<Player> losers = new ArrayList<>();

        if (hunterWon) {
            if (hunterPlayer != null) winners.add(hunterPlayer);
            for (UUID id : seekers) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) losers.add(p);
            }
        } else {
            for (UUID id : seekers) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) winners.add(p);
            }
            if (hunterPlayer != null) losers.add(hunterPlayer);
        }

        // 2. Run the End Commands from config
        executeEndCommands(winners, losers);

        // 3. Reset Players (existing logic)
        String title = hunterWon ? "§cHunter Wins!" : "§aSeekers Survived!";
        for (UUID id : inGame) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;

            p.sendTitle(title, "", 10, 70, 20);
            p.getInventory().clear();
            p.teleport(arena.getLobby());
            p.setGameMode(GameMode.ADVENTURE);
            p.setInvisible(false);
            p.setCollidable(true);
            p.setSilent(false);
        }

        disguiseManager.cleanupAll();
        bossBar.removeAll();

        hunters.clear();
        seekers.clear();
        spectators.clear();
        inGame.clear();
    }
    /* ========================================================= */
    /* ================= HITBOX VISIBILITY FIX ================== */
    /* ========================================================= */

    public void applyHitboxConceal(Player p) {
        p.setCollidable(false);
        p.setGlowing(false);
        p.setInvisible(false);
        p.setSilent(true);
        p.setCustomNameVisible(false);
    }

    /* ========================================================= */
    /* =================== PUBLIC API ========================== */
    /* ========================================================= */

    public boolean isHunter(Player p) { return hunters.contains(p.getUniqueId()); }
    public boolean isSeeker(Player p) { return seekers.contains(p.getUniqueId()); }
    public boolean isSpectator(Player p) { return spectators.contains(p.getUniqueId()); }
    public boolean isInGame(Player p) { return inGame.contains(p.getUniqueId()); }
    public boolean isGameRunning() { return gameRunning; }
    public Set<UUID> getHunters() { return hunters; }
    public Set<UUID> getSeekers() { return seekers; }
    public Set<UUID> getSpectators() { return spectators; }

    /* ========================================================= */
    /* ================== PLAYER LEAVE ========================== */
    /* ========================================================= */

    public void leaveGame(Player p) {
        if (!isInGame(p)) return;

        inGame.remove(p.getUniqueId());
        hunters.remove(p.getUniqueId());
        seekers.remove(p.getUniqueId());
        spectators.remove(p.getUniqueId());

        disguiseManager.removeDisguise(p);
        p.getInventory().clear();
        p.setGameMode(GameMode.ADVENTURE);
        p.teleport(arena.getLobby());

        checkGameStop();
    }

    private void checkGameStop() {
        if (inGame.size() < plugin.getConfig().getInt("min-players") && !gameRunning) {
            if (timerTask != null) timerTask.cancel();
        }
    }

    /* ========================================================= */
    /* ================== ELIMINATION HELPERS =================== */
    /* ========================================================= */

    public void eliminateSeeker(Player seeker) {
        seekers.remove(seeker.getUniqueId());
        seeker.setGameMode(GameMode.SPECTATOR);
        seeker.sendMessage("§cYou were found!");

        if (seekers.isEmpty()) {
            endGame(true);
        }
    }

    public void handleDeath(Player p) {
        if (isHunter(p)) {
            endGame(false);
        } else if (isSeeker(p)) {
            eliminateSeeker(p);
        }
    }
    private void executeEndCommands(List<Player> winners, List<Player> losers) {
        List<String> commands = plugin.getConfig().getStringList("end-commands");
        if (commands.isEmpty()) return;

        for (String cmd : commands) {
            // If the command mentions a winner, run it for every winner
            if (cmd.contains("%winner%")) {
                for (Player w : winners) {
                    String finalCmd = cmd.replace("%winner%", w.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                }
            }
            // If the command mentions a looser, run it for every loser
            else if (cmd.contains("%looser%")) {
                for (Player l : losers) {
                    String finalCmd = cmd.replace("%looser%", l.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                }
            }
            // Otherwise, just run the command once (like a global 'say' command)
            else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
    }
}
