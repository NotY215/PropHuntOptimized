package pro.noty.prop.game;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pro.noty.prop.PropHuntOptimized;
import pro.noty.prop.disguise.DisguiseManager;
import pro.noty.prop.arena.Arena;
import pro.noty.prop.game.HunterVoteManager;

import java.util.*;

public class GameManager implements Listener {

    private final PropHuntOptimized plugin;
    private final DisguiseManager disguiseManager;
    private final StatsManager statsManager;

    private final HunterVoteManager voteManager;

    private final Set<UUID> hunters = new HashSet<>();
    private final Set<UUID> seekers = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();
    private final Set<UUID> inGame = new HashSet<>();

    private final Map<UUID, Long> rocketCooldown = new HashMap<>();

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

    public GameManager(PropHuntOptimized plugin, DisguiseManager disguiseManager) {
        this.plugin = plugin;
        this.disguiseManager = disguiseManager;
        this.statsManager = new StatsManager();
        this.voteManager = new HunterVoteManager(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(voteManager, plugin); // ✅ ADD THIS
    }



    /* ========================================================= */
    /* ===================== COMBAT SYSTEM ====================== */
    /* ========================================================= */

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!isInGame(victim) || !isInGame(attacker)) return;

        // Prevent friendly fire
        if (isSeeker(victim) && isSeeker(attacker)) {
            e.setCancelled(true);
            return;
        }

        // Hunter vs Hunter shouldn't happen but block anyway
        if (isHunter(victim) && isHunter(attacker)) {
            e.setCancelled(true);
            return;
        }

        // REAL DAMAGE — but control deaths manually
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (victim.getHealth() <= 0) handleDeath(victim);
        });
    }

    /* ========================================================= */
    /* ===================== FIREWORK ABILITY =================== */
    /* ========================================================= */

    @EventHandler
    public void onFireworkUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isInGame(p)) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.FIREWORK_ROCKET) return;
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) return;

        long now = System.currentTimeMillis();
        if (rocketCooldown.containsKey(p.getUniqueId())
                && now - rocketCooldown.get(p.getUniqueId()) < 10000) {
            p.sendMessage("§cRocket recharging...");
            return;
        }

        rocketCooldown.put(p.getUniqueId(), now);
        e.setCancelled(true);

        Firework fw = p.getWorld().spawn(p.getLocation().add(0, 1, 0), Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BURST)
                .withColor(Color.AQUA)
                .withFade(Color.WHITE)
                .trail(true)
                .build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);

        fw.detonate();
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);

        item.setAmount(item.getAmount() - 1);
    }

    /* ========================================================= */
    /* ===================== JOIN & LOBBY ======================= */
    /* ========================================================= */

    public void joinArena(Player p, Arena arena) {
        if (gameRunning) {
            p.sendMessage("§cGame already running!");
            return;
        }

        this.arena = arena;

        if (!inGame.contains(p.getUniqueId())) {
            inGame.add(p.getUniqueId());
            p.teleport(arena.getLobby());
            fullPlayerReset(p);
        }

        int minPlayers = plugin.getConfig().getInt("min-players", 2);
        broadcast("§e" + p.getName() + " joined! §7(" + inGame.size() + "/" +
                plugin.getConfig().getInt("max-players") + ")");

        if (inGame.size() >= minPlayers && !lobbyCountdownActive) {
            startLobbyCountdown();
        }
        ItemStack voteItem = createNamedItem(Material.DIAMOND, "§bVote Hunter", 1);
        p.getInventory().setItem(4, voteItem);
    }

    private void startLobbyCountdown() {
        lobbyCountdownActive = true;

        new BukkitRunnable() {
            int count = lobbyTime;

            public void run() {
                if (inGame.size() < plugin.getConfig().getInt("min-players", 2)) {
                    broadcast("§cNot enough players!");
                    lobbyCountdownActive = false;
                    cancel();
                    return;
                }

                for (UUID id : inGame) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) updateScoreboard(p, "Lobby", count);
                }

                if (count <= 0) {
                    startGame();
                    lobbyCountdownActive = false;
                    cancel();
                    return;
                }

                if (count % 10 == 0 || count <= 5)
                    broadcast("§6Starting in §e" + count + "§6 seconds");

                count--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }
    @EventHandler
    public void onVoteItemUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isInGame(p) || gameRunning) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.DIAMOND) return;
        if (!item.hasItemMeta() || !item.getItemMeta().getDisplayName().contains("Vote Hunter")) return;

        e.setCancelled(true);
        voteManager.openVoteGUI(p, inGame);
    }


    /* ========================================================= */
    /* ======================= GAME START ======================= */
    /* ========================================================= */

    private void startGame() {
        gameRunning = true;
        hidingPhase = true;
        elapsedSeconds = 0;

        List<UUID> players = new ArrayList<>(inGame);
        Collections.shuffle(players);

        UUID hunterId = voteManager.getWinningHunter(inGame);
        hunterPlayer = Bukkit.getPlayer(hunterId);
        hunters.add(hunterId);

        List<UUID> others = new ArrayList<>(inGame);
        others.remove(hunterId);
        seekers.addAll(others);

        voteManager.clearVotes();


        assignKits();
        startHidingPhase();
    }

    private void assignKits() {
        for (UUID id : inGame) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.getInventory().remove(Material.DIAMOND);
        }

        for (UUID id : seekers) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;

            p.teleport(arena.getSpawn());
            p.getInventory().clear();
            p.setHealth(20);

            p.getInventory().addItem(new ItemStack(Material.SPYGLASS));
            p.getInventory().addItem(createNamedItem(Material.FIREWORK_ROCKET, "§bFlash Rocket", 3));
            p.sendTitle("§aSEEKER", "Hide as a block!", 10, 60, 10);
        }

        if (hunterPlayer != null) {
            hunterPlayer.teleport(arena.getLobby());
            hunterPlayer.getInventory().clear();
            hunterPlayer.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));

            hunterPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, hideTime * 20, 1));
            hunterPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, hideTime * 20, 10));
            hunterPlayer.sendTitle("§cHUNTER", "Wait...", 10, 60, 10);
        }
    }

    private void startHidingPhase() {
        new BukkitRunnable() {
            int count = hideTime;

            public void run() {
                if (!gameRunning) { cancel(); return; }

                for (UUID id : inGame) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) updateScoreboard(p, "Hiding", count);
                }

                if (count <= 0) {
                    releaseHunter();
                    startMainTimer();
                    cancel();
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
            hunterPlayer.sendTitle("§cGO!", "Find the props!", 10, 40, 10);
        }
    }

    /* ========================================================= */
    /* ======================= MAIN TIMER ======================= */
    /* ========================================================= */

    private void startMainTimer() {
        timerTask = new BukkitRunnable() {
            int timeLeft = gameTime;

            public void run() {
                if (!gameRunning) { cancel(); return; }

                elapsedSeconds++;

                for (UUID id : inGame) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) updateScoreboard(p, "Finding", timeLeft);
                }

                handleHunterUpgrades(timeLeft);

                if (timeLeft <= 0) {
                    endGame(false);
                    cancel();
                    return;
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void handleHunterUpgrades(int timeLeft) {
        if (hunterPlayer == null) return;

        if (timeLeft == 400)
            hunterPlayer.getInventory().setItem(0, new ItemStack(Material.STONE_SWORD));
        else if (timeLeft == 200)
            hunterPlayer.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
        else if (timeLeft == 60) {
            hunterPlayer.getInventory().setItem(0, new ItemStack(Material.NETHERITE_SWORD));
            hunterPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 1));
        }
    }

    /* ========================================================= */
    /* ======================= DEATH / END ====================== */
    /* ========================================================= */

    public void handleDeath(Player p) {
        if (!isInGame(p)) return;

        if (isHunter(p)) {
            endGame(false);
        } else if (isSeeker(p)) {
            eliminateSeeker(p);
        }
    }

    public void eliminateSeeker(Player seeker) {
        seekers.remove(seeker.getUniqueId());
        spectators.add(seeker.getUniqueId());

        seeker.setGameMode(GameMode.SPECTATOR);
        seeker.getInventory().clear();
        disguiseManager.removeDisguise(seeker);
        pReset(seeker);

        if (hunterPlayer != null)
            statsManager.addKill(hunterPlayer.getUniqueId());

        if (seekers.isEmpty())
            endGame(true);
    }

    private void endGame(boolean hunterWon) {
        gameRunning = false;
        if (timerTask != null) timerTask.cancel();

        broadcast(hunterWon ? "§cHUNTER WINS!" : "§aSEEKERS WIN!");

        for (UUID id : inGame) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) fullPlayerReset(p);
        }

        disguiseManager.cleanupAll();
        inGame.clear();
        hunters.clear();
        seekers.clear();
        spectators.clear();
    }

    /* ========================================================= */
    /* ======================= UTILITIES ======================== */
    /* ========================================================= */

    public void fullPlayerReset(Player p) {
        p.getInventory().clear();
        p.setGameMode(GameMode.ADVENTURE);
        p.setHealth(20);
        p.setFoodLevel(20);
        p.setFireTicks(0);

        for (PotionEffect effect : p.getActivePotionEffects())
            p.removePotionEffect(effect.getType());

        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        disguiseManager.removeDisguise(p);
        pReset(p);

        if (arena != null) p.teleport(arena.getLobby());
    }

    private void pReset(Player p) {
        p.setInvisible(false);
        p.removePotionEffect(PotionEffectType.INVISIBILITY);

        for (Player online : Bukkit.getOnlinePlayers())
            online.showPlayer(plugin, p);
    }

    private void updateScoreboard(Player p, String status, int time) {
        statsManager.updateScoreboard(p, status, time, seekers.size(),
                arena != null ? arena.getName() : "None");
    }

    private ItemStack createNamedItem(Material mat, String name, int amt) {
        ItemStack item = new ItemStack(mat, amt);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
    public void leaveGame(Player p) {
        if (!isInGame(p)) return;

        UUID id = p.getUniqueId();

        fullPlayerReset(p);

        inGame.remove(id);
        hunters.remove(id);
        seekers.remove(id);
        spectators.remove(id);

        broadcast("§c" + p.getName() + " left the game.");

        // End game if one team left
        if (gameRunning) {
            if (hunters.isEmpty()) {
                endGame(false);
            } else if (seekers.isEmpty()) {
                endGame(true);
            }
        }
    }

    public boolean isInGame(Player p) { return inGame.contains(p.getUniqueId()); }
    public boolean isHunter(Player p) { return hunters.contains(p.getUniqueId()); }
    public boolean isSeeker(Player p) { return seekers.contains(p.getUniqueId()); }
    public boolean isGameRunning() { return gameRunning; }

    private void broadcast(String msg) {
        for (UUID id : inGame) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage("§6[PropHunt] " + msg);
        }
    }
    public Arena getArena() {
        return arena;
    }

    public void forceStop() {
        endGame(false);
    }
    public StatsManager getStatsManager() {
        return statsManager;
    }

}
