package com.complextalents.elemental.entity;

import com.complextalents.TalentsMod;
import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.ElementalReactionHandler;
import com.complextalents.elemental.ElementalStackManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.UUID;

/**
 * Bloom Core entity spawned by Bloom reactions
 * Can be triggered by Lightning (Hyperbloom) or Fire (Burgeon) for secondary reactions
 */
public class BloomCoreEntity extends Entity {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TalentsMod.MODID);

    public static final RegistryObject<EntityType<BloomCoreEntity>> BLOOM_CORE =
        ENTITY_TYPES.register("bloom_core", () -> EntityType.Builder.<BloomCoreEntity>of(
            BloomCoreEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .clientTrackingRange(10)
            .build("bloom_core"));

    private static final EntityDataAccessor<Integer> REMAINING_TICKS =
        SynchedEntityData.defineId(BloomCoreEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TRIGGERED =
        SynchedEntityData.defineId(BloomCoreEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID ownerUUID;
    private float baseDamage;
    private boolean hasTriggered = false;

    public BloomCoreEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public BloomCoreEntity(Level level, Vec3 position, Player owner, float damage) {
        this(BLOOM_CORE.get(), level);
        this.setPos(position);
        this.ownerUUID = owner.getUUID();
        this.baseDamage = damage;

        // Set duration from config
        int duration = ElementalReactionConfig.bloomCoreDuration.get();
        this.entityData.set(REMAINING_TICKS, duration);
        this.entityData.set(TRIGGERED, false);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(REMAINING_TICKS, 120); // 6 seconds default
        this.entityData.define(TRIGGERED, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.hasTriggered || this.entityData.get(TRIGGERED)) {
            this.discard();
            return;
        }

        int remainingTicks = this.entityData.get(REMAINING_TICKS);
        if (remainingTicks <= 0) {
            // Explode on natural expiration (small damage)
            explodeCore(ElementType.NATURE, 0.5f);
            return;
        }

        this.entityData.set(REMAINING_TICKS, remainingTicks - 1);

        // Check for elemental triggers
        if (!this.level().isClientSide && this.tickCount % 5 == 0) {
            checkForElementalTriggers();
        }

        // Spawn particles
        if (this.level().isClientSide) {
            spawnCoreParticles();
        }
    }

    /**
     * Check if any nearby entities have Lightning or Fire elements to trigger secondary reactions
     */
    private void checkForElementalTriggers() {
        float detectionRadius = 2.0f;
        AABB area = new AABB(
            this.position().subtract(detectionRadius, detectionRadius, detectionRadius),
            this.position().add(detectionRadius, detectionRadius, detectionRadius)
        );

        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity entity : entities) {
            // Check for elemental stacks on the entity
            ElementType element = ElementalStackManager.getHighestStack(entity.getUUID());

            if (element == ElementType.LIGHTNING) {
                // Trigger Hyperbloom
                triggerHyperbloom();
                return;
            } else if (element == ElementType.FIRE) {
                // Trigger Burgeon
                triggerBurgeon();
                return;
            }
        }
    }

    /**
     * Trigger Hyperbloom reaction (Lightning + Bloom Core)
     */
    private void triggerHyperbloom() {
        if (this.hasTriggered) return;
        this.hasTriggered = true;
        this.entityData.set(TRIGGERED, true);

        // Spawn tracking projectiles
        if (!this.level().isClientSide) {
            ServerPlayer owner = getOwner();
            if (owner != null) {
                int projectileCount = ElementalReactionConfig.hyperbloomProjectileCount.get();
                float targetRadius = ElementalReactionConfig.hyperbloomTargetRadius.get().floatValue();

                // Find targets for projectiles
                List<LivingEntity> targets = this.level().getEntitiesOfClass(
                    LivingEntity.class,
                    this.getBoundingBox().inflate(targetRadius),
                    entity -> entity != owner && entity.isAlive()
                );

                for (int i = 0; i < Math.min(projectileCount, targets.size()); i++) {
                    LivingEntity target = targets.get(i);
                    spawnTrackingProjectile(target, owner);
                }
            }
        }

        this.discard();
    }

    /**
     * Trigger Burgeon reaction (Fire + Bloom Core)
     */
    private void triggerBurgeon() {
        if (this.hasTriggered) return;
        this.hasTriggered = true;
        this.entityData.set(TRIGGERED, true);

        // Create large AoE explosion
        float explosionRadius = ElementalReactionConfig.burgeonAoeRadius.get().floatValue();
        explodeCore(ElementType.FIRE, 1.5f);

        // Spawn Smoldering Gloom zone
        if (!this.level().isClientSide) {
            ServerPlayer owner = getOwner();
            if (owner != null) {
                SmolderingGloomEntity gloom = new SmolderingGloomEntity(this.level(), this.position(), owner);
                this.level().addFreshEntity(gloom);
            }
        }

        this.discard();
    }

    /**
     * Spawn a tracking projectile for Hyperbloom
     */
    private void spawnTrackingProjectile(LivingEntity target, ServerPlayer owner) {
        HyperbloomProjectile projectile = new HyperbloomProjectile(this.level(), this.position(), owner, target);
        this.level().addFreshEntity(projectile);
    }

    /**
     * Explode the core dealing damage in an area
     */
    private void explodeCore(ElementType triggerElement, float damageMultiplier) {
        if (this.level().isClientSide) return;

        float radius = 3.0f;
        float damage = this.baseDamage * damageMultiplier;

        // Deal damage to nearby entities
        this.level().getEntitiesOfClass(LivingEntity.class,
            this.getBoundingBox().inflate(radius),
            entity -> entity != getOwner())
            .forEach(entity -> {
                DamageSource source = this.level().damageSources().magic();
                entity.hurt(source, damage);
            });

        // Spawn explosion particles
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                this.getX(), this.getY(), this.getZ(),
                1, 0, 0, 0, 0);
        }
    }

    private void spawnCoreParticles() {
        // Green nature particles
        this.level().addParticle(ParticleTypes.COMPOSTER,
            this.getX(), this.getY() + 0.25, this.getZ(),
            0.0, 0.02, 0.0);

        // Occasional leaves particle
        if (this.random.nextFloat() < 0.1f) {
            this.level().addParticle(ParticleTypes.SPORE_BLOSSOM_AIR,
                this.getX(), this.getY() + 0.25, this.getZ(),
                0.0, 0.0, 0.0);
        }
    }

    private ServerPlayer getOwner() {
        if (this.ownerUUID != null && !this.level().isClientSide) {
            Entity entity = ((net.minecraft.server.level.ServerLevel) this.level()).getEntity(this.ownerUUID);
            if (entity instanceof ServerPlayer player) {
                return player;
            }
        }
        return null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.hasUUID("OwnerUUID")) {
            this.ownerUUID = compound.getUUID("OwnerUUID");
        }
        this.baseDamage = compound.getFloat("BaseDamage");
        this.hasTriggered = compound.getBoolean("HasTriggered");
        this.entityData.set(REMAINING_TICKS, compound.getInt("RemainingTicks"));
        this.entityData.set(TRIGGERED, compound.getBoolean("Triggered"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if (this.ownerUUID != null) {
            compound.putUUID("OwnerUUID", this.ownerUUID);
        }
        compound.putFloat("BaseDamage", this.baseDamage);
        compound.putBoolean("HasTriggered", this.hasTriggered);
        compound.putInt("RemainingTicks", this.entityData.get(REMAINING_TICKS));
        compound.putBoolean("Triggered", this.entityData.get(TRIGGERED));
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false; // Immune to damage
    }
}