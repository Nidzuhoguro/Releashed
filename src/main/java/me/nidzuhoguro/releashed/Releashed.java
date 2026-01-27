package me.nidzuhoguro.releashed;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public final class Releashed extends JavaPlugin {

    public final ArrayList<Pair> pairs = new ArrayList<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new Handler(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
