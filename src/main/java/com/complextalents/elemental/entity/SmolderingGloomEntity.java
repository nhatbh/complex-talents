package com.complextalents.elemental.entity;

import com.complextalents.TalentsMod;
import com.complextalents.config.ElementalReactionConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * Smoldering Gloom zone created by Burgeon reactions
 * Creates an area that deals damage over time and slows enemies
 */
public class SmolderingGloomEntity extends Entity {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TalentsMod.MODID);

    public static final RegistryObject<EntityType<SmolderingGloomEntity>> SMOLDERING_GLOOM =
        ENTITY_TYPES.register("smoldering_gloom", () -> EntityType.Builder.<SmolderingGloomEntity>of(
            SmolderingGloomEntity::new, MobCategory.MISC)
            .sized(4.0F, 0.5F)
            .clientTrackingRange(10)
            .build("smoldering_gloom"));

    private static final EntityDataAccessor<Integer> REMAINING_TICKS =
        SynchedEntityData.defineId(SmolderingGloomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ZONE_RADIUS =
        SynchedEntityData.defineId(SmolderingGloomEntity.class, EntityDataSerializers.FLOAT);

    private UUID casterUUID;
    private float damagePerTick = 2.0f;
    private float slowPercentage = 0.3f;

    public SmolderingGloomEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SmolderingGloomEntity(Level level, Vec3 position, Player caster) {
        this(SMOLDERING_GLOOM.get(), level);
        this.setPos(position);
        this.casterUUID = caster.getUUID();

        // Set duration from config
        int duration = ElementalReactionConfig.burgeonZoneDuration.get();
        this.entityData.set(REMAINING_TICKS, duration);

        // Set radius from config
        float radius = ElementalReactionConfig.burgeonAoeRadius.get().floatValue();
        this.entityData.set(ZONE_RADIUS, radius);

        // Set slow percentage from config
        this.slowPercentage = ElementalReactionConfig.burgeonSlowPercentage.get().floatValue();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(REMAINING_TICKS, 80); // 4 seconds default
        this.entityData.define(ZONE_RADIUS, 6.0F);
    }

    @Override
    public void tick() {
        super.tick();

        int remainingTicks = this.entityData.get(REMAINING_TICKS);

        if (remainingTicks <= 0) {
            this.discard();
            return;
        }

        this.entityData.set(REMAINING_TICKS, remainingTicks - 1);

        // Apply damage and effects every 10 ticks (0.5 seconds)
        if (!this.level().isClientSide && this.tickCount % 10 == 0) {
            applyZoneEffects();
        }

        // Spawn particles
        if (this.level().isClientSide) {
            spawnGloomParticles();
        }
    }

    private void applyZoneEffects() {
        float radius = this.entityData.get(ZONE_RADIUS);
        AABB area = new AABB(
            this.position().subtract(radius, 1, radius),
            this.position().add(radius, 2, radius)
        );

        ServerPlayer caster = getCaster();
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity entity : entities) {
            // Don't affect the caster
            if (entity == caster) continue;

            // Apply damage
            DamageSource source = this.level().damageSources().inFire();
            if (caster != null) {
                source = this.level().damageSources().playerAttack(caster);
            }
            entity.hurt(source, damagePerTick);

            // Apply slowness
            int slowLevel = (int)(slowPercentage * 10); // Convert percentage to potion level
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, slowLevel, false, false));

            // Set on fire briefly
            entity.setSecondsOnFire(1);
        }
    }

    private void spawnGloomParticles() {
        float radius = this.entityData.get(ZONE_RADIUS);

        // Spawn smoke particles around the perimeter
        for (int i = 0; i < 3; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double distance = this.random.nextDouble() * radius;
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;

            this.level().addParticle(ParticleTypes.LARGE_SMOKE,
                this.getX() + offsetX,
                this.getY() + 0.1,
                this.getZ() + offsetZ,
                0.0, 0.05, 0.0);
        }

        // Spawn flame particles
        if (this.random.nextFloat() < 0.3f) {
            double offsetX = (this.random.nextDouble() - 0.5) * radius * 2;
            double offsetZ = (this.random.nextDouble() - 0.5) * radius * 2;

            this.level().addParticle(ParticleTypes.FLAME,
                this.getX() + offsetX,
                this.getY() + 0.1,
                this.getZ() + offsetZ,
                0.0, 0.01, 0.0);
        }

        // Spawn embers
        if (this.random.nextFloat() < 0.1f) {
            double offsetX = (this.random.nextDouble() - 0.5) * radius * 2;
            double offsetZ = (this.random.nextDouble() - 0.5) * radius * 2;

            this.level().addParticle(ParticleTypes.LAVA,
                this.getX() + offsetX,
                this.getY() + 0.1,
                this.getZ() + offsetZ,
                0.0, 0.0, 0.0);
        }
    }

    private ServerPlayer getCaster() {
        if (this.casterUUID != null && !this.level().isClientSide) {
            Entity entity = ((net.minecraft.server.level.ServerLevel) this.level()).getEntity(this.casterUUID);
            if (entity instanceof ServerPlayer player) {
                return player;
            }
        }
        return null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.hasUUID("CasterUUID")) {
            this.casterUUID = compound.getUUID("CasterUUID");
        }
        this.damagePerTick = compound.getFloat("DamagePerTick");
        this.slowPercentage = compound.getFloat("SlowPercentage");
        this.entityData.set(REMAINING_TICKS, compound.getInt("RemainingTicks"));
        this.entityData.set(ZONE_RADIUS, compound.getFloat("ZoneRadius"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if (this.casterUUID != null) {
            compound.putUUID("CasterUUID", this.casterUUID);
        }
        compound.putFloat("DamagePerTick", this.damagePerTick);
        compound.putFloat("SlowPercentage", this.slowPercentage);
        compound.putInt("RemainingTicks", this.entityData.get(REMAINING_TICKS));
        compound.putFloat("ZoneRadius", this.entityData.get(ZONE_RADIUS));
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