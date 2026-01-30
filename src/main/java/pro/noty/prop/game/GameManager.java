package pro.noty.prop.game;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pro.noty.prop.PropHuntOptimized;
import pro.noty.prop.disguise.DisguiseManager;
import pro.noty.prop.arena.Arena;

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

    private BukkitTask mainTimerTask;
    private Player hunterPlayer;

    public GameManager(PropHuntOptimized plugin, DisguiseManager disguiseManager) {
        this.plugin = plugin;
        this.disguiseManager = disguiseManager;
        this.statsManager = new StatsManager();
        this.voteManager = new HunterVoteManager(plugin);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(voteManager, plugin);
    }

    /* ========================================================= */
    /* ===================== COMBAT SYSTEM ====================== */
    /* ========================================================= */

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!isInGame(victim) || !isInGame(attacker)) return;

        // Block friendly fire
        if ((isSeeker(victim) && isSeeker(attacker)) ||
                (isHunter(victim) && isHunter(attacker))) {
            e.setCancelled(true);
            return;
        }

        // Hunter kills seeker PROPERLY
        if (isHunter(attacker) && isSeeker(victim)) {
            e.setCancelled(true);
            eliminateSeeker(victim);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1, 1);
            return;
        }

        // Seekers cannot hurt hunter
        if (isSeeker(attacker) && isHunter(victim)) {
            e.setCancelled(true);
        }
    }

    /* ========================================================= */
    /* ======================= FIREWORKS ======================== */
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
            p.sendMessage("§cRocket cooldown!");
            return;
        }

        rocketCooldown.put(p.getUniqueId(), now);
        e.setCancelled(true);

        Firework fw = p.getWorld().spawn(p.getLocation().add(0,1,0), Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BURST)
                .withColor(Color.AQUA)
                .trail(true)
                .build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);
        fw.detonate();

        p.getWorld().spawnParticle(Particle.FIREWORK, p.getLocation(), 30);
        item.setAmount(item.getAmount() - 1);
    }

    /* ========================================================= */
    /* ===================== JOIN & LEAVE ======================= */
    /* ========================================================= */

    public void joinArena(Player p, Arena arena) {
        if (gameRunning) {
            p.sendMessage("§cGame already running!");
            return;
        }

        this.arena = arena;
        inGame.add(p.getUniqueId());
        p.teleport(arena.getLobby());
        fullPlayerReset(p);

        p.getInventory().setItem(4, createNamedItem(Material.DIAMOND, "§bVote Hunter", 1));
        broadcast("§e" + p.getName() + " joined! §7(" + inGame.size() + ")");

        if (inGame.size() >= plugin.getConfig().getInt("min-players", 2) && !lobbyCountdownActive)
            startLobbyCountdown();
    }

    public void leaveGame(Player p) {
        if (!isInGame(p)) return;

        fullPlayerReset(p);
        UUID id = p.getUniqueId();

        inGame.remove(id);
        hunters.remove(id);
        seekers.remove(id);
        spectators.remove(id);

        broadcast("§c" + p.getName() + " left!");

        if (gameRunning) {
            if (seekers.isEmpty()) endGame(true);
            if (hunters.isEmpty()) endGame(false);
        }
    }

    /* ========================================================= */
    /* ====================== LOBBY TIMER ======================= */
    /* ========================================================= */

    private void startLobbyCountdown() {
        lobbyCountdownActive = true;

        new BukkitRunnable() {
            int count = lobbyTime;

            public void run() {
                if (count <= 0) {
                    startGame();
                    cancel();
                    return;
                }

                broadcast("§6Starting in §e" + count + "s");
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

        UUID hunterId = voteManager.getWinningHunter(inGame);
        hunterPlayer = Bukkit.getPlayer(hunterId);
        hunters.add(hunterId);

        for (UUID id : inGame) if (!id.equals(hunterId)) seekers.add(id);
        voteManager.clearVotes();

        assignKits();
        startHidingPhase();
    }

    private void assignKits() {
        for (UUID id : inGame) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;

            p.getInventory().clear();

            if (isSeeker(p)) {
                p.teleport(arena.getSpawn());
                p.getInventory().addItem(new ItemStack(Material.SPYGLASS));
                p.getInventory().addItem(createNamedItem(Material.FIREWORK_ROCKET, "§bFlash Rocket", 64));
                p.getInventory().addItem(createNamedItem(Material.FIREWORK_ROCKET, "§bFlash Rocket", 64));
                p.sendTitle("§aSEEKER", "Hide as a block!", 10, 60, 10);
            } else {
                p.teleport(arena.getLobby());
                p.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, hideTime * 20, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, hideTime * 20, 10));
                p.sendTitle("§cHUNTER", "Wait...", 10, 60, 10);
            }
        }
    }

    private void startHidingPhase() {
        new BukkitRunnable() {
            int count = hideTime;
            public void run() {
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
        hunterPlayer.removePotionEffect(PotionEffectType.BLINDNESS);
        hunterPlayer.removePotionEffect(PotionEffectType.SLOWNESS);
        hunterPlayer.teleport(arena.getSpawn());
        hunterPlayer.sendTitle("§cGO!", "Find them!", 10, 40, 10);
    }

    /* ========================================================= */
    /* ======================= MAIN TIMER ======================= */
    /* ========================================================= */

    private void startMainTimer() {
        mainTimerTask = new BukkitRunnable() {
            int timeLeft = gameTime;
            public void run() {
                if (timeLeft <= 0) {
                    endGame(false);
                    cancel();
                    return;
                }
                handleHunterUpgrades(timeLeft);
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void handleHunterUpgrades(int timeLeft) {
        if (hunterPlayer == null) return;
        if (timeLeft == 400) hunterPlayer.getInventory().setItem(0, new ItemStack(Material.STONE_SWORD));
        if (timeLeft == 200) hunterPlayer.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
        if (timeLeft == 60) {
            hunterPlayer.getInventory().setItem(0, new ItemStack(Material.NETHERITE_SWORD));
            hunterPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 1));
        }
    }

    /* ========================================================= */
    /* ======================= END GAME ========================= */
    /* ========================================================= */

    public void eliminateSeeker(Player seeker) {
        seekers.remove(seeker.getUniqueId());
        spectators.add(seeker.getUniqueId());

        seeker.setGameMode(GameMode.SPECTATOR);
        seeker.getInventory().clear();
        disguiseManager.undisguise(seeker);

        statsManager.addKill(hunterPlayer.getUniqueId());

        if (seekers.isEmpty())
            endGame(true);
    }

    private void endGame(boolean hunterWon) {
        gameRunning = false;
        if (mainTimerTask != null) mainTimerTask.cancel();

        broadcast(hunterWon ? "§cHUNTER WINS!" : "§aSEEKERS WIN!");

        for (UUID id : inGame) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) fullPlayerReset(p);
        }

        disguiseManager.removeAll();
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

        disguiseManager.undisguise(p);
        if (arena != null) p.teleport(arena.getLobby());
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

    public boolean isInGame(Player p) { return inGame.contains(p.getUniqueId()); }
    public boolean isHunter(Player p) { return hunters.contains(p.getUniqueId()); }
    public boolean isSeeker(Player p) { return seekers.contains(p.getUniqueId()); }

    private void broadcast(String msg) {
        for (UUID id : inGame) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage("§6[PropHunt] " + msg);
        }
    }
    // Needed by other classes
    public boolean isGameRunning() {
        return gameRunning;
    }

    public void handleDeath(Player p) {
        if (!isInGame(p)) return;

        if (isHunter(p)) {
            endGame(false);
        } else if (isSeeker(p)) {
            eliminateSeeker(p);
        }
    }


    public Arena getArena() { return arena; }
    public StatsManager getStatsManager() { return statsManager; }
    public void forceStop() { endGame(false); }
}
