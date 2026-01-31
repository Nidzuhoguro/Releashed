package me.nidzuhoguro.releashed;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;

public final class Releashed extends JavaPlugin {

    public final ArrayList<Pair> pairs = new ArrayList<>();
    private static BukkitTask ticker;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new Handler(), this);
        ticker = new BukkitRunnable() {
            public void run() {
                for (Pair pair : pairs) {
                    if (!pair.dominant.getWorld().equals(pair.submissive.getWorld())) { // New, more reliable dimension mismatch check.
                        pair.unleash(false);
                        pair.dominant.getInventory().addItem(new ItemStack(Material.LEAD, 1));
                    }
                    pair.update();
                }
            }
        }.runTaskTimer(this, 0L, 1L); // Experimental new pair updater independent of players' movement.
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        ticker.cancel();
    }
}
