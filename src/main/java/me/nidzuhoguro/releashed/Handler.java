package me.nidzuhoguro.releashed;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
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

import java.util.List;
import java.util.UUID;

public class Handler implements Listener {

    private final Releashed releashed;

    public Handler(Releashed plugin) {
        releashed = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Pair pair = releashed.getSubPair(player.getUniqueId());

        List<Pair> domPairs = releashed.getDomPairs(player.getUniqueId());
        if (!domPairs.isEmpty()) {
            for (Pair domPair : domPairs) {
                if (domPair.isAttached()) continue;
                player.getInventory().addItem(new ItemStack(Material.LEAD));
                domPair.unleash();
            }
        }

        if (pair == null) return;

        pair.getSubmissivePlayer().damage(20.0);
        Player dominant = pair.getDominantPlayer();
        pair.unleash();
        if (dominant == null) return;
        dominant.getInventory().addItem(new ItemStack(Material.LEAD));
    }

    @EventHandler
    public void onPlayerDie(PlayerDeathEvent event) {
        Pair subPair = releashed.getSubPair(event.getEntity().getUniqueId());

        List<Pair> domPairs = releashed.getDomPairs(event.getEntity().getUniqueId());

        for (Pair pair : domPairs) {
            if (pair.isAttached()) continue;
            pair.getDominantPlayer().getInventory().addItem(new ItemStack(Material.LEAD));
            pair.unleash();
        }

        if (subPair == null) return;

        subPair.getDominantPlayer().getInventory().addItem(new ItemStack(Material.LEAD));
        subPair.unleash();
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Pair subPair = releashed.getSubPair(uuid);

        List<Pair> domPairs = releashed.getDomPairs(uuid);

        for (Pair pair : domPairs) {
            if (pair.isAttached()) continue;
            pair.getDominantPlayer().getInventory().addItem(new ItemStack(Material.LEAD));
            pair.unleash();
        }

        if (subPair == null) return;

        subPair.getDominantPlayer().getInventory().addItem(new ItemStack(Material.LEAD));
        subPair.unleash();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUnleash(EntityUnleashEvent event) {
        for (Pair pair : releashed.getPairCollection()) {
            if (pair.getMount() == null || !pair.getMount().equals(event.getEntity())) continue;
            if (event.getReason() == EntityUnleashEvent.UnleashReason.PLAYER_UNLEASH) pair.getDominantPlayer().getInventory().addItem(new ItemStack(Material.LEAD));
            pair.unleash();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        Pair subPair = releashed.getSubPair(player.getUniqueId());

        if (subPair == null) return;

        switch (event.getEntity().getType()) {
            case LEASH_KNOT -> {
                if (player.equals(subPair.getSubmissivePlayer())) event.setCancelled(true);
            }
            case PLAYER -> {
                if (event.getEntity().equals(subPair.getDominantPlayer())) player.damage(event.getDamage());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void attachToFence(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getClickedBlock() == null) return;
        if (!event.getClickedBlock().getType().name().endsWith("_FENCE")) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND || releashed.getDomPairs(event.getPlayer().getUniqueId()).isEmpty()) return;

        Player player = event.getPlayer();
        Location location = event.getClickedBlock().getLocation().add(0.0, 0.5, 0.0);
        Entity knot = player.getWorld().spawnEntity(location, EntityType.LEASH_KNOT);

        List<Pair> pairs = releashed.getDomPairs(player.getUniqueId());

        for (Pair pair : pairs) {
            pair.attachToBlock(location, knot, event.getClickedBlock());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHangingDamage(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player player)) return; // Fixed potential ClassCastException.
        if (!event.getEntity().getType().equals(EntityType.LEASH_KNOT)) return;
        Pair subPair = releashed.getSubPair(player.getUniqueId());
        if (subPair == null) return;
        if (event.getEntity() != subPair.getMount()) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Pair subPair = releashed.getSubPair(event.getPlayer().getUniqueId());
        if (subPair == null) return;
        if (event.getBlock().equals(subPair.getFence())) { // If a sub tries to break the fence
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();

        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getRightClicked().getType() == EntityType.LEASH_KNOT) {
            Pair subPair = releashed.getSubPair(player.getUniqueId());
            if (subPair != null) { // Subs cannot untie the leash knots
                if (subPair.getSubmissivePlayer().equals(player)) event.setCancelled(true);
                return;
            }

            List<Pair> domPairs = releashed.getDomPairs(player.getUniqueId());

            for (Pair pair : domPairs) {
                if (event.getRightClicked() == pair.getMount()) {
                    pair.detachFromBlock();
                }
            }
            return;
        }

        if (!(event.getRightClicked() instanceof Player target)) return;

        ItemStack domItem = player.getInventory().getItemInMainHand();

        Pair subPair = releashed.getSubPair(target.getUniqueId());

        if (subPair != null) { // The targeted player already is a sub
            if (subPair.getDominantPlayer().equals(player)) { // if the player performing action is the dominant of the target player, the pair is broken.
                player.getInventory().addItem(new ItemStack(Material.LEAD));
                subPair.unleash();
                return;
            }
        }

        if (domItem.getType() != Material.LEAD) return;

        if (subPair != null) {
            player.sendMessage("This player is already leashed");
            return;
        }

        Pair playerSubPair = releashed.getSubPair(player.getUniqueId()); // to check if the player performing the action is a sub.

        if (playerSubPair != null && playerSubPair.getDominantPlayer().equals(target)) { // the player is, in fact, a sub and targets their dominant trying to leash them.
            player.sendMessage("You cannot leash your dominant~");
            return;
        }

        // if nothing of the above got triggered, that means there is no pair player -> target yet, create one.

        new Pair(player, target, releashed);
        domItem.setAmount(domItem.getAmount() - 1);
        player.getInventory().setItemInMainHand(domItem);
    }
}