package me.nidzuhoguro.releashed;

import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Releashed extends JavaPlugin {

    //public final Path config = getDataFolder().toPath().resolve("config.json");
    public static final Releashed PLUGIN = getPlugin(Releashed.class);
    public final Map<UUID, List<Pair>> pairs = new HashMap<>();
    public final Map<UUID, UUID> subToDomMap = new HashMap<>();
    private final PluginLogger logger = new PluginLogger(this);
    private static BukkitTask ticker;

    public List<Pair> getDomPairs(UUID player) {
        List<Pair> result = pairs.get(player);
        return result == null ? new ArrayList<>() : result;
    }

    public List<Pair> getSubPairs(UUID player) {
        UUID subToDom = subToDomMap.get(player);
        if (subToDom == null) return new ArrayList<>();
        return pairs.get(subToDomMap.get(player)).stream().filter(p -> p.submissiveID == player).toList();
    }

    public List<Pair> getAllPairs(UUID player) {
        return Stream.concat(getDomPairs(player).stream(), getSubPairs(player).stream()).collect(Collectors.toList());
    }

    @Nullable
    public UUID getDominant(UUID submissive) {
        return subToDomMap.get(submissive);
    }

    public void unpair(Pair pair) {
        pair.unleash();
        subToDomMap.remove(pair.submissiveID);
        if (pairs.get(pair.dominantID).size() > 1) {
            pairs.get(pair.dominantID).remove(pair);
            return;
        }
        pairs.remove(pair.dominantID);
    }

    public void pair (UUID dominant, UUID submissive) {
        if (subToDomMap.get(submissive) != null) return;
        logger.info("Added a new pair");
        subToDomMap.put(submissive, dominant);
        if (pairs.get(dominant) != null) {
            pairs.get(dominant).add(new Pair(dominant, submissive));
            return;
        }
        pairs.put(dominant, List.of(new Pair(dominant, submissive)));
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new Handler(), this);
        ticker = new BukkitRunnable() {
            public void run() {
                for (Pair pair : pairs.values().stream().flatMap(Collection::stream).toList()) {
                    if (!pair.isValid()) {
                        unpair(pair);
                        logger.info("Removed an invalidated pair");
                    }
                    pair.update();
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        ticker.cancel();
    }
}
