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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class Handler implements Listener {

    private final Releashed releashed = JavaPlugin.getPlugin(Releashed.class);

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        releashed.getSubPairs(player.getUniqueId()).forEach(p -> player.damage(releashed.getConfig().getInt("logoutDamage")));
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
        UUID actorID = event.getPlayer().getUniqueId(); // The UUID of the player who triggered the event.

        if (event.getHand() == EquipmentSlot.OFF_HAND) return; // Only execute the code if the event was triggered by doing a main hand action.

        if (event.getRightClicked().getType() == EntityType.LEASH_KNOT) { // If the target of the interaction is a leash knot.
            for (Pair pair : releashed.getDomPairs(actorID)) {
                pair.detachFromBlock();
            }
            return;
        }

        Entity clicked = event.getRightClicked();

        if (!(clicked instanceof Player)) return; // Everything else is done only if the target is a player.

        UUID targetID = clicked.getUniqueId(); // UUID of the target player.
        Player actor = Bukkit.getPlayer(actorID);
        if (actor == null) return;

        if (targetID.equals(releashed.getDominant(actorID))) { // If the target was one's dominant.
            if (actor.getInventory().getItemInMainHand().getType() != Material.LEAD) return;
            String message = releashed.getConfig().getString("dominantLeashingMessage");
            if (message == null) return;
            actor.sendMessage(message);
            return;
        }

        List<Pair> targetSubPairs = releashed.getSubPairs(targetID);
        ItemStack domItem = actor.getInventory().getItemInMainHand();

        if (targetSubPairs.isEmpty()) { // If the target player is not a sub to anyone.
            if (domItem.getType() != Material.LEAD) return;

            domItem.setAmount(domItem.getAmount() - 1);
            actor.getInventory().setItemInMainHand(domItem);
            releashed.pair(actorID, targetID);
            return;
        }

        Pair actorToTarget = releashed.getPair(actorID, targetID);

        if (actorToTarget == null) {
            if (domItem.getType() != Material.LEAD) return;

            String message = releashed.getConfig().getString("alreadyLeashedMessage");
            if (message != null) actor.sendMessage(message);
            return;
        }

        actor.getInventory().addItem(new ItemStack(Material.LEAD));
        releashed.unpair(actorToTarget);
    }
}