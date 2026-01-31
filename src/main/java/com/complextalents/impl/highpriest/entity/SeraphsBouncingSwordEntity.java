package com.complextalents.impl.highpriest.entity;

import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.SpawnSeraphSwordFXPacket;
import com.complextalents.util.AllyHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Seraph's Bouncing Sword Entity - A spectral sword that bounces between targets.
 * <p>
 * The sword hits enemies for damage and heals/shields allies.
 * It prefers enemies but cannot hit the same ally twice in a row.
 */
public class SeraphsBouncingSwordEntity extends Projectile {

    private static final int LIFETIME_TICKS = 600; // 30 seconds max lifetime
    private static final int HIT_COOLDOWN_TICKS = 10; // 0.5 seconds (10 ticks) between hits
    private static final int NO_HOMING_AFTER_HIT_TICKS = 10; // 0.5 seconds of no homing after hit
    private static final float HOMING_SPEED_BASE = 0.6f;
    private static final float HOMING_SPEED_MAX = 1.6f;
    private static final float TURN_RATE_BASE = 0.6f;
    private static final float TURN_RATE_MIN = 0.2f;
    private static final double SEARCH_RADIUS = 5.0; // 10x10x10 box (inflate by 5)
    private static final double DAMAGE_FALLOFF = 0.85; // 15% reduction per bounce
    private static final float TURN_ADJUST_THRESHOLD = 10f; // Degrees - start slowing down
    private static final float EXTREME_TURN_THRESHOLD = 45f; // Degrees - maximum extreme turn

    // NBT keys
    private static final String NBT_BOUNCE_COUNT = "BounceCount";
    private static final String NBT_MAX_BOUNCES = "MaxBounces";
    private static final String NBT_BASE_DAMAGE = "BaseDamage";
    private static final String NBT_HEAL_AMOUNT = "HealAmount";
    private static final String NBT_LAST_HIT_WAS_ALLY = "LastHitWasAlly";
    private static final String NBT_LAST_HIT_ALLY_UUID = "LastHitAllyUUID";
    private static final String NBT_LAST_HIT_ENTITY_UUID = "LastHitEntityUUID";
    private static final String NBT_SEARCH_RADIUS = "SearchRadius";

    // State
    private int bounceCount = 0;
    private int maxBounces = 3;
    private float baseDamage = 8.0f;
    private float healAmount = 4.0f;
    private int lastHitTick = -HIT_COOLDOWN_TICKS; // Initialize to allow immediate first hit
    private int hitDisableHomingTick = -NO_HOMING_AFTER_HIT_TICKS; // Track when homing was disabled
    private double searchRadius = SEARCH_RADIUS;
    private boolean lastHitWasAlly = false;
    private UUID lastHitAllyUUID = null;
    private UUID lastHitEntityUUID = null;  // Track last hit entity to prioritize new targets

    // Target tracking
    private LivingEntity target;

    // Visual rotation
    public float prevYawRender;
    public float yawRender;
    public float prevPitchRender;
    public float pitchRender;
    public float prevRollRender;
    public float rollRender;

    public SeraphsBouncingSwordEntity(EntityType<? extends SeraphsBouncingSwordEntity> entityType, Level level) {
        super(entityType, level);
    }

    public SeraphsBouncingSwordEntity(Level level, LivingEntity owner) {
        super(HighPriestEntities.SERAPHS_BOUNCING_SWORD.get(), level);
    }

    /**
     * Configure the sword's bouncing behavior.
     */
    public void configureBouncing(int maxBounces, float baseDamage, float healAmount, double searchRadius) {
        this.maxBounces = maxBounces;
        this.baseDamage = baseDamage;
        this.healAmount = healAmount;
        this.searchRadius = searchRadius;
    }

    /**
     * Set the target for homing.
     */
    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.5f;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        // Larger hitbox for more generous hit detection
        return EntityDimensions.scalable(1.5f, 1.5f);
    }

    @Override
    public boolean isInWall() {
        return false; // Don't get stuck in walls
    }

    @Override
    public void tick() {
        super.tick();

        // Guard against NaN/Infinity in position and velocity
        Vec3 pos = this.position();
        Vec3 vel = getDeltaMovement();
        if (!Double.isFinite(pos.x) || !Double.isFinite(pos.y) || !Double.isFinite(pos.z) ||
            !Double.isFinite(vel.x) || !Double.isFinite(vel.y) || !Double.isFinite(vel.z)) {
            // Invalid state - discard to prevent crashes
            this.discard();
            return;
        }

        // Update homing on both sides for proper rendering
        updateHoming();

        // Lifetime check (server only)
        if (!level().isClientSide && this.tickCount >= LIFETIME_TICKS) {
            this.discard();
            return;
        }

        // COLLISION CHECK (before move)
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
        }

        // MOVE ENTITY - this is what Projectile doesn't do automatically
        vel = getDeltaMovement();
        move(MoverType.SELF, vel);

        updateFacingFromVelocity();

        // Guard rotation values against NaN/Infinity
        if (!Float.isFinite(yawRender)) yawRender = 0;
        if (!Float.isFinite(pitchRender)) pitchRender = 0;
        if (!Float.isFinite(rollRender)) rollRender = 0;
        if (!Float.isFinite(prevYawRender)) prevYawRender = 0;
        if (!Float.isFinite(prevPitchRender)) prevPitchRender = 0;
        if (!Float.isFinite(prevRollRender)) prevRollRender = 0;

        // Flight trail FX - send packet every tick (server side only)
        if (!level().isClientSide) {
            PacketHandler.sendToNearby(
                new SpawnSeraphSwordFXPacket(this.position(), vel, 0), // 0 = flight trail
                (ServerLevel) level(),
                this.position()
            );
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // Always render this entity regardless of distance
        return true;
    }

    @Override
    public void shoot(double x, double y, double z, float speed, float inaccuracy) {
        // Projectile doesn't normalize or apply velocity automatically - we must do it manually
        Vec3 dir = new Vec3(x, y, z)
                .normalize()
                .add(
                        random.nextGaussian() * 0.0075 * inaccuracy,
                        random.nextGaussian() * 0.0075 * inaccuracy,
                        random.nextGaussian() * 0.0075 * inaccuracy
                )
                .scale(speed);

        setDeltaMovement(dir);

        hasImpulse = true;
        initFacingFromVelocity();
    }

    private void initFacingFromVelocity() {
        Vec3 vel = getDeltaMovement();
        if (vel.lengthSqr() < 1.0E-6) return;

        // Guard against NaN in velocity
        if (!Double.isFinite(vel.x) || !Double.isFinite(vel.y) || !Double.isFinite(vel.z)) {
            yawRender = prevYawRender = 0;
            pitchRender = prevPitchRender = 0;
            rollRender = prevRollRender = 0;
            return;
        }

        double horiz = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (horiz < 1.0E-6) horiz = 1.0E-6; // Prevent divide-by-zero

        float yaw = (float)(Mth.atan2(vel.x, vel.z) * (180F / Math.PI));
        float pitch = (float)(Mth.atan2(vel.y, horiz) * (180F / Math.PI));

        // Clamp to safe ranges
        yaw = Mth.wrapDegrees(yaw);
        pitch = Mth.clamp(-pitch, -90f, 90f);

        yawRender = prevYawRender = yaw;
        pitchRender = prevPitchRender = pitch;
        rollRender = prevRollRender = 0f;
    }

    /**
     * Update homing behavior - smooth velocity adjustment toward target.
     * Speed increases as the sword gets closer to its target.
     */
    private void updateHoming() {
        // Disable homing for a short period after hitting
        if (this.tickCount - hitDisableHomingTick < NO_HOMING_AFTER_HIT_TICKS) {
            return; // No homing - continue in current direction
        }

        if (target == null || !target.isAlive()) {
            // Try to find a new target if current is lost
            if (bounceCount > 0) {
                target = findNextTarget();
            }
            if (target == null) {
                return; // Continue in last direction
            }
        }

        Vec3 currentVel = getDeltaMovement();

        // FIX: Prevent processing if velocity is effectively zero (causes NaN on normalize)
        if (currentVel.lengthSqr() < 1.0E-7) {
            return;
        }

        // FIX: Guard against NaN in velocity before any calculations
        if (!Double.isFinite(currentVel.x) || !Double.isFinite(currentVel.y) || !Double.isFinite(currentVel.z)) {
            return;
        }

        // Calculate direction to target center
        Vec3 toTarget = target.position()
                .add(0, target.getBbHeight() * 0.5, 0)
                .subtract(this.position());

        // FIX: Use lengthSqr for distance check and guard against NaN
        double distToTargetSqr = toTarget.lengthSqr();
        if (!Double.isFinite(distToTargetSqr) || distToTargetSqr > 64 * 64) {
            target = null;
            return;
        }

        Vec3 desiredDir = toTarget.normalize();
        double distToTarget = Math.sqrt(distToTargetSqr);

        // Speed increases exponentially as distance decreases
        // Base speed + exponential bonus based on proximity
        float proximityFactor = (float) (1.0 - Math.min(distToTarget / 16.0, 1.0)); // 0 at 16+ blocks, 1 at 0 blocks
        float exponentialBonus = (float) Math.pow(proximityFactor, 3); // Cubic curve for dramatic acceleration
        float idealSpeed = HOMING_SPEED_BASE + exponentialBonus * (HOMING_SPEED_MAX - HOMING_SPEED_BASE);

        // FIX: Safe Dot Product for ACos - clamp strictly to prevent NaN
        Vec3 currentVelNorm = currentVel.normalize();
        double dot = currentVelNorm.dot(desiredDir);
        dot = Mth.clamp(dot, -1.0, 1.0); // Strict clamp prevents NaN from acos

        float angleToTarget = (float) Math.toDegrees(Math.acos(dot));

        // Turn rate decreases as speed increases, but we slow down for sharp turns
        float turnRate = TURN_RATE_BASE * (1f - exponentialBonus * 0.7f); // Faster = slower turn rate
        turnRate = Math.max(turnRate, TURN_RATE_MIN);

        // Slow down for turns - starts at 10 degrees, max reduction at 45 degrees
        float speed = idealSpeed;
        if (angleToTarget > TURN_ADJUST_THRESHOLD) {
            // Gradually slow down as turn angle increases
            float turnFactor = 1f - ((angleToTarget - TURN_ADJUST_THRESHOLD) / (EXTREME_TURN_THRESHOLD - TURN_ADJUST_THRESHOLD));
            speed *= Math.max(turnFactor, 0.2f); // Min 20% speed during extreme turns
        }

        // Blend current velocity toward desired direction (smooth turn)
        Vec3 newVel = currentVelNorm
                .lerp(desiredDir, turnRate)
                .normalize()
                .scale(speed);

        // FIX: Guard final velocity
        if (Double.isFinite(newVel.x) && Double.isFinite(newVel.y) && Double.isFinite(newVel.z)) {
            setDeltaMovement(newVel);
        }
    }

    public void updateFacingFromVelocity() {
        Vec3 vel = getDeltaMovement();

        // 1. Snapshot previous state BEFORE any modifications
        prevYawRender = yawRender;
        prevPitchRender = pitchRender;
        prevRollRender = rollRender;

        // 2. Check for "Stop" or "NaN" conditions
        double velSqr = vel.lengthSqr();
        if (velSqr < 1.0E-5 || !Double.isFinite(vel.x) || !Double.isFinite(vel.y) || !Double.isFinite(vel.z)) {
            // FIX: Do NOT reset to 0. Return early to keep the last known good rotation.
            // This prevents the sword from snapping to looking at its feet when it hits something.

            // Decay roll slowly for visual polish
            rollRender = Mth.lerp(0.1f, rollRender, 0f);
            return;
        }

        double horiz = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (horiz < 1.0E-5) horiz = 1.0E-5; // Prevent divide-by-zero

        // 3. Calculate new rotations
        float newYaw = (float)(Mth.atan2(vel.x, vel.z) * (180F / Math.PI));
        float newPitch = (float)(Mth.atan2(vel.y, horiz) * (180F / Math.PI));

        // 4. Sanitize Inputs (Yaw is 360, Pitch is -90 to 90)
        this.yawRender = Mth.wrapDegrees(newYaw);
        this.pitchRender = Mth.clamp(-newPitch, -90f, 90f);

        // 5. Roll banking logic
        float yawDelta = Mth.wrapDegrees(this.yawRender - this.prevYawRender);
        float targetRoll = Mth.clamp(yawDelta * 2.5f, -45f, 45f);

        this.rollRender = Mth.lerp(0.2f, this.rollRender, targetRoll);

        // 6. FINAL SAFETY GATE: Prevent NaN corruption from spreading to prev values
        if (!Float.isFinite(yawRender)) yawRender = prevYawRender;
        if (!Float.isFinite(pitchRender)) pitchRender = prevPitchRender;
        if (!Float.isFinite(rollRender)) rollRender = prevRollRender;

        // Double check that prev is also finite (initialization safety)
        if (!Float.isFinite(prevYawRender)) prevYawRender = 0;
        if (!Float.isFinite(prevPitchRender)) prevPitchRender = 0;
        if (!Float.isFinite(prevRollRender)) prevRollRender = 0;

        // 7. Sync to Vanilla Data
        // This ensures that if your custom renderer fails, the hitbox/debug renderer
        // and server-side logic still know which way it's facing.
        this.setYRot(this.yawRender);
        this.setXRot(this.pitchRender);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // DON'T call super - it will discard the projectile
        // super.onHitEntity(result);

        // Check cooldown - prevent hitting again too soon
        if (this.tickCount - lastHitTick < HIT_COOLDOWN_TICKS) {
            return; // Still in cooldown, ignore this hit
        }

        Entity hitTarget = result.getEntity();
        Entity owner = this.getOwner();

        // Only affect living entities
        if (!(hitTarget instanceof LivingEntity livingTarget)) {
            return;
        }

        // Update last hit tick and disable homing temporarily
        lastHitTick = this.tickCount;
        hitDisableHomingTick = this.tickCount;

        // Track the last hit entity to prioritize new targets
        lastHitEntityUUID = livingTarget.getUUID();

        // Check if this is an ally
        boolean isAlly = owner instanceof Player player && AllyHelper.isAlly(player, hitTarget);

        if (isAlly) {
            applyAllyEffect(livingTarget);
        } else {
            applyEnemyEffect(livingTarget, owner);
        }

        // Entity hit FX - flash particle and hit sound
        if (!level().isClientSide) {
            PacketHandler.sendToNearby(
                new SpawnSeraphSwordFXPacket(livingTarget.position(), null, 2), // 2 = entity hit
                (ServerLevel) level(),
                livingTarget.position()
            );
        }

        bounceCount++;

        // Check if we should continue bouncing
        if (bounceCount >= maxBounces) {
            this.discard();
            return;
        }

        // Find next target
        LivingEntity nextTarget = findNextTarget();

        if (nextTarget != null) {
            // Update target and continue
            target = nextTarget;

            // FIX: Safe Separation Math - prevent NaN from normalize on zero vector
            Vec3 toMob = this.position().subtract(livingTarget.position());
            Vec3 awayFromHit;

            // Check if length is too small to avoid dividing by zero (NaN)
            if (toMob.lengthSqr() < 1.0E-7) {
                // If inside the mob, pick a random upward direction to escape
                awayFromHit = new Vec3(0, 1, 0).scale(0.5);
            } else {
                awayFromHit = toMob.normalize().scale(0.5);
            }

            Vec3 newPos = this.position().add(awayFromHit);

            // FIX: Guard against NaN position before setting
            if (Double.isFinite(newPos.x) && Double.isFinite(newPos.y) && Double.isFinite(newPos.z)) {
                this.setPos(newPos);
                this.yo = this.getY(); 
                this.xo = this.getX();
                this.zo = this.getZ();
                // Ensure bounding box is updated after manual position change
                this.setBoundingBox(this.makeBoundingBox());
            }
        } else {
            // No valid target found - clear target but don't discard yet
            // The sword will continue flying and search again in ~0.5 seconds when homing re-enables
            target = null;
            // Only discard if we've been flying for a while without finding anything
            if (this.tickCount > 60 && bounceCount == 0) {
                this.discard();
            }
        }
    }

    /**
     * Apply healing/shielding effect to an ally.
     */
    private void applyAllyEffect(LivingEntity ally) {
        ally.heal(healAmount);

        lastHitWasAlly = true;
        lastHitAllyUUID = ally.getUUID();

        TalentsMod.LOGGER.debug("Seraph's Edge healed {} for {} health",
                ally.getName().getString(), healAmount);
    }

    /**
     * Apply damage effect to an enemy.
     */
    private void applyEnemyEffect(LivingEntity enemy, Entity owner) {
        // Calculate damage with falloff
        float damage = (float) (baseDamage * Math.pow(DAMAGE_FALLOFF, bounceCount));

        enemy.hurt(
                level().damageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null),
                damage
        );

        // Knockback
        Vec3 knockback = enemy.position().subtract(this.position()).normalize().scale(0.3);
        enemy.setDeltaMovement(enemy.getDeltaMovement().add(knockback));
        enemy.hurtMarked = true;

        // Reset ally flag when hitting enemy
        lastHitWasAlly = false;

        TalentsMod.LOGGER.debug("Seraph's Edge hit {} for {} damage (bounce #{})",
                enemy.getName().getString(), String.format("%.2f", damage), bounceCount + 1);
    }

    /**
     * Find the next target for the sword to bounce to.
     * Priority: New enemies > Old enemies > New allies > Old allies (with restrictions)
     */
    private LivingEntity findNextTarget() {
        Entity owner = this.getOwner();
        if (!(owner instanceof Player playerOwner)) {
            return null;
        }

        // Get all living entities in search radius
        List<LivingEntity> nearbyEntities = level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(searchRadius),
                e -> e.isAlive() && e != owner
        );

        if (nearbyEntities.isEmpty()) {
            return null;
        }

        // Separate into allies and enemies, and track new vs old targets
        List<LivingEntity> newEnemies = new ArrayList<>();
        List<LivingEntity> oldEnemies = new ArrayList<>();
        List<LivingEntity> newAllies = new ArrayList<>();
        List<LivingEntity> oldAllies = new ArrayList<>();

        for (LivingEntity entity : nearbyEntities) {
            boolean isEnemy = AllyHelper.isEnemy(playerOwner, entity);
            boolean isNewTarget = !entity.getUUID().equals(lastHitEntityUUID);

            if (isEnemy) {
                if (isNewTarget) {
                    newEnemies.add(entity);
                } else {
                    oldEnemies.add(entity);
                }
            } else if (AllyHelper.isAlly(playerOwner, entity)) {
                // Check ally restriction: cannot hit same ally twice in a row
                if (canHitAlly(entity)) {
                    if (isNewTarget) {
                        newAllies.add(entity);
                    } else {
                        oldAllies.add(entity);
                    }
                }
            }
        }

        // Priority 1: New enemies (prefer fresh targets)
        if (!newEnemies.isEmpty()) {
            return newEnemies.stream()
                    .min(Comparator.comparingDouble(e -> e.position().distanceToSqr(this.position())))
                    .orElse(null);
        }

        // Priority 2: Old enemies (can hit same enemy again if no new enemies)
        if (!oldEnemies.isEmpty()) {
            return oldEnemies.stream()
                    .min(Comparator.comparingDouble(e -> e.position().distanceToSqr(this.position())))
                    .orElse(null);
        }

        // Priority 3: New allies (only if didn't just hit an ally)
        if (!newAllies.isEmpty() && !lastHitWasAlly) {
            return newAllies.stream()
                    .min(Comparator.comparingDouble(e -> e.position().distanceToSqr(this.position())))
                    .orElse(null);
        }

        // Priority 4: Old allies (different ally than last hit)
        if (!oldAllies.isEmpty() && lastHitWasAlly) {
            // Find allies that are different from the last hit ally
            List<LivingEntity> differentAllies = oldAllies.stream()
                    .filter(e -> !e.getUUID().equals(lastHitAllyUUID))
                    .toList();

            if (!differentAllies.isEmpty()) {
                return differentAllies.stream()
                        .min(Comparator.comparingDouble(e -> e.position().distanceToSqr(this.position())))
                        .orElse(null);
            }
        }

        // No valid target found
        return null;
    }

    /**
     * Check if we can hit a specific ally based on the ally restriction rule.
     */
    private boolean canHitAlly(LivingEntity ally) {
        if (!lastHitWasAlly) {
            return true; // Didn't just hit an ally, so any ally is valid
        }
        // Hit an ally last time - can only hit a different ally now
        return !ally.getUUID().equals(lastHitAllyUUID);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // Bounce off the block and find a new target
        Vec3 currentVel = getDeltaMovement();

        // FIX: Guard against NaN velocity before reflection
        if (!Double.isFinite(currentVel.x) || !Double.isFinite(currentVel.y) || !Double.isFinite(currentVel.z)) {
            // Reset to safe default velocity
            setDeltaMovement(new Vec3(0, 0.5, 0));
            return;
        }

        // Reflect velocity based on the hit face
        Vec3 reflectedVel = switch (result.getDirection()) {
            case NORTH, SOUTH -> new Vec3(currentVel.x, currentVel.y, -currentVel.z);
            case EAST, WEST -> new Vec3(-currentVel.x, currentVel.y, currentVel.z);
            case UP, DOWN -> new Vec3(currentVel.x, -currentVel.y, currentVel.z);
        };

        // FIX: Guard reflected velocity
        if (Double.isFinite(reflectedVel.x) && Double.isFinite(reflectedVel.y) && Double.isFinite(reflectedVel.z)) {
            setDeltaMovement(reflectedVel);
        }
        hasImpulse = true;

        // Push slightly away from the block to prevent getting stuck
        Vec3 normal = new Vec3(result.getDirection().getNormal().getX(), result.getDirection().getNormal().getY(), result.getDirection().getNormal().getZ());
        Vec3 awayFromBlock = result.getLocation().add(normal.scale(0.1));

        // FIX: Guard against NaN position before setting
        if (Double.isFinite(awayFromBlock.x) && Double.isFinite(awayFromBlock.y) && Double.isFinite(awayFromBlock.z)) {
            this.setPos(awayFromBlock);
            this.yo = this.getY(); 
            this.xo = this.getX();
            this.zo = this.getZ();
            // Ensure bounding box is updated after manual position change
            this.setBoundingBox(this.makeBoundingBox());
        }

        // Terrain collision FX - ember burst and clang sound
        if (!level().isClientSide) {
            PacketHandler.sendToNearby(
                new SpawnSeraphSwordFXPacket(result.getLocation(), null, 1), // 1 = terrain hit
                (ServerLevel) level(),
                result.getLocation()
            );
        }

        // Find a new target
        target = findNextTarget();

        // If no target found and we've bounced too many times without hitting anything, discard
        if (target == null && bounceCount == 0) {
            // Allow a few block bounces before giving up if no enemies hit yet
            if (this.tickCount > 100) {
                this.discard();
            }
        }
    }

    @Override
    public boolean isNoGravity() {
        return true; // Floating sword - no gravity
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(NBT_BOUNCE_COUNT, bounceCount);
        compound.putInt(NBT_MAX_BOUNCES, maxBounces);
        compound.putFloat(NBT_BASE_DAMAGE, baseDamage);
        compound.putFloat(NBT_HEAL_AMOUNT, healAmount);
        compound.putDouble(NBT_SEARCH_RADIUS, searchRadius);
        compound.putBoolean(NBT_LAST_HIT_WAS_ALLY, lastHitWasAlly);
        if (lastHitAllyUUID != null) {
            compound.putUUID(NBT_LAST_HIT_ALLY_UUID, lastHitAllyUUID);
        }
        if (lastHitEntityUUID != null) {
            compound.putUUID(NBT_LAST_HIT_ENTITY_UUID, lastHitEntityUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(NBT_BOUNCE_COUNT)) {
            bounceCount = compound.getInt(NBT_BOUNCE_COUNT);
        }
        if (compound.contains(NBT_MAX_BOUNCES)) {
            maxBounces = compound.getInt(NBT_MAX_BOUNCES);
        }
        if (compound.contains(NBT_BASE_DAMAGE)) {
            baseDamage = compound.getFloat(NBT_BASE_DAMAGE);
        }
        if (compound.contains(NBT_HEAL_AMOUNT)) {
            healAmount = compound.getFloat(NBT_HEAL_AMOUNT);
        }
        if (compound.contains(NBT_SEARCH_RADIUS)) {
            searchRadius = compound.getDouble(NBT_SEARCH_RADIUS);
        }
        if (compound.contains(NBT_LAST_HIT_WAS_ALLY)) {
            lastHitWasAlly = compound.getBoolean(NBT_LAST_HIT_WAS_ALLY);
        }
        if (compound.contains(NBT_LAST_HIT_ALLY_UUID)) {
            lastHitAllyUUID = compound.getUUID(NBT_LAST_HIT_ALLY_UUID);
        }
        if (compound.contains(NBT_LAST_HIT_ENTITY_UUID)) {
            lastHitEntityUUID = compound.getUUID(NBT_LAST_HIT_ENTITY_UUID);
        }
    }
}
