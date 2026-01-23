package com.complextalents.elemental.entity;

import com.complextalents.TalentsMod;
import com.complextalents.config.ElementalReactionConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.UUID;

/**
 * Steam cloud entity created by Vaporize reactions
 * Creates an area that applies miss chance to ranged attacks
 */
public class SteamCloudEntity extends Entity {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TalentsMod.MODID);

    public static final RegistryObject<EntityType<SteamCloudEntity>> STEAM_CLOUD =
        ENTITY_TYPES.register("steam_cloud", () -> EntityType.Builder.<SteamCloudEntity>of(
            SteamCloudEntity::new, MobCategory.MISC)
            .sized(3.0F, 2.0F)
            .clientTrackingRange(10)
            .build("steam_cloud"));

    private static final EntityDataAccessor<Integer> REMAINING_TICKS =
        SynchedEntityData.defineId(SteamCloudEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> CLOUD_RADIUS =
        SynchedEntityData.defineId(SteamCloudEntity.class, EntityDataSerializers.FLOAT);

    private UUID casterUUID;
    private float missChance = 0.5f; // 50% miss chance by default

    public SteamCloudEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SteamCloudEntity(Level level, Vec3 position, Player caster) {
        this(STEAM_CLOUD.get(), level);
        this.setPos(position);
        this.casterUUID = caster.getUUID();

        // Set duration from config
        int duration = ElementalReactionConfig.vaporizeSteamCloudDuration.get();
        this.entityData.set(REMAINING_TICKS, duration);

        // Set radius from config
        float radius = ElementalReactionConfig.vaporizeSteamCloudRadius.get().floatValue();
        this.entityData.set(CLOUD_RADIUS, radius);

        // Set miss chance from config
        this.missChance = ElementalReactionConfig.vaporizeRangedMissChance.get().floatValue();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(REMAINING_TICKS, 200); // 10 seconds default
        this.entityData.define(CLOUD_RADIUS, 3.0F);
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

        // Apply effects to entities within the cloud
        if (!this.level().isClientSide && this.tickCount % 10 == 0) {
            applyCloudEffects();
        }

        // Spawn particles
        if (this.level().isClientSide) {
            spawnCloudParticles();
        }
    }

    private void applyCloudEffects() {
        float radius = this.entityData.get(CLOUD_RADIUS);
        AABB area = new AABB(
            this.position().subtract(radius, radius, radius),
            this.position().add(radius, radius, radius)
        );

        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity entity : entities) {
            // Apply brief blindness to simulate vision obstruction
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));

            // Mark entity as being in steam cloud for miss chance calculation
            entity.getPersistentData().putFloat("steam_cloud_miss_chance", missChance);
            entity.getPersistentData().putLong("steam_cloud_tick", this.level().getGameTime() + 20);
        }
    }

    private void spawnCloudParticles() {
        float radius = this.entityData.get(CLOUD_RADIUS);

        for (int i = 0; i < 5; i++) {
            double offsetX = (this.random.nextDouble() - 0.5) * radius * 2;
            double offsetY = this.random.nextDouble() * radius;
            double offsetZ = (this.random.nextDouble() - 0.5) * radius * 2;

            this.level().addParticle(ParticleTypes.CLOUD,
                this.getX() + offsetX,
                this.getY() + offsetY,
                this.getZ() + offsetZ,
                0.0, 0.01, 0.0);
        }
    }

    /**
     * Static helper to check if a projectile should miss due to steam cloud
     */
    public static boolean shouldMissFromSteamCloud(Projectile projectile, LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        if (data.contains("steam_cloud_miss_chance") && data.contains("steam_cloud_tick")) {
            long cloudTick = data.getLong("steam_cloud_tick");
            if (target.level().getGameTime() <= cloudTick) {
                float missChance = data.getFloat("steam_cloud_miss_chance");
                return target.level().random.nextFloat() < missChance;
            }
        }
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.hasUUID("CasterUUID")) {
            this.casterUUID = compound.getUUID("CasterUUID");
        }
        this.missChance = compound.getFloat("MissChance");
        this.entityData.set(REMAINING_TICKS, compound.getInt("RemainingTicks"));
        this.entityData.set(CLOUD_RADIUS, compound.getFloat("CloudRadius"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if (this.casterUUID != null) {
            compound.putUUID("CasterUUID", this.casterUUID);
        }
        compound.putFloat("MissChance", this.missChance);
        compound.putInt("RemainingTicks", this.entityData.get(REMAINING_TICKS));
        compound.putFloat("CloudRadius", this.entityData.get(CLOUD_RADIUS));
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }
}