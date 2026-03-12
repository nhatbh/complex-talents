package com.complextalents.impl.highpriest.entity;

import com.complextalents.impl.highpriest.data.SeraphSwordData;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.SpawnSeraphSwordFXPacket;
import com.complextalents.util.AllyHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Seraph's Edge Entity - A spectral sword that embeds itself in the ground.
 * <p>
 * Three casting modes:
 * 1. Spawn & Move: Creates sword at player position, flies to target, embeds
 * 2. Move from Embedded: Moves existing sword with enhanced effects (1.5x damage/shield)
 * 3. Pull: Target the sword to pull enemies toward it
 */
public class SeraphsEdgeEntity extends Projectile {

    // Configuration constants
    private static final double MOVE_SPEED = 0.8;
    private static final int PLUNGE_DURATION = 10;
    private static final double DESPAWN_RANGE = 48.0;
    private static final double PULL_RADIUS = 8.0;

    // Movement state machine
    private enum SwordState {
        MOVING,      // Flying to target
        PLUNGING,    // Landing animation
        EMBEDDED     // Stabbed in ground
    }

    // State
    private SwordState state = SwordState.EMBEDDED;
    private Vec3 targetPos = Vec3.ZERO;
    private boolean wasEmbedded = false;
    private int plungeTimer = 0;

    // Configuration
    private float baseDamage = 10.0f;
    private float shieldAmount = 5.0f;

    // Tracking
    private final Set<UUID> hitEntitiesThisMove = new HashSet<>();

    // Visual rotation (public for renderer)
    public float prevYawRender;
    public float yawRender;
    public float prevPitchRender;
    public float pitchRender;
    public float prevRollRender;
    public float rollRender;
    private float prePlungePitch;

    public SeraphsEdgeEntity(EntityType<? extends SeraphsEdgeEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SeraphsEdgeEntity(Level level, LivingEntity owner) {
        super(HighPriestEntities.SERAPHS_EDGE.get(), level);
        this.setOwner(owner);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(1.0f, 1.0f);
    }

    // === PUBLIC API ===

    /**
     * Configure damage and shield values for this sword.
     * Called by SeraphsEdgeSkill when spawning.
     */
    public void configure(float damage, float shield) {
        this.baseDamage = damage;
        this.shieldAmount = shield;
    }

    /**
     * Move the sword to a target position.
     * Called by SeraphsEdgeSkill when casting.
     */
    public void moveTo(Vec3 target) {
        this.targetPos = target;
        this.wasEmbedded = (this.state == SwordState.EMBEDDED);
        this.state = SwordState.MOVING;
        this.hitEntitiesThisMove.clear();
        this.plungeTimer = 0;

        // Set initial velocity and rotation
        Vec3 direction = targetPos.subtract(position()).normalize();
        Vec3 velocity = direction.scale(MOVE_SPEED);
        setDeltaMovement(velocity);

        initFacingFromVelocity();
    }

    /**
     * Pull enemies toward the sword.
     * Called by SeraphsEdgeSkill when targeting the sword itself.
     */
    public void pullEnemies() {
        if (level().isClientSide) return;

        Entity owner = getOwner();
        List<LivingEntity> enemies = level().getEntitiesOfClass(
            LivingEntity.class,
            getBoundingBox().inflate(PULL_RADIUS),
            e -> e.isAlive() && owner instanceof Player p && AllyHelper.isEnemy(p, e)
        );

        for (LivingEntity enemy : enemies) {
            // Pull toward sword
            Vec3 pullDir = position().subtract(enemy.position()).normalize().scale(0.5);
            enemy.setDeltaMovement(enemy.getDeltaMovement().add(pullDir));
            enemy.hurtMarked = true;

            // Damage and slow
            enemy.hurt(
                level().damageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null),
                baseDamage * 0.8f
            );
            enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }

        // Send pull FX packet
        PacketHandler.sendToNearby(
            new SpawnSeraphSwordFXPacket(position(), null, 4), // 4 = pull effect
            (ServerLevel) level(),
            position()
        );
    }

    /**
     * Check if sword is currently embedded in ground.
     */
    public boolean isEmbedded() {
        return state == SwordState.EMBEDDED;
    }

    // === TICK LOGIC ===

    @Override
    public void tick() {
        super.tick();

        // Client-side: only update visuals
        if (level().isClientSide) {
            updateClientVisuals();
            return;
        }

        // Server-side logic
        checkOwnerDistance();

        switch (state) {
            case MOVING -> tickMoving();
            case PLUNGING -> tickPlunging();
            case EMBEDDED -> tickEmbedded();
        }

        sendParticleFX();
    }

    private void tickMoving() {
        Vec3 currentPos = position();
        Vec3 toTarget = targetPos.subtract(currentPos);
        double distSq = toTarget.lengthSqr();

        // Arrived at target?
        if (distSq < MOVE_SPEED * MOVE_SPEED) {
            setPos(targetPos);
            startPlunge();
            return;
        }

        // Move toward target
        Vec3 direction = toTarget.normalize();
        Vec3 velocity = direction.scale(MOVE_SPEED);
        setDeltaMovement(velocity);

        // Sweep path for entities
        Vec3 prevPos = currentPos;
        move(MoverType.SELF, velocity);
        Vec3 newPos = position();

        applyPathEffects(prevPos, newPos);
        updateFacingFromVelocity();
    }

    private void tickPlunging() {
        setDeltaMovement(Vec3.ZERO);
        plungeTimer++;

        // Ease-in-out interpolation
        float progress = Math.min(1.0f, (float) plungeTimer / PLUNGE_DURATION);
        float eased = (float) (0.5 - 0.5 * Math.cos(progress * Math.PI));

        // Rotate to point downward
        pitchRender = Mth.lerp(eased, prePlungePitch, 90.0f);
        rollRender = Mth.lerp(eased, rollRender, 0.0f);

        if (plungeTimer >= PLUNGE_DURATION) {
            state = SwordState.EMBEDDED;
            pitchRender = 90.0f;
            rollRender = 0.0f;
        }
    }

    private void tickEmbedded() {
        // Keep stable, pointing down
        setDeltaMovement(Vec3.ZERO);
        pitchRender = 90.0f;
        yawRender = 0.0f;
        rollRender = 0.0f;
    }

    private void startPlunge() {
        state = SwordState.PLUNGING;
        plungeTimer = 0;
        prePlungePitch = pitchRender;
        setDeltaMovement(Vec3.ZERO);
    }

    // === PATH EFFECTS ===

    private void applyPathEffects(Vec3 start, Vec3 end) {
        AABB sweepBox = new AABB(start, end).inflate(1.5);
        List<LivingEntity> entities = level().getEntitiesOfClass(
            LivingEntity.class,
            sweepBox,
            e -> e.isAlive() && !hitEntitiesThisMove.contains(e.getUUID())
        );

        Entity owner = getOwner();

        for (LivingEntity entity : entities) {
            if (entity == owner) continue;

            hitEntitiesThisMove.add(entity.getUUID());
            boolean isAlly = owner instanceof Player p && AllyHelper.isAlly(p, entity);

            if (isAlly) {
                applyAllyEffects(entity);
            } else {
                applyEnemyEffects(entity);
            }
        }
    }

    private void applyAllyEffects(LivingEntity ally) {
        // Shield (Absorption) - stronger if from embedded
        float shieldAmplifier = wasEmbedded ? shieldAmount * 1.5f : shieldAmount;
        ally.addEffect(new MobEffectInstance(
            MobEffects.ABSORPTION,
            200,
            (int)(shieldAmplifier / 2)
        ));

        // Speed - stronger if from embedded
        int speedAmplifier = wasEmbedded ? 2 : 1;
        ally.addEffect(new MobEffectInstance(
            MobEffects.MOVEMENT_SPEED,
            100,
            speedAmplifier
        ));
    }

    private void applyEnemyEffects(LivingEntity enemy) {
        // Damage - 1.5x if from embedded
        float damage = wasEmbedded ? baseDamage * 1.5f : baseDamage;
        Entity owner = getOwner();
        enemy.hurt(
            level().damageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null),
            damage
        );

        // Slow - stronger and longer if from embedded
        int slowAmplifier = wasEmbedded ? 2 : 1;
        int duration = wasEmbedded ? 60 : 40;
        enemy.addEffect(new MobEffectInstance(
            MobEffects.MOVEMENT_SLOWDOWN,
            duration,
            slowAmplifier
        ));
    }

    // === ROTATION ===

    private void initFacingFromVelocity() {
        Vec3 vel = getDeltaMovement();
        if (vel.lengthSqr() < 1.0E-6) return;

        double horiz = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        horiz = Math.max(horiz, 1.0E-6);

        float yaw = (float)(Mth.atan2(vel.x, vel.z) * (180.0 / Math.PI));
        float pitch = (float)(Mth.atan2(vel.y, horiz) * (180.0 / Math.PI));

        yawRender = prevYawRender = Mth.wrapDegrees(yaw);
        pitchRender = prevPitchRender = Mth.clamp(-pitch, -90f, 90f);
        rollRender = prevRollRender = 0f;

        setYRot(yawRender);
        setXRot(pitchRender);
    }

    private void updateFacingFromVelocity() {
        Vec3 vel = getDeltaMovement();
        if (vel.lengthSqr() < 1.0E-5) return;

        double horiz = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        horiz = Math.max(horiz, 1.0E-5);

        float newYaw = (float)(Mth.atan2(vel.x, vel.z) * (180.0 / Math.PI));
        float newPitch = (float)(Mth.atan2(vel.y, horiz) * (180.0 / Math.PI));

        yawRender = Mth.wrapDegrees(newYaw);
        pitchRender = Mth.clamp(-newPitch, -90f, 90f);

        // Banking effect during turns
        float yawDelta = Mth.wrapDegrees(yawRender - prevYawRender);
        float targetRoll = Mth.clamp(yawDelta * 2.5f, -45f, 45f);
        rollRender = Mth.lerp(0.2f, rollRender, targetRoll);

        setYRot(yawRender);
        setXRot(pitchRender);
    }

    // === CLIENT VISUALS ===

    private void updateClientVisuals() {
        prevYawRender = yawRender;
        prevPitchRender = pitchRender;
        prevRollRender = rollRender;

        switch (state) {
            case MOVING -> updateFacingFromVelocity();
            case PLUNGING -> {
                // Mirror server plunge animation
                plungeTimer++;
                float progress = Math.min(1.0f, (float) plungeTimer / PLUNGE_DURATION);
                float eased = (float) (0.5 - 0.5 * Math.cos(progress * Math.PI));

                pitchRender = Mth.lerp(eased, prePlungePitch, 90.0f);
                rollRender = Mth.lerp(eased, rollRender, 0.0f);

                if (plungeTimer >= PLUNGE_DURATION) {
                    state = SwordState.EMBEDDED;
                    pitchRender = 90.0f;
                    rollRender = 0.0f;
                }
            }
            case EMBEDDED -> {
                // Keep stable
            }
        }
    }

    // === DESPAWN ===

    private void checkOwnerDistance() {
        Entity owner = getOwner();
        if (!(owner instanceof Player player)) {
            discardAndClear();
            return;
        }

        if (player.distanceToSqr(this) > DESPAWN_RANGE * DESPAWN_RANGE) {
            discardAndClear();
        }
    }

    private void discardAndClear() {
        Entity owner = getOwner();
        if (owner instanceof Player p) {
            SeraphSwordData.clearActiveSword(p.getUUID());
        }
        discard();
    }

    // === PARTICLES ===

    private void sendParticleFX() {
        if (tickCount % 2 != 0) return;

        int fxType = switch (state) {
            case MOVING -> 0;    // Trail particles
            case PLUNGING -> 2;  // Plunge effect
            case EMBEDDED -> 3;  // Idle glow
        };

        PacketHandler.sendToNearby(
            new SpawnSeraphSwordFXPacket(position(), getDeltaMovement(), fxType),
            (ServerLevel) level(),
            position()
        );
    }

    // === NBT SERIALIZATION ===

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("State", state.name());
        compound.putBoolean("WasEmbedded", wasEmbedded);
        compound.putInt("PlungeTimer", plungeTimer);
        compound.putFloat("PrePlungePitch", prePlungePitch);
        compound.putDouble("TargetX", targetPos.x);
        compound.putDouble("TargetY", targetPos.y);
        compound.putDouble("TargetZ", targetPos.z);
        compound.putFloat("BaseDamage", baseDamage);
        compound.putFloat("ShieldAmount", shieldAmount);
        compound.putFloat("YawRender", yawRender);
        compound.putFloat("PitchRender", pitchRender);
        compound.putFloat("RollRender", rollRender);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        state = SwordState.valueOf(compound.getString("State"));
        wasEmbedded = compound.getBoolean("WasEmbedded");
        plungeTimer = compound.getInt("PlungeTimer");
        prePlungePitch = compound.getFloat("PrePlungePitch");
        targetPos = new Vec3(
            compound.getDouble("TargetX"),
            compound.getDouble("TargetY"),
            compound.getDouble("TargetZ")
        );
        baseDamage = compound.getFloat("BaseDamage");
        shieldAmount = compound.getFloat("ShieldAmount");
        yawRender = prevYawRender = compound.getFloat("YawRender");
        pitchRender = prevPitchRender = compound.getFloat("PitchRender");
        rollRender = prevRollRender = compound.getFloat("RollRender");
    }
}
