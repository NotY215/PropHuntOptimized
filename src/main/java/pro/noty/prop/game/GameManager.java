package pro.noty.prop.game;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import pro.noty.prop.PropHuntOptimized;
import pro.noty.prop.arena.Arena;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;
import java.util.stream.Collectors;

public class GameManager implements Listener {

    private final PropHuntOptimized plugin;
    private final NamespacedKey undroppableKey;
    private final GameSession session;

    public GameManager(PropHuntOptimized plugin) {
        this.plugin = plugin;
        this.undroppableKey = new NamespacedKey(plugin, "undroppable");
        this.session = new GameSession();
    }

    /* ================= JOIN ================= */
    // Handles only 1 arena for now
    public void joinArena(Player p, Arena arena) {
        if (session.inGame) {
            p.sendMessage("§cGame is already in progress!");
            return;
        }
        if (arena.getPlayers().contains(p.getUniqueId())) {
            p.sendMessage("§cYou already joined this game!");
            return;
        }
        arena.getPlayers().add(p.getUniqueId());
        p.teleport(arena.getLobby());
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().addItem(createLeaveDiamond());
        p.sendMessage("§aYou joined arena " + arena.getName());
        session.players.add(p);

        if (arena.getPlayers().size() >= plugin.getConfig().getInt("min-players")) {
            startCountdown(arena);
        }
    }

    private ItemStack createLeaveDiamond() {
        ItemStack diamond = new ItemStack(Material.DIAMOND);
        ItemMeta meta = diamond.getItemMeta();
        meta.setDisplayName("§cLeave Game");
        diamond.setItemMeta(meta);
        return diamond;
    }

    /* ================= LEAVE ================= */

    public void leaveGame(Player p) {
        if (!session.players.contains(p)) {
            p.sendMessage("§cYou are not in the game!");
            return;
        }
        session.players.remove(p);
        p.getInventory().clear();
        p.setGameMode(GameMode.ADVENTURE);
        p.teleport(session.arena.getLobby());
        p.sendMessage("§aYou left the game!");

        if (session.players.size() < plugin.getConfig().getInt("min-players") && session.countdownTask != null) {
            session.countdownTask.cancel();
            broadcast("§cCountdown stopped because not enough players!");
        }
    }

    /* ================= COUNTDOWN ================= */

    private void startCountdown(Arena arena) {
        if (session.countdownTask != null) session.countdownTask.cancel();
        session.countdownTime = plugin.getConfig().getInt("countdown-seconds");
        session.countdownTask = new BukkitRunnable() {
            public void run() {
                if (session.players.size() < plugin.getConfig().getInt("min-players")) {
                    cancel();
                    broadcast("§cCountdown stopped!");
                    return;
                }
                if (session.countdownTime <= 0) {
                    cancel();
                    startGame(arena);
                    return;
                }
                broadcast("§eGame starts in §c" + session.countdownTime + "s");
                session.countdownTime--;
            }
        };
        session.countdownTask.runTaskTimer(plugin, 0, 20);
    }

    /* ================= GAME START ================= */

    private void startGame(Arena arena) {
        List<Player> players = arena.getPlayers().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (players.size() < 2) return;

        Player hunter = players.get(new Random().nextInt(players.size()));
        hunters.put(arena.getName(), hunter.getUniqueId());

        for (Player p : players) {
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);

            if (p.equals(hunter)) {
                // Hunter stays in lobby
                p.teleport(arena.getLobby());
                giveHunterKit(p);

                // Blindness during hiding phase
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * hidingSeconds, 1, false, false));
                p.sendTitle("§cYou are the Hunter!", "§7Wait for seekers to hide...", 10, 60, 10);

            } else {
                // Seekers go to arena spawn FIRST
                p.teleport(arena.getSpawn());
                giveSeekerKit(p);
                seekers.add(p.getUniqueId());
                p.sendTitle("§aYou are a Seeker!", "§7Hide quickly!", 10, 60, 10);
            }
        }

        startHidingCountdown(arena, hunter);
    }


    private void startHidingCountdown(Arena arena, Player hunter) {
        new BukkitRunnable() {
            int time = hidingSeconds;

            public void run() {
                if (time <= 0) {
                    // Release hunter into arena
                    hunter.teleport(arena.getSpawn());
                    hunter.removePotionEffect(PotionEffectType.BLINDNESS);
                    hunter.sendTitle("§cHUNT STARTED!", "", 5, 40, 5);

                    startMainGameTimer(arena);
                    cancel();
                    return;
                }

                for (UUID uuid : seekers) {
                    Player seeker = Bukkit.getPlayer(uuid);
                    if (seeker != null)
                        seeker.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                new TextComponent("§eHiding Time: §c" + time + "s"));
                }

                time--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }


    /* ================= MAIN TIMER ================= */

    private void startMainTimer() {
        int gameTime = plugin.getConfig().getInt("game-time-seconds");
        session.gameBar = Bukkit.createBossBar("Time Left: " + gameTime, BarColor.RED, BarStyle.SOLID);
        session.getAll().forEach(session.gameBar::addPlayer);

        new BukkitRunnable() {
            int t = gameTime;
            public void run() {
                if (t <= 0) { endGame(false); cancel(); return; }
                session.gameBar.setProgress(t/(double)gameTime);
                session.gameBar.setTitle("Seekers: "+session.seekers.size()+" | Time: "+t);
                t--;
            }
        }.runTaskTimer(plugin,0,20);
    }

    /* ================= KITS ================= */

    private void giveHunterKit(Player p) { p.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD)); }
    private void giveSeekerKit(Player p) {
        p.getInventory().addItem(new ItemStack(Material.SPYGLASS));
        p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF,16));
        ItemStack rocket = new ItemStack(Material.FIREWORK_ROCKET,5);
        FireworkMeta meta = (FireworkMeta) rocket.getItemMeta(); meta.setPower(1); rocket.setItemMeta(meta);
        p.getInventory().addItem(rocket);
    }

    /* ================= MORPH SYSTEM ================= */

    @EventHandler
    public void onSpyglass(PlayerInteractEvent e) {
        if (e.getItem()==null||e.getItem().getType()!=Material.SPYGLASS) return;
        Player p = e.getPlayer();
        Block target = p.getTargetBlockExact(10);

        // Sky => remove morph
        if (target==null||target.getType()==Material.AIR) {
            plugin.getDisguiseManager().removeDisguise(p);
            p.sendMessage("§eReturned to normal form");
            return;
        }

        if (!target.getType().isBlock()) return;
        plugin.getDisguiseManager().disguise(p,target.getType());
    }

    /* ================= HUNTER HIT ================= */

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player hunter)) return;
        if (!(e.getEntity() instanceof Player seeker)) return;
        if (!session.seekers.contains(seeker)) return;

        plugin.getDisguiseManager().removeDisguise(seeker);
        session.seekers.remove(seeker);
        seeker.setGameMode(GameMode.SPECTATOR);
        checkEndConditions();
    }

    private void checkEndConditions() {
        if (session.seekers.isEmpty()) endGame(true);
        if (session.hunter.isDead()) endGame(false);
    }

    /* ================= DEATH ================= */

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        plugin.getDisguiseManager().removeDisguise(p);
        if (p.equals(session.hunter)) endGame(false);
        if (session.seekers.contains(p)) session.seekers.remove(p);
        checkEndConditions();
    }

    /* ================= END GAME ================= */

    private void endGame(boolean hunterWon) {
        Player winner = hunterWon ? session.hunter : session.seekers.get(0);
        Player loser = hunterWon ? session.seekers.get(0) : session.hunter;

        for (String cmd: plugin.getConfig().getStringList("end-commands")) {
            cmd = cmd.replace("%winner%",winner.getName()).replace("%loser%",loser.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        session.getAll().forEach(p->{
            plugin.getDisguiseManager().removeDisguise(p);
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            p.teleport(session.arena.getLobby());
        });
        if (session.gameBar!=null) session.gameBar.removeAll();
        session.reset();
    }

    /* ================= UTILITIES ================= */

    private void broadcast(String msg) { session.getAll().forEach(p->p.sendMessage(msg)); }

    /* ================= SESSION CLASS ================= */

    private static class GameSession {
        Arena arena;
        Player hunter;
        List<Player> seekers = new ArrayList<>();
        List<Player> players = new ArrayList<>();
        boolean inGame = false;
        BossBar gameBar;
        BukkitRunnable countdownTask;
        int countdownTime;

        void reset() { inGame=false; players.clear(); seekers.clear(); hunter=null; countdownTask=null; countdownTime=0; }
        List<Player> getAll() { List<Player> all=new ArrayList<>(players); if(hunter!=null) all.add(hunter); return all; }
    }
}
