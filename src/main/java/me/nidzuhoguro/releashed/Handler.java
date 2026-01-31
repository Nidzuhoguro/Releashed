package me.nidzuhoguro.releashed;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class Handler implements Listener {

    private static final Releashed releashed = Releashed.getPlugin(Releashed.class);

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ArrayList<Pair> pairs = Pair.getAllPairs(player);

        for (Pair pair : pairs) {
            if (pair.submissive.equals(player)) player.damage(20.0); // Deals great damage to the sub if they disconnect.
            pair.dominant.getInventory().addItem(new ItemStack(Material.LEAD));
            pair.unleash(false);
        }
    }

    @EventHandler
    public void onPlayerDie(PlayerDeathEvent event) {
        ArrayList<Pair> pairs = Pair.getAllPairs(event.getEntity());

        for (Pair pair : pairs) {
            if (pair.isAttached() && event.getEntity() != pair.submissive) continue; // If the dominant left the sub tied to a fence and died, the sub stays tied.
            pair.unleash(false);
            pair.dominant.getInventory().addItem(new ItemStack(Material.LEAD));
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        ArrayList<Pair> pairs = Pair.getAllPairs(event.getPlayer());

        for (Pair pair : pairs) {
            if (pair.isAttached() && event.getPlayer() != pair.submissive) continue; // If the dominant left the sub tied to a fence and teleported away, the sub stays tied.
            pair.unleash(false);
            pair.dominant.getInventory().addItem(new ItemStack(Material.LEAD));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUnleash(EntityUnleashEvent event) {
        ArrayList<Pair> discard = new ArrayList<>();
        for (Pair pair : releashed.pairs) {
            if (pair.leashMount == null) continue; // Prevent a potential NullPointerException.
            if (pair.leashMount.equals(event.getEntity())) {
                if (pair.unleash(true)) discard.add(pair);
            }
        }

        releashed.pairs.removeAll(discard); // Fixed ConcurrentModificationException.
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        // Subs take damage equal to that they dealt to their dominants
        if (event.getEntity() instanceof Player) {
            ArrayList<Pair> pairs = Pair.getAllPairs((Player) event.getDamager());

            for (Pair pair : pairs) {
                if (pair.dominant.equals(event.getEntity())) ((Player) event.getDamager()).damage(event.getDamage());
            }
        }

        if (event.getEntity().getType().equals(EntityType.LEASH_KNOT)) {
            if (!Pair.getSubmissivePairs((Player) event.getDamager()).isEmpty()) event.setCancelled(true); // If the sub attacks their leash knot, cancel the event.
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST) // EXPERIMENTAL
    public void attachToFence(PlayerInteractEvent event) {
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getClickedBlock() != null) {
            if (event.getClickedBlock().getType().name().endsWith("_FENCE")) {
                if (event.getHand() == EquipmentSlot.OFF_HAND || Pair.getAllPairs(event.getPlayer()).isEmpty()) return;
                Location location = event.getClickedBlock().getLocation().add(0.0, 0.5, 0.0);
                Entity knot = event.getPlayer().getWorld().spawnEntity(location, EntityType.LEASH_KNOT);

                ArrayList<Pair> pairs = Pair.getAllPairs(event.getPlayer());

                for (Pair pair : pairs) {
                    pair.attachToBlock(location, knot, event.getClickedBlock());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHangingDamage(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player)) return; // Fixed potential ClassCastException.
        if (event.getEntity().getType().equals(EntityType.LEASH_KNOT) && !Pair.getSubmissivePairs((Player) event.getRemover()).isEmpty()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType().name().endsWith("_FENCE") && !Pair.getSubmissivePairs(event.getPlayer()).isEmpty()) { // If a sub tries to break a fence
            ArrayList<Pair> pairs = Pair.getAllPairs(event.getPlayer());
            for (Pair pair : pairs) {
                if (pair.getFence().equals(event.getBlock())) event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {

        Player player = event.getPlayer();

        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getRightClicked().getType() == EntityType.LEASH_KNOT) {

            if (!Pair.getSubmissivePairs(event.getPlayer()).isEmpty()) { // Subs cannot untie the leash knots
                event.setCancelled(true);
                return;
            }
            ArrayList<Pair> pairs = Pair.getAllPairs(event.getPlayer());

            for (Pair pair : pairs) {
                pair.detachFromBlock();
            }
            return;
        }

        if (!(event.getRightClicked() instanceof Player)) return;

        Entity target = event.getRightClicked();

        ItemStack domItem = player.getInventory().getItemInMainHand();

        ArrayList<Pair> domPairs = Pair.getAllPairs(player);
        ArrayList<Pair> subPairs = Pair.getAllPairs((Player) target);

        for (Pair pair : domPairs) {
            if (pair.submissive.equals(target)) {
                pair.unleash(false);
                player.getInventory().addItem(new ItemStack(Material.LEAD));
                return;
            }
        }

        if (domItem.getType() != Material.LEAD) return;

        for (Pair pair : subPairs) {
            if (!pair.isDominant((Player) target)) {
                player.sendMessage("This player is already leashed");
                return;
            }
        }

        for (Pair pair : domPairs) {
            if (pair.dominant.equals(target)) {
                player.sendMessage("You cannot leash your dominant~");
                return;
            }
        }

        Pair pair = new Pair(player, (Player) target);
        domItem.setAmount(domItem.getAmount() - 1);
        player.getInventory().setItemInMainHand(domItem);
        releashed.pairs.add(pair);
    }
}