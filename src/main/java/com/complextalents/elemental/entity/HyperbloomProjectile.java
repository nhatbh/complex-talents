package com.complextalents.elemental.entity;

import com.complextalents.TalentsMod;
import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.elemental.effects.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.UUID;

/**
 * Tracking projectile spawned by Hyperbloom reaction
 * Homes in on targets and applies Vulnerable effect
 */
public class HyperbloomProjectile extends Projectile {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TalentsMod.MODID);

    public static final RegistryObject<EntityType<HyperbloomProjectile>> HYPERBLOOM_PROJECTILE =
        ENTITY_TYPES.register("hyperbloom_projectile", () -> EntityType.Builder.<HyperbloomProjectile>of(
            HyperbloomProjectile::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(8)
            .updateInterval(1)
            .build("hyperbloom_projectile"));

    private UUID ownerUUID;
    private UUID targetUUID;
    private LivingEntity target;
    private int ticksAlive = 0;
    private static final int MAX_LIFE = 100; // 5 seconds
    private float damage = 10.0f;

    public HyperbloomProjectile(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public HyperbloomProjectile(Level level, Vec3 position, ServerPlayer owner, LivingEntity target) {
        this(HYPERBLOOM_PROJECTILE.get(), level);
        this.setPos(position);
        this.setOwner(owner);
        this.ownerUUID = owner.getUUID();
        this.target = target;
        this.targetUUID = target.getUUID();

        // Calculate initial velocity towards target
        Vec3 direction = target.position().subtract(position).normalize();
        this.setDeltaMovement(direction.scale(0.5));
    }

    @Override
    protected void defineSynchedData() {
        // No additional synched data needed
    }

    @Override
    public void tick() {
        super.tick();

        ticksAlive++;
        if (ticksAlive > MAX_LIFE) {
            this.discard();
            return;
        }

        // Update target reference if needed
        if (target == null && targetUUID != null && !this.level().isClientSide) {
            Entity entity = ((net.minecraft.server.level.ServerLevel) this.level()).getEntity(targetUUID);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                target = living;
            }
        }

        // Home in on target
        if (target != null && target.isAlive()) {
            Vec3 toTarget = target.getEyePosition().subtract(this.position());
            double distance = toTarget.length();

            if (distance < 0.5) {
                // Hit the target
                onHitEntity(new EntityHitResult(target));
                return;
            }

            // Adjust velocity to track target
            Vec3 currentVel = this.getDeltaMovement();
            Vec3 desiredVel = toTarget.normalize().scale(0.8);

            // Smooth tracking
            double trackingStrength = 0.15;
            Vec3 newVel = currentVel.lerp(desiredVel, trackingStrength);
            this.setDeltaMovement(newVel);
        } else {
            // Target lost, continue in current direction
            if (this.getDeltaMovement().lengthSqr() < 0.01) {
                this.discard();
            }
        }

        // Update position
        this.setPos(this.position().add(this.getDeltaMovement()));

        // Spawn particles
        if (this.level().isClientSide) {
            spawnTrailParticles();
        }

        // Check for collisions
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide) return;

        Entity entity = result.getEntity();
        if (entity instanceof LivingEntity living && entity != this.getOwner()) {
            // Deal damage
            DamageSource source = this.damageSources().magic();
            if (this.getOwner() instanceof ServerPlayer player) {
                source = this.damageSources().playerAttack(player);
            }
            living.hurt(source, damage);

            // Apply Vulnerable effect
            int duration = ElementalReactionConfig.hyperbloomVulnerableDuration.get();
            living.addEffect(new MobEffectInstance(ModEffects.VULNERABLE.get(), duration, 0));

            // Spawn impact particles
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    living.getX(), living.getY() + living.getBbHeight() / 2, living.getZ(),
                    10, 0.2, 0.2, 0.2, 0.1);
            }

            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY) {
            this.onHitEntity((EntityHitResult) result);
        } else if (result.getType() == HitResult.Type.BLOCK) {
            // Explode on block hit
            if (!this.level().isClientSide) {
                if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        this.getX(), this.getY(), this.getZ(),
                        5, 0.1, 0.1, 0.1, 0.05);
                }
            }
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity != this.getOwner() && entity instanceof LivingEntity && entity.isAlive();
    }

    private void spawnTrailParticles() {
        this.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
            this.getX(), this.getY(), this.getZ(),
            0, 0, 0);

        if (this.random.nextFloat() < 0.3f) {
            this.level().addParticle(ParticleTypes.ENCHANT,
                this.getX(), this.getY(), this.getZ(),
                0, 0, 0);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.hasUUID("OwnerUUID")) {
            this.ownerUUID = compound.getUUID("OwnerUUID");
        }
        if (compound.hasUUID("TargetUUID")) {
            this.targetUUID = compound.getUUID("TargetUUID");
        }
        this.ticksAlive = compound.getInt("TicksAlive");
        this.damage = compound.getFloat("Damage");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if (this.ownerUUID != null) {
            compound.putUUID("OwnerUUID", this.ownerUUID);
        }
        if (this.targetUUID != null) {
            compound.putUUID("TargetUUID", this.targetUUID);
        }
        compound.putInt("TicksAlive", this.ticksAlive);
        compound.putFloat("Damage", this.damage);
    }
}