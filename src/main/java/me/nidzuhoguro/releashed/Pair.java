package me.nidzuhoguro.releashed;

import org.bukkit.Location;
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

    public void attachToBlock(Location blockLocation, Entity knot) {
        anchor = blockLocation;
        leashMount.setLeashHolder(knot);
        this.knot = knot;
        attached = true;
    }

    public void detachFromBlock() {
        leashMount.setLeashHolder(dominant);
        attached = false;
    }

    public void unleash() {
        if (leashMount != null) {
            leashMount.remove();
            leashMount = null;
        }

        if (knot != null) {
            knot.remove();
            knot = null;
        }
        releashed.pairs.remove(this);
    }

    public void update() {
        if (submissive.getWorld() != dominant.getWorld()) {
            unleash();
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
            location.add(0.0, 1.1, 0.0);
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
