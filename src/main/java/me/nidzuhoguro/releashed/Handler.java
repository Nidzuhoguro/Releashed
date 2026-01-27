package me.nidzuhoguro.releashed;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class Handler implements Listener {

    private static final Releashed releashed = Releashed.getPlugin(Releashed.class);

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        ArrayList<Pair> pairs = Pair.getAllPairs(event.getPlayer());

        for (Pair pair : pairs) {
            pair.update();
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ArrayList<Pair> pairs = Pair.getAllPairs(player);

        for (Pair pair : pairs) {
            if (pair.submissive.equals(player)) player.damage(10.0);
            pair.dominant.getInventory().addItem(new ItemStack(Material.LEAD));
            pair.unleash();
        }
    }

    @EventHandler
    public void onPlayerDie(PlayerDeathEvent event) {
        ArrayList<Pair> pairs = Pair.getAllPairs(event.getEntity());

        for (Pair pair : pairs) {
            pair.unleash();
            pair.dominant.getInventory().addItem(new ItemStack(Material.LEAD));
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        ArrayList<Pair> pairs = Pair.getAllPairs(event.getPlayer());

        for (Pair pair : pairs) {
            pair.unleash();
            pair.dominant.getInventory().addItem(new ItemStack(Material.LEAD));
        }
    }

    @EventHandler
    public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
        ArrayList<Pair> pairs = Pair.getAllPairs(event.getPlayer());

        for (Pair pair : pairs) {
            pair.unleash();
            pair.dominant.getInventory().addItem(new ItemStack(Material.LEAD));
        }
    }

    @EventHandler
    public void onUnleash(EntityUnleashEvent event) {
        for (Pair pair : releashed.pairs) {
            if (pair.leashMount.equals(event.getEntity())) {
                pair.unleash();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {

        Player dominant = event.getPlayer();
        Entity submissive = event.getRightClicked();

        ItemStack domItem = dominant.getInventory().getItemInMainHand();

        ArrayList<Pair> domPairs = Pair.getAllPairs(dominant);
        ArrayList<Pair> subPairs = Pair.getAllPairs((Player) submissive);

        for (Pair pair : domPairs) {
            if (pair.leashMount.equals(submissive)) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getHand() == EquipmentSlot.OFF_HAND || !(event.getRightClicked() instanceof Player)) return;

        for (Pair pair : domPairs) {
            if (pair.submissive.equals(submissive)) {
                pair.unleash();
                dominant.getInventory().addItem(new ItemStack(Material.LEAD));
                return;
            }
        }

        if (domItem.getType() != Material.LEAD) return;

        for (Pair pair : subPairs) {
            if (!pair.isDominant((Player) submissive)) {
                dominant.sendMessage("This player is already leashed");
                return;
            }
        }

        for (Pair pair : domPairs) {
            if (pair.dominant.equals(submissive)) {
                dominant.sendMessage("You cannot leash your dominant~");
                return;
            }
        }

        Pair pair = new Pair(dominant, (Player) submissive);
        domItem.setAmount(domItem.getAmount() - 1);
        dominant.getInventory().setItemInMainHand(domItem);
        releashed.pairs.add(pair);
    }
}
