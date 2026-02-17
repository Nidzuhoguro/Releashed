package me.nidzuhoguro.releashed;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.UUID;

public class Pair {
    private final Releashed releashed;
    private final UUID domUUID;
    private final UUID subUUID;
    private LivingEntity leashMount;
    private boolean attached;
    private Location anchor;
    private Entity knot;
    private Block fence;
    private float leashLength = 5.0f; // Maybe will be configurable later.
    private final Vector velocity = new Vector();

    private ArmorStand domDebugTag;
    private ArmorStand subDebugTag;

    public Pair(Player dominant, Player submissive, Releashed plugin) {
        releashed = plugin;
        this.domUUID = dominant.getUniqueId();
        this.subUUID = submissive.getUniqueId();

        leashMount = makeLeashTracker(dominant);

        domDebugTag = (ArmorStand) dominant.getWorld().spawnEntity(dominant.getLocation().add(0.0, 0.5, 0.0), EntityType.ARMOR_STAND);
        domDebugTag.setMarker(true);
        domDebugTag.setGravity(false);
        domDebugTag.setInvisible(true);
        domDebugTag.setBasePlate(false);
        domDebugTag.setCustomName("dominant");
        domDebugTag.setCustomNameVisible(true);

        dominant.addPassenger(domDebugTag);

        subDebugTag = (ArmorStand) submissive.getWorld().spawnEntity(submissive.getLocation().add(0.0, 0.5, 0.0), EntityType.ARMOR_STAND);
        subDebugTag.setMarker(true);
        subDebugTag.setGravity(false);
        subDebugTag.setInvisible(true);
        subDebugTag.setBasePlate(false);
        subDebugTag.setCustomName("submissive");
        subDebugTag.setCustomNameVisible(true);

        submissive.addPassenger(subDebugTag);

        releashed.addPair(this);
    }

    @SuppressWarnings("unused")
    public Pair setLeashLength(float length) {
        this.leashLength = length;
        return this;
    }

    private LivingEntity makeLeashTracker(Entity leashHolder) {
        Player player = Bukkit.getPlayer(subUUID);
        if (player == null) return null;
        if (!player.isOnline()) return null;
        if (leashHolder == null) return null;
        LivingEntity entity = (LivingEntity) player.getWorld().spawnEntity(player.getLocation().add(0.0, 1.1, 0.0), EntityType.BAT);

        entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1));
        entity.setAI(false);
        entity.setInvisible(true);
        entity.setInvulnerable(true);
        entity.setCollidable(false);
        entity.setSilent(true);
        entity.setLeashHolder(leashHolder);
        return entity;
    }

    public void attachToBlock(Location blockLocation, Entity knot, Block fence) {
        if (attached) return;
        this.anchor = blockLocation;
        this.fence = fence;
        this.knot = knot;
        leashMount.setLeashHolder(knot);
        attached = true;
    }

    public void detachFromBlock() {
        if (!attached) return;
        Player dominant = Bukkit.getPlayer(domUUID);
        if (dominant == null) return;
        leashMount.setLeashHolder(dominant);
        attached = false;
        fence = null;
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

        domDebugTag.remove();
        subDebugTag.remove();
        domDebugTag = null;
        subDebugTag = null;

        releashed.removePair(subUUID);
    }

    public Block getFence() {
        return fence;
    }

    public Player getDominantPlayer() {
        return Bukkit.getPlayer(domUUID);
    }

    public Player getSubmissivePlayer() {
        Player sub = Bukkit.getPlayer(subUUID);
        if (sub == null) {
            unleash();
        }
        return sub;
    }

    public UUID getDomUUID() {
        return domUUID;
    }

    public UUID getSubUUID() {
        return subUUID;
    }

    public Entity getMount() {
        return leashMount;
    }

    public boolean isAttached() {
        return attached;
    }

    public void tick() {

        Player submissive = Bukkit.getPlayer(subUUID);
        if (submissive == null) return;

        Player dominant = Bukkit.getPlayer(domUUID); // May be null!

        tickTracking(dominant, submissive);

        if (attached) {
            double distance = submissive.getLocation().distance(anchor);

            if (distance <= leashLength) return;

            double d0 = (anchor.getX() - submissive.getLocation().getX()) / distance;
            double d1 = (anchor.getY() - submissive.getLocation().getY()) / distance;
            double d2 = (anchor.getZ() - submissive.getLocation().getZ()) / distance;
            velocity.setX(Math.copySign(d0 * d0 * 0.4, d0)); // Now mutating a single Vector object instead of allocating a new one every physics tick.
            velocity.setY(Math.copySign(d1 * d1 * 0.4, d1));
            velocity.setZ(Math.copySign(d2 * d2 * 0.4, d2));
            submissive.setVelocity(submissive.getVelocity().add(velocity));
        }

        if (dominant == null) return;

        if (submissive.getWorld() != dominant.getWorld()) {
            unleash();
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
        }
    }

    private void tickTracking(Player dominant, Player submissive) {

        if (dominant == null && !attached) unleash();

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
            leashMount.setLeashHolder(attached ? knot : dominant);
            leashMount.setGravity(false);
            return;
        }

        leashMount.teleport(submissive.getLocation().add(0.0, 1.1, 0.0));
    }
}
