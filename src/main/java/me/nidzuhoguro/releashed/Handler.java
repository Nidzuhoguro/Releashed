package me.nidzuhoguro.releashed;

import org.bukkit.Bukkit;
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

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class Handler implements Listener {

    private final Releashed releashed = Releashed.PLUGIN;

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        releashed.getSubPairs(player.getUniqueId()).forEach(p -> player.damage(20.0));
    }

    @EventHandler
    public void onPlayerDie(PlayerDeathEvent event) {
        UUID player = event.getEntity().getUniqueId();
        for (Pair pair : releashed.getAllPairs(event.getEntity().getUniqueId())) {
            if (player.equals(pair.dominantID) && !pair.isAttached()) pair.invalidate();
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        float distance = event.getTo() == null ? 100.0f : (float) event.getFrom().distance(event.getTo());

        for (Pair pair : releashed.getAllPairs(player)) {
            if (!pair.isAttached() && pair.isDominant(player)) {
                pair.invalidate();
                continue;
            }

            if (distance > 40.0f) pair.invalidate();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUnleash(EntityUnleashEvent event) {
        for (Pair pair : releashed.pairs.values().stream().flatMap(Collection::stream).toList()) {
            if (pair.leashMount == null) continue; // Prevent a potential NullPointerException.
            if (pair.leashMount.equals(event.getEntity())) {
                pair.invalidate();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;

        List<Pair> subPairs = releashed.getSubPairs(event.getDamager().getUniqueId());

        for (Pair pair : subPairs) {
            if (pair.dominantID == event.getEntity().getUniqueId())
                ((Player) event.getDamager()).damage(event.getDamage());
        }

        if (event.getEntity().getType().equals(EntityType.LEASH_KNOT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void attachToFence(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        List<Pair> domPairs = releashed.getDomPairs(event.getPlayer().getUniqueId());
        if (!(event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) || event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getType().name().endsWith("_FENCE"))) return;

        Location location = event.getClickedBlock().getLocation().add(0.0, 0.5, 0.0);
        Entity knot = event.getPlayer().getWorld().spawnEntity(location, EntityType.LEASH_KNOT);

        for (Pair pair : domPairs) {
            pair.attachToBlock(location, knot, event.getClickedBlock());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHangingDamage(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player)) return; // Fixed potential ClassCastException.
        if (!event.getEntity().getType().equals(EntityType.LEASH_KNOT)) return;

        UUID player = event.getRemover().getUniqueId();
        List<Pair> subPairs = releashed.getSubPairs(player);

        if (!subPairs.isEmpty()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        List<Pair> subPairs = releashed.getSubPairs(player);
        if (event.getBlock().getType().name().endsWith("_FENCE") && !subPairs.isEmpty()) { // If a sub tries to break a fence
            for (Pair pair : subPairs) {
                if (pair.getFence().equals(event.getBlock())) event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        UUID actorID = event.getPlayer().getUniqueId();

        if (event.getRightClicked().getType() == EntityType.LEASH_KNOT) {
            for (Pair pair : releashed.getDomPairs(actorID)) {
                pair.detachFromBlock();
            }
            return;
        }

        Entity clicked = event.getRightClicked();

        if (!(clicked instanceof Player)) return;

        UUID targetID = clicked.getUniqueId();
        UUID actorDominant = releashed.getDominant(actorID);

        if (targetID.equals(actorDominant)) {
            Player actor = Bukkit.getPlayer(actorID);
            if (actor == null) return;
            actor.sendMessage("You cannot leash your dominant!");
            return;
        }

        List<Pair> actorDomPairs = releashed.getDomPairs(actorID).stream().filter(p -> p.submissiveID.equals(targetID)).toList();

        if (actorDomPairs.isEmpty()) {
            Player dominant = Bukkit.getPlayer(actorID);
            if (dominant == null) return;
            ItemStack domItem = dominant.getInventory().getItemInMainHand();
            if (domItem.getType() != Material.LEAD) return;
            domItem.setAmount(domItem.getAmount() - 1);
            dominant.getInventory().setItemInMainHand(domItem);
            releashed.pair(actorID, targetID);
            return;
        }

        actorDomPairs.forEach(releashed::unpair);
    }
}