package me.nidzuhoguro.releashed;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class Releashed extends JavaPlugin {

    private static final Map<UUID, Pair> subPairs = new HashMap<>(); // Sub -> Pair
    private static final Map<UUID, List<UUID>> domPairs = new HashMap<>(); // Dom -> Subs
    private final List<UUID> removal = new ArrayList<>(); // List of UUIDs of subs whose pairs to remove the next tick.
    private final List<Pair> queue = new ArrayList<>();
    private static BukkitTask ticker;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new Handler(this), this);
        ticker = new BukkitRunnable() {
            public void run() {
                for (UUID uuid : removal) {
                    if (domPairs.containsKey(subPairs.get(uuid).getDomUUID())) {
                        domPairs.get(subPairs.get(uuid).getDomUUID()).remove(subPairs.get(uuid).getSubUUID());
                    }
                    subPairs.remove(uuid);
                }
                removal.clear();

                for (Pair pair : subPairs.values()) {
                    if (pair.getDominantPlayer() != null && !pair.isAttached()) {
                        if (!pair.getDominantPlayer().getWorld().equals(pair.getSubmissivePlayer().getWorld())) {
                            pair.unleash();
                            pair.getDominantPlayer().getInventory().addItem(new ItemStack(Material.LEAD));
                        }
                    }
                    pair.tick();
                }

                if (queue.isEmpty()) return;

                for (Pair pair : queue) {
                    subPairs.put(pair.getSubmissivePlayer().getUniqueId(), pair);
                    if (domPairs.containsKey(pair.getDomUUID())) {
                        domPairs.get(pair.getDomUUID()).add(pair.getSubUUID());
                        continue;
                    }
                    List<UUID> pairs = new ArrayList<>();
                    pairs.add(pair.getSubUUID());
                    domPairs.put(pair.getDomUUID(), pairs);
                }
                queue.clear();
            }
        }.runTaskTimer(this, 0L, 1L); // Experimental new pair updater independent of players' movement.
    }

    /// # Releashed.getSubPair()
    /// Returns the pair where the target player is submissive, might return null. Time complexity is O(1).
    public Pair getSubPair(UUID player) {
        if (!subPairs.containsKey(player)) return null;
        return subPairs.get(player);
    }

    /// # Releashed.getDomPairs()
    /// Returns all pairs where the target player is dominant, might return an empty List. Time complexity is O(n) where 'n' is the number of subs associated with this dom, not depending on the total count of dominants overall.
    public List<Pair> getDomPairs(UUID player) {
        List<Pair> pairs = new ArrayList<>();
        List<UUID> subs = domPairs.get(player); // O(1).

        if (subs == null) return pairs;
        for (UUID uuid : subs) { // O(n)
            Pair pair = subPairs.get(uuid);
            if (pair == null) continue;
            pairs.add(pair);
        }
        return pairs;
    }

    /// # Releashed.getAllPairs()
    /// Returns a List of all pairs where the target player is either dominant or submissive.
    public List<Pair> getAllPairs(UUID player) {
        List<Pair> pairs = new ArrayList<>();
        Pair subPair = getSubPair(player);
        if (getSubPair(player) != null) {
            pairs.add(subPair);
            return pairs;
        }

        pairs.addAll(getDomPairs(player));
        return pairs;
    }

    public Collection<Pair> getPairCollection() {
        return subPairs.values();
    }

    /// # Releashed.removePair()
    /// Removes a pair by UUID of the submissive player in it.
    public void removePair(UUID player) {
        removal.add(player);
    }

    public void addPair(Pair pair) {
        queue.add(pair);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        ticker.cancel();
    }
}
