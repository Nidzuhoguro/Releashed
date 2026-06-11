package me.nidzuhoguro.releashed;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.UUID;

public class Pair {

    public final UUID dominantID;
    public final UUID submissiveID;
    public LivingEntity leashMount;
    private Vector3f anchor;
    private Entity knot;
    private Block fence;
    private float leashLength = 5.0f;
    private final Vector velocity = new Vector();
    private boolean valid = true;

    public Pair(UUID dominant, UUID submissive) {
        this.dominantID = dominant;
        this.submissiveID = submissive;

        ensureMountExistence();
    }

    public void ensureMountExistence() {
        if (leashMount == null || leashMount.isDead() || !leashMount.isValid()) {
            if (leashMount != null) leashMount.remove();

            Player submissive = Bukkit.getPlayer(submissiveID);
            Player dominant = Bukkit.getPlayer(dominantID);

            if (dominant == null || submissive == null) return;
            if (isAttached()) {
                if (knot == null) return;
                if (!knot.isValid()) return;
            }


            Location location = submissive.getLocation();
            location.add(0.0, 0.8, 0.0);
            leashMount = (LivingEntity) submissive.getWorld().spawnEntity(location, EntityType.BAT);

            leashMount.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1));
            leashMount.setAI(false);
            leashMount.setInvisible(true);
            leashMount.setInvulnerable(true);
            leashMount.setCollidable(false);
            leashMount.setSilent(true);
            leashMount.setLeashHolder(isAttached() ? knot : dominant);
            leashMount.setGravity(false);
        }
    }

    public void attachToBlock(Location blockLocation, Entity knot, Block fence) {
        if (anchor != null) return;
        anchor = blockLocation.toVector().toVector3f();
        this.fence = fence;
        this.knot = knot;
        leashMount.setLeashHolder(knot);
    }

    public void detachFromBlock() {
        if (anchor == null) return;
        Player dominant = Bukkit.getPlayer(dominantID);
        if (dominant == null) return;
        leashMount.setLeashHolder(dominant);
        anchor = null;
        fence = null;
    }

    public void unleash() {
        if (leashMount != null) leashMount.remove();
        if (knot != null) knot.remove();
    }

    private void computePhysics(double distance, Vector3f anchor, Vector3f target) {
        double d0 = (anchor.x() - target.x()) / distance;
        double d1 = (anchor.y() - target.y()) / distance;
        double d2 = (anchor.z() - target.z()) / distance;
        velocity.setX(Math.copySign(d0 * d0 * 0.4, d0));
        velocity.setY(Math.copySign(d1 * d1 * 0.4, d1));
        velocity.setZ(Math.copySign(d2 * d2 * 0.4, d2));
    }

    public void update() {
        Player submissive = Bukkit.getPlayer(submissiveID);
        Player dominant = Bukkit.getPlayer(dominantID);
        boolean attached = isAttached();

        if (submissive == null) return;

        if (!attached && dominant == null) {
            invalidate();
            return;
        }

        ensureMountExistence();

        leashMount.teleport(submissive.getLocation().add(0.0, 1.1, 0.0));

        Vector3f domLocation = attached ? null : dominant.getLocation().toVector().toVector3f();
        Vector3f subLocation = submissive.getLocation().toVector().toVector3f();

        double distance = anchor == null ? subLocation.distance(domLocation) : subLocation.distance(anchor);

        if (distance > leashLength) return;

        computePhysics(distance, attached ? anchor : domLocation, subLocation);
        submissive.setVelocity(submissive.getVelocity().add(velocity));
    }

    public boolean isDominant(UUID player) {
        return dominantID.equals(player);
    }

    public boolean isAttached() {
        return anchor != null;
    }

    public void invalidate() {
        valid = false;
    }

    public boolean isValid() {
        if (!valid) return false;
        if (isAttached() && knot == null || !knot.isValid()) return false;
        Player dominant = Bukkit.getPlayer(dominantID);
        if (!isAttached()) {
            if (dominant == null) return false;
            if (!dominant.isValid()) return false;
        }
        return Bukkit.getPlayer(submissiveID) != null;
    }

    public Block getFence() {
        return fence;
    }
}
