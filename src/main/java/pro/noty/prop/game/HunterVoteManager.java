package pro.noty.prop.game;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.SkullMeta;
import pro.noty.prop.PropHuntOptimized;

import java.util.*;

public class HunterVoteManager implements Listener {

    // Fixed: Using the specific class type so getGameManager() is accessible
    private final PropHuntOptimized plugin;
    private final Map<UUID, UUID> votes = new HashMap<>(); // voter -> voted

    public HunterVoteManager(PropHuntOptimized plugin) {
        this.plugin = plugin;
    }

    public void openVoteGUI(Player viewer, Set<UUID> players) {
        // Create a 3-row inventory
        Inventory inv = Bukkit.createInventory(null, 27, "§8Vote for Hunter");

        for (UUID id : players) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(p);
                meta.setDisplayName("§eVote §f" + p.getName());
                head.setItemMeta(meta);
            }

            inv.addItem(head);
        }

        viewer.openInventory(inv);
    }

    public void castVote(Player voter, Player target) {
        votes.put(voter.getUniqueId(), target.getUniqueId());
        voter.sendMessage("§aYou voted for §e" + target.getName());
        voter.playSound(voter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    public UUID getWinningHunter(Set<UUID> players) {
        Map<UUID, Integer> counts = new HashMap<>();

        for (UUID voted : votes.values()) {
            if (!players.contains(voted)) continue;
            counts.put(voted, counts.getOrDefault(voted, 0) + 1);
        }

        // If no one voted, pick a random player from the set
        if (counts.isEmpty()) {
            List<UUID> playerList = new ArrayList<>(players);
            return playerList.get(new Random().nextInt(playerList.size()));
        }

        int highest = Collections.max(counts.values());
        List<UUID> top = new ArrayList<>();

        for (Map.Entry<UUID, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == highest) top.add(entry.getKey());
        }

        // Randomly pick one of the players tied for first place
        return top.get(new Random().nextInt(top.size()));
    }

    @EventHandler
    public void onVoteClick(InventoryClickEvent e) {
        // Check if this is our voting inventory
        if (!e.getView().getTitle().equals("§8Vote for Hunter")) return;

        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player voter)) return;

        // Check game state via the casted plugin variable
        if (plugin.getGameManager().isGameRunning()) {
            voter.closeInventory();
            voter.sendMessage("§cYou cannot vote after the game has started!");
            return;
        }

        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() != Material.PLAYER_HEAD) return;

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null || meta.getOwningPlayer() == null) return;

        // Get the player associated with the head clicked
        Player target = meta.getOwningPlayer().getPlayer();
        if (target == null) {
            voter.sendMessage("§cThat player is no longer online.");
            return;
        }

        // Call the local method directly
        castVote(voter, target);
        voter.closeInventory();
    }

    public void clearVotes() {
        votes.clear();
    }
}
