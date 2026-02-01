package com.complextalents.impl.highpriest.entity;

import com.complextalents.impl.highpriest.util.ProjectileDamageResolver;
import com.complextalents.util.TeamHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.UUID;

/**
 * Sanctuary Barrier Entity - A spherical barrier created by Tier 3 Divine Grace.
 */
public class SanctuaryBarrierEntity extends Entity {

    private static final int LIFETIME_TICKS = 20 * 60; // 60 seconds

    // NBT keys
    private static final String NBT_OWNER = "SanctuaryOwner";
    private static final String NBT_RADIUS = "SanctuaryRadius";
    private static final String NBT_HP = "SanctuaryHP";
    private static final String NBT_MAX_HP = "SanctuaryMaxHp";
    private static final String NBT_LIFETIME = "SanctuaryLifetime";

    // Synced data accessors
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(
            SanctuaryBarrierEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HP = SynchedEntityData.defineId(
            SanctuaryBarrierEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_MAX_HP = SynchedEntityData.defineId(
            SanctuaryBarrierEntity.class, EntityDataSerializers.FLOAT);

    // State (only non-synced fields here)
    private UUID owner;
    private int lifetime = LIFETIME_TICKS;

    public SanctuaryBarrierEntity(EntityType<SanctuaryBarrierEntity> type, Level level) {
        super(type, level);
    }

    public SanctuaryBarrierEntity(Level level, Vec3 position, float radius, float hp, UUID owner) {
        this(HighPriestEntities.SANCTUARY_BARRIER.get(), level);
        this.owner = owner;
        setPos(position.x, position.y, position.z);
        // Note: entity data is set via setters after construction
        // because defineSynchedData() hasn't been called yet in the constructor chain
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_RADIUS, 4f);
        this.entityData.define(DATA_HP, 100f);
        this.entityData.define(DATA_MAX_HP, 100f);
    }

    @Override
    public void tick() {
        super.tick();

        // Always update position to stay in sync
        updateBoundingBox();

        if (level().isClientSide) {
            return;
        }

        lifetime--;
        if (lifetime <= 0 || getHp() <= 0) {
            discard();
            return;
        }

        handleEntityCollisions();
        handleProjectiles();
    }

    @Override
    protected void setRot(float p_21017_, float p_21018_) {
        // No rotation needed
    }

    /**
     * Update bounding box based on position and radius.
     */
    private void updateBoundingBox() {
        float boxRadius = getRadius();
        Vec3 pos = position();
        setBoundingBox(new AABB(
                pos.x - boxRadius, pos.y - boxRadius, pos.z - boxRadius,
                pos.x + boxRadius, pos.y + boxRadius, pos.z + boxRadius
        ));
    }

    /**
     * Handle enemy collisions - push any enemy inside the radius.
     */
    private void handleEntityCollisions() {
        List<LivingEntity> entities = level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox(),
                e -> !e.getUUID().equals(owner) && !isAlly(e)
        );

        for (LivingEntity entity : entities) {
            // Check if entity is inside or touching the sphere
            double distSq = entity.position().distanceToSqr(position());
            double rSq = getRadius() * getRadius();

            // Push if inside the sphere or at the edge
            if (distSq <= rSq) {
                repelEnemy(entity);
            }
        }
    }

    /**
     * Handle projectiles - destroy any projectile inside the sphere.
     */
    private void handleProjectiles() {
        List<Projectile> projectiles = level().getEntitiesOfClass(
                Projectile.class,
                getBoundingBox(),
                p -> true
        );

        for (Projectile projectile : projectiles) {
            // Check if projectile is inside the sphere
            double distSq = projectile.position().distanceToSqr(position());
            double rSq = getRadius() * getRadius();

            if (distSq <= rSq) {
                // Resolve damage
                float damage = ProjectileDamageResolver.get(projectile);

                // Damage the barrier
                damage(damage);

                // Destroy the projectile
                projectile.discard();
            }
        }
    }

    /**
     * Check if a projectile is within the sphere.
     */
    public boolean isProjectileInSphere(Entity p) {
        return p.position().distanceToSqr(position()) <= getRadius() * getRadius();
    }

    /**
     * Check if an entity is an ally of the caster.
     */
    private boolean isAlly(Entity e) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        ServerPlayer caster = serverLevel.getServer().getPlayerList().getPlayer(owner);
        if (caster == null || !(e instanceof LivingEntity living)) {
            return false;
        }
        return TeamHelper.isAlly(caster, living);
    }

    /**
     * Repel an enemy away from the barrier and damage the barrier.
     */
    private void repelEnemy(LivingEntity enemy) {
        Vec3 dir = enemy.position().subtract(position()).normalize();

        // Strong knockback
        enemy.push(dir.x * 3.0, 0.6, dir.z * 3.0);
        enemy.hurtMarked = true;

        // Damage barrier (50% of enemy max health)
        float damage = enemy.getMaxHealth() * 0.5f;
        damage(damage);
    }

    /**
     * Damage the barrier.
     */
    public void damage(float amount) {
        float newHp = getHp() - amount;
        this.entityData.set(DATA_HP, newHp);
        if (newHp <= 0) {
            discard();
        }
    }

    /**
     * Get current barrier HP.
     */
    public float getHp() {
        return this.entityData.get(DATA_HP);
    }

    /**
     * Get max barrier HP.
     */
    public float getMaxHp() {
        return this.entityData.get(DATA_MAX_HP);
    }

    /**
     * Get barrier radius.
     */
    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    /**
     * Get caster UUID.
     */
    public UUID getOwner() {
        return owner;
    }

    /**
     * Set the barrier radius (synced to client).
     */
    public void setRadius(float radius) {
        this.entityData.set(DATA_RADIUS, radius);
    }

    /**
     * Set the barrier HP (synced to client).
     */
    public void setHp(float hp) {
        this.entityData.set(DATA_HP, hp);
    }

    /**
     * Set the max barrier HP (synced to client).
     */
    public void setMaxHp(float maxHp) {
        this.entityData.set(DATA_MAX_HP, maxHp);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        compound.putUUID(NBT_OWNER, owner);
        compound.putFloat(NBT_RADIUS, getRadius());
        compound.putFloat(NBT_HP, getHp());
        compound.putFloat(NBT_MAX_HP, getMaxHp());
        compound.putInt(NBT_LIFETIME, lifetime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        owner = compound.getUUID(NBT_OWNER);
        this.entityData.set(DATA_RADIUS, compound.getFloat(NBT_RADIUS));
        this.entityData.set(DATA_HP, compound.getFloat(NBT_HP));
        this.entityData.set(DATA_MAX_HP, compound.getFloat(NBT_MAX_HP));
        lifetime = compound.getInt(NBT_LIFETIME);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
