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

import java.util.*;
import java.util.stream.Collectors;

public class GameManager implements Listener {

    private final PropHuntOptimized plugin;
    private final Map<String, GameSession> games = new HashMap<>();
    private final NamespacedKey undroppableKey;

    public GameManager(PropHuntOptimized plugin) {
        this.plugin = plugin;
        this.undroppableKey = new NamespacedKey(plugin, "undroppable");
    }

    /* ================= JOIN ================= */

    public void joinArena(Player p, String name) {
        Arena arena = plugin.getArenaManager().getArena(name);
        if (arena == null) {
            p.sendMessage("§cArena not found");
            return;
        }

        arena.getPlayers().add(p.getUniqueId());
        p.teleport(arena.getLobby());
        p.setGameMode(GameMode.ADVENTURE);

        if (arena.getPlayers().size() >= plugin.getConfig().getInt("min-players")) {
            startCountdown(arena);
        }
    }

    /* ================= COUNTDOWN ================= */

    private void startCountdown(Arena arena) {
        int seconds = plugin.getConfig().getInt("countdown-seconds");

        new BukkitRunnable() {
            int time = seconds;

            public void run() {
                if (time <= 0) {
                    startGame(arena);
                    cancel();
                    return;
                }

                arena.getPlayers().forEach(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.sendMessage("§eGame starts in §c" + time + "s");
                });
                time--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    /* ================= START GAME ================= */

    private void startGame(Arena arena) {
        List<Player> players = arena.getPlayers().stream().map(Bukkit::getPlayer)
                .filter(Objects::nonNull).collect(Collectors.toList());

        Player hunter = players.get(new Random().nextInt(players.size()));
        GameSession session = new GameSession(arena, hunter);
        games.put(arena.getName(), session);

        for (Player p : players) {
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);

            if (p.equals(hunter)) giveHunterKit(p);
            else {
                giveSeekerKit(p);
                session.seekers.add(p.getUniqueId());
            }
        }

        applyBlindness(session);
    }

    /* ================= BLINDNESS ================= */

    private void applyBlindness(GameSession s) {
        int blind = plugin.getConfig().getInt("blindness-seconds");
        Player hunter = Bukkit.getPlayer(s.hunter);

        hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blind * 20, 1));

        BossBar bar = Bukkit.createBossBar("Seekers hiding: " + blind, BarColor.YELLOW, BarStyle.SOLID);
        s.bar = bar;
        s.getAll().forEach(bar::addPlayer);

        new BukkitRunnable() {
            int t = blind;

            public void run() {
                if (t <= 0) {
                    bar.removeAll();
                    startMainTimer(s);
                    cancel();
                    return;
                }
                bar.setTitle("Seekers hiding: " + t);
                t--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    /* ================= MAIN TIMER ================= */

    private void startMainTimer(GameSession s) {
        int total = plugin.getConfig().getInt("game-time-seconds");
        BossBar bar = Bukkit.createBossBar("Time Left: " + total, BarColor.RED, BarStyle.SOLID);
        s.bar = bar;
        s.getAll().forEach(bar::addPlayer);

        new BukkitRunnable() {
            int t = total;

            public void run() {
                if (t <= 0) {
                    endGame(s, false);
                    cancel();
                    return;
                }
                bar.setProgress(t / (double) total);
                bar.setTitle("Seekers: " + s.seekers.size() + " | Time: " + t);
                t--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    /* ================= KITS ================= */

    private void giveHunterKit(Player p) {
        p.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
    }

    private void giveSeekerKit(Player p) {
        p.getInventory().addItem(tag(new ItemStack(Material.SPYGLASS)));
        p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));

        ItemStack rocket = new ItemStack(Material.FIREWORK_ROCKET, 5);
        FireworkMeta meta = (FireworkMeta) rocket.getItemMeta();
        meta.setPower(1);
        rocket.setItemMeta(meta);
        p.getInventory().addItem(rocket);
    }

    private ItemStack tag(ItemStack i) {
        ItemMeta m = i.getItemMeta();
        m.getPersistentDataContainer().set(undroppableKey, PersistentDataType.INTEGER, 1);
        i.setItemMeta(m);
        return i;
    }

    /* ================= MORPH SYSTEM ================= */

    @EventHandler
    public void onSpyglass(PlayerInteractEvent e) {
        if (e.getItem() == null || e.getItem().getType() != Material.SPYGLASS) return;

        Player p = e.getPlayer();
        Block target = p.getTargetBlockExact(10);

        // Look at sky → remove morph
        if (target == null || target.getType() == Material.AIR) {
            plugin.getDisguiseManager().removeDisguise(p);
            p.sendMessage("§eReturned to normal form");
            return;
        }

        Material m = target.getType();
        if (!m.isBlock() || m.name().contains("PORTAL")) return;

        plugin.getDisguiseManager().disguise(p, m);
        p.sendMessage("§aMorphed into §e" + m.name());
    }

    /* ================= HUNTER HIT ================= */

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player hunter)) return;
        if (!(e.getEntity() instanceof Player seeker)) return;

        GameSession s = getSession(hunter);
        if (s == null || !s.seekers.contains(seeker.getUniqueId())) return;

        plugin.getDisguiseManager().removeDisguise(seeker);
        s.seekers.remove(seeker.getUniqueId());
        seeker.setGameMode(GameMode.SPECTATOR);

        if (s.seekers.isEmpty()) endGame(s, true);
    }

    /* ================= DEATH ================= */

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        plugin.getDisguiseManager().removeDisguise(e.getEntity());
    }

    /* ================= END GAME ================= */

    private void endGame(GameSession s, boolean hunterWon) {
        Player hunter = Bukkit.getPlayer(s.hunter);
        List<Player> seekers = s.getSeekersOnline();

        Player winner = hunterWon ? hunter : seekers.get(0);
        Player loser = hunterWon ? seekers.get(0) : hunter;

        for (String cmd : plugin.getConfig().getStringList("end-commands")) {
            cmd = cmd.replace("%winner%", winner.getName())
                    .replace("%loser%", loser.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        s.getAll().forEach(p -> {
            plugin.getDisguiseManager().removeDisguise(p);
            p.getInventory().clear();
            p.teleport(s.arena.getLobby());
            p.setGameMode(GameMode.ADVENTURE);
        });

        s.bar.removeAll();
        games.remove(s.arena.getName());
        s.arena.getPlayers().clear();
    }

    /* ================= UTIL ================= */

    private GameSession getSession(Player p) {
        return games.values().stream().filter(s -> s.getAll().contains(p)).findFirst().orElse(null);
    }

    @EventHandler public void onDrop(PlayerDropItemEvent e) {
        if (e.getItemDrop().getItemStack().hasItemMeta() &&
                e.getItemDrop().getItemStack().getItemMeta().getPersistentDataContainer()
                        .has(undroppableKey, PersistentDataType.INTEGER)) e.setCancelled(true);
    }

    @EventHandler public void onPortal(PlayerPortalEvent e) { e.setCancelled(true); }

    /* ================= SESSION CLASS ================= */

    private static class GameSession {
        Arena arena;
        UUID hunter;
        Set<UUID> seekers = new HashSet<>();
        BossBar bar;

        GameSession(Arena arena, Player hunter) {
            this.arena = arena;
            this.hunter = hunter.getUniqueId();
        }

        List<Player> getAll() {
            List<Player> list = new ArrayList<>();
            Player h = Bukkit.getPlayer(hunter);
            if (h != null) list.add(h);
            list.addAll(getSeekersOnline());
            return list;
        }

        List<Player> getSeekersOnline() {
            return seekers.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).collect(Collectors.toList());
        }
    }
}
