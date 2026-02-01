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
    private float leashLength = 5.0f; // Maybe will be configurable later.
    private Vector velocity;

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

    /// ### Pair.getSubmissivePairs(Player sub)
    /// Returns an ArrayList of all pairs where the specified player is submissive.
    public static ArrayList<Pair> getSubmissivePairs(Player sub) {
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


        if (!attached) { // New physics!
            double distance = submissive.getLocation().distance(dominant.getLocation());
            if (distance > leashLength) {
                double d0 = (dominant.getLocation().getX() - submissive.getLocation().getX()) / distance;
                double d1 = (dominant.getLocation().getY() - submissive.getLocation().getY()) / distance;
                double d2 = (dominant.getLocation().getZ() - submissive.getLocation().getZ()) / distance;
                velocity.setX(Math.copySign(d0 * d0 * 0.4, d0)); // Now mutating a single Vector object instead of allocating a new one every physics tick.
                velocity.setY(Math.copySign(d1 * d1 * 0.4, d1));
                velocity.setZ(Math.copySign(d2 * d2 * 0.4, d2));
                submissive.setVelocity(submissive.getVelocity().add(velocity));
            }
        }else{
            double distance = submissive.getLocation().distance(anchor);
            if (distance > leashLength) {
                double d0 = (anchor.getX() - submissive.getLocation().getX()) / distance;
                double d1 = (anchor.getY() - submissive.getLocation().getY()) / distance;
                double d2 = (anchor.getZ() - submissive.getLocation().getZ()) / distance;
                velocity.setX(Math.copySign(d0 * d0 * 0.4, d0)); // Now mutating a single Vector object instead of allocating a new one every physics tick.
                velocity.setY(Math.copySign(d1 * d1 * 0.4, d1));
                velocity.setZ(Math.copySign(d2 * d2 * 0.4, d2));
                submissive.setVelocity(submissive.getVelocity().add(velocity));
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


        leashMount.teleport(submissive.getLocation().add(0.0, 1.1, 0.0)); // NOT allocating an object now.
    }
}
