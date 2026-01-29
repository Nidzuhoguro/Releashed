package me.nidzuhoguro.releashed;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class Pair {
    private static final Releashed releashed = Releashed.getPlugin(Releashed.class);
    public final Player dominant;
    public final Player submissive;
    public LivingEntity leashMount;
    private boolean attached;
    private Location anchor;
    private Entity knot;
    private Block fence;

    public Pair(Player dominant, Player submissive) {
        this.dominant = dominant;
        this.submissive = submissive;

        Location location = submissive.getLocation();
        location.add(0.0, 1.1, 0.0);
        leashMount = (LivingEntity) submissive.getWorld().spawnEntity(location, EntityType.BAT);

        leashMount.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1));
        leashMount.setAI(false);
        leashMount.setInvisible(true);
        leashMount.setInvulnerable(true);
        leashMount.setCollidable(false);
        leashMount.setSilent(true);
        leashMount.setLeashHolder(dominant);
    }

    public static ArrayList<Pair> getSubmissivePairs(Player dom) {
        ArrayList<Pair> pairs = new ArrayList<>();
        for (Pair pair : releashed.pairs) {
            if (pair.dominant.equals(dom)) pairs.add(pair);
        }
        return pairs;
    }

    public static ArrayList<Pair> getDominantPairs(Player sub) {
        ArrayList<Pair> pairs = new ArrayList<>();
        for (Pair pair : releashed.pairs) {
            if (pair.submissive.equals(sub)) pairs.add(pair);
        }
        return pairs;
    }

    public static ArrayList<Pair> getAllPairs(Player player) {
        ArrayList<Pair> pairs = new ArrayList<>();
        for (Pair pair : releashed.pairs) {
            if (pair.dominant.equals(player) || pair.submissive.equals(player)) pairs.add(pair);
        }
        return pairs;
    }

    public boolean isDominant(Player player) {
        return dominant.equals(player);
    }

    public void attachToBlock(Location blockLocation, Entity knot, Block fence) {
        anchor = blockLocation;
        this.fence = fence;
        leashMount.setLeashHolder(knot);
        this.knot = knot;
        attached = true;
    }

    public void detachFromBlock() {
        leashMount.setLeashHolder(dominant);
        attached = false;
        fence = null;
    }

    public boolean unleash(boolean queued) {
        if (leashMount != null) {
            leashMount.remove();
            leashMount = null;
        }

        if (knot != null) {
            knot.remove();
            knot = null;
        }
        if (!queued) releashed.pairs.remove(this); // The queue thing is my lazy and horrible solution to the ConcurrentModificationExceptions.
        return true;
    }

    public boolean isAttached() {
        return attached;
    }

    public Block getFence() {
        return fence;
    }

    public void update() {
        if (submissive.getWorld() != dominant.getWorld()) {
            unleash(false);
        }

        if (!attached) {
            double subDistance = submissive.getLocation().distance(dominant.getLocation());

            if (subDistance > 5) {
                Vector direction = dominant.getLocation().toVector().subtract(submissive.getLocation().toVector()).normalize().multiply(0.5);
                submissive.setVelocity(direction);
            }
        }else{
            double subDistance = submissive.getLocation().distance(anchor);

            if (subDistance > 5) {
                Vector direction = anchor.toVector().subtract(submissive.getLocation().toVector()).normalize().multiply(0.5);
                submissive.setVelocity(direction);
            }
        }

        if (leashMount == null || leashMount.isDead() || !leashMount.isValid()) {
            if (leashMount != null) leashMount.remove();

            Location location = submissive.getLocation();
            location.add(0.0, 0.8, 0.0);
            leashMount = (LivingEntity) submissive.getWorld().spawnEntity(location, EntityType.BAT);

            leashMount.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1));
            leashMount.setAI(false);
            leashMount.setInvisible(true);
            leashMount.setInvulnerable(true);
            leashMount.setCollidable(false);
            leashMount.setSilent(true);
            leashMount.setLeashHolder(dominant);
            leashMount.setGravity(false);
            return;
        }

        Location location = submissive.getLocation();
        location.add(0.0, 1.1, 0.0);
        leashMount.teleport(location);
    }
}
