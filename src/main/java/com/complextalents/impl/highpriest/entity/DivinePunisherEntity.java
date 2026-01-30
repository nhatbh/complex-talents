package com.complextalents.impl.highpriest.entity;

import com.complextalents.TalentsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Divine Punisher Entity - A flying sword projectile with homing capability.
 * Flies toward its target and damages the first entity it hits.
 */
public class DivinePunisherEntity extends ThrowableProjectile {

    private static final int LIFETIME_TICKS = 1000; // 5 seconds
    private static final float BASE_DAMAGE = 5.0f;
    private static final float HOMING_SPEED = 0.1f;
    private static final float TURN_RATE = 0.15f;

    private static final String NBT_MASTERY = "Mastery";
    private static final String NBT_TARGET_UUID = "TargetUUID";

    private float mastery = 1.0f;
    private LivingEntity target;
    private float speed = HOMING_SPEED;
    private float turnRate = TURN_RATE;
    // Facing
    public float prevYawRender;
    public float yawRender;

    public float prevPitchRender;
    public float pitchRender;

    public float prevRollRender;
    public float rollRender;


    public DivinePunisherEntity(EntityType<? extends DivinePunisherEntity> entityType, Level level) {
        super(entityType, level);
    }

    public DivinePunisherEntity(Level level, LivingEntity owner) {
        super(HighPriestEntities.DIVINE_PUNISHER.get(), owner, level);
    }

    public void setOwner(ServerPlayer owner, float mastery) {
        this.setOwner(owner);
        this.mastery = mastery;
    }

    /**
     * Set the target for homing.
     */
    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    /**
     * Set the homing speed.
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * Set the turn rate for homing.
     */
    public void setTurnRate(float turnRate) {
        this.turnRate = turnRate;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();

        // Update homing on server side
        if (!level().isClientSide) {
            updateHoming();
        }

        // Lifetime check
        if (!level().isClientSide && this.tickCount >= LIFETIME_TICKS) {
            this.discard();
        }

        updateFacingFromVelocity();
    }
    @Override
    public void shoot(double x, double y, double z, float speed, float inaccuracy) {
        super.shoot(x, y, z, speed, inaccuracy);

        // After velocity is applied, initialize facing
        initFacingFromVelocity();
    }

    private void initFacingFromVelocity() {
        Vec3 vel = getDeltaMovement();
        if (vel.lengthSqr() < 1.0E-6) return;

        double horiz = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

        float yaw = (float)(Mth.atan2(vel.x, vel.z) * (180F / Math.PI));
        float pitch = (float)(Mth.atan2(vel.y, horiz) * (180F / Math.PI));

        yawRender = prevYawRender = yaw;
        pitchRender = prevPitchRender = -pitch; // negative is important
        rollRender = prevRollRender = 0f;
    }


    /**
     * Update homing behavior - smooth velocity adjustment toward target.
     */
    private void updateHoming() {
        if (target == null || !target.isAlive()) {
            return;
        }

        Vec3 currentVel = getDeltaMovement();

        // Calculate direction to target center
        Vec3 toTarget = target.position()
                .add(0, target.getBbHeight() * 0.5, 0)
                .subtract(this.position());

        // Check if target is too far
        double distToTarget = toTarget.length();
        if (distToTarget > 64) {
            target = null;
            return;
        }

        Vec3 desiredDir = toTarget.normalize();

        // Blend current velocity toward desired direction (smooth turn)
        Vec3 newVel = currentVel.normalize()
                .lerp(desiredDir, turnRate)
                .normalize()
                .scale(speed);

        setDeltaMovement(newVel);
    }

    public void updateFacingFromVelocity() {
        Vec3 vel = getDeltaMovement();

        prevYawRender = yawRender;
        prevPitchRender = pitchRender;
        prevRollRender = rollRender;

        if (vel.lengthSqr() < 1.0E-6) {
            rollRender *= 0.9f;
            return;
        }

        double horiz = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

        yawRender = (float)(Mth.atan2(vel.x, vel.z) * (180F / Math.PI));
        pitchRender = -(float)(Mth.atan2(vel.y, horiz) * (180F / Math.PI));

        float yawDelta = Mth.wrapDegrees(yawRender - prevYawRender);
        float targetRoll = Mth.clamp(yawDelta * 2.5f, -45f, 45f);

        rollRender = Mth.lerp(0.25f, rollRender, targetRoll);
    }


    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        Entity hitTarget = result.getEntity();
        Entity owner = this.getOwner();

        float damage = BASE_DAMAGE * (1.0f + mastery * 0.5f);

        hitTarget.hurt(
                level().damageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null),
                damage
        );

        // Knockback
        Vec3 knockback = hitTarget.position().subtract(this.position()).normalize().scale(0.5);
        hitTarget.setDeltaMovement(hitTarget.getDeltaMovement().add(knockback));
        if (hitTarget instanceof LivingEntity living) {
            living.hurtMarked = true;
        }

        TalentsMod.LOGGER.debug("Divine Punisher hit {} for {} damage",
                hitTarget.getName().getString(), damage);

        this.discard(); // Remove sword after hit
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        this.discard();
    }

    @Override
    protected float getGravity() {
        return 0.0F; // Floating sword, no gravity
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat(NBT_MASTERY, mastery);
        if (target != null && target.isAlive()) {
            compound.putUUID(NBT_TARGET_UUID, target.getUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(NBT_MASTERY)) {
            mastery = compound.getFloat(NBT_MASTERY);
        }
        if (compound.contains(NBT_TARGET_UUID)) {
            // Note: Target reference is not restored on load to avoid stale references
            // The sword will continue in its last direction without homing
        }
    }
}
