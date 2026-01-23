package com.complextalents.elemental.entity;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalStackManager;
import com.complextalents.elemental.attributes.MasteryAttributes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

import java.util.UUID;

/**
 * Orbiting projectile spawned by Elemental Ward (Rank 3B - Reprisal)
 * Orbits around the player and damages enemies on contact
 */
public class ElementalOrbEntity extends Projectile {

    private UUID ownerUUID;
    private ElementType elementType;
    private float damage = 10.0f;
    private int ticksAlive = 0;
    private static final int MAX_LIFETIME = 600; // 30 seconds

    // Orbital motion parameters
    private float orbitRadius = 2.5f;
    private float orbitSpeed = 0.1f; // Radians per tick
    private float currentAngle = 0f;
    private float verticalOffset = 1.5f; // Height above player's feet

    public ElementalOrbEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    /**
     * Constructor for creating orb from Ward talent
     */
    public ElementalOrbEntity(Level level, ServerPlayer owner, ElementType element, float baseDamage, float startAngle) {
        this(ModEntities.ELEMENTAL_ORB.get(), level);
        this.setOwner(owner);
        this.ownerUUID = owner.getUUID();
        this.elementType = element;
        this.currentAngle = startAngle;

        // Calculate damage with mastery scaling: baseDamage * (1 + 0.5 * (Mastery - 1))
        this.damage = calculateDamageWithMastery(owner, element, baseDamage);

        // Set initial position
        updateOrbitalPosition(owner);
    }

    @Override
    protected void defineSynchedData() {
        // No additional synched data needed
    }

    @Override
    public void tick() {
        super.tick();

        ticksAlive++;
        if (ticksAlive > MAX_LIFETIME) {
            this.discard();
            return;
        }

        // Get owner reference
        Entity ownerEntity = this.getOwner();
        if (ownerEntity == null && ownerUUID != null && !this.level().isClientSide) {
            ownerEntity = ((net.minecraft.server.level.ServerLevel) this.level()).getEntity(ownerUUID);
            if (ownerEntity != null) {
                this.setOwner(ownerEntity);
            }
        }

        // Discard if owner is not a player or is dead
        if (!(ownerEntity instanceof ServerPlayer player) || !player.isAlive()) {
            this.discard();
            return;
        }

        // Update orbital angle
        currentAngle += orbitSpeed;
        if (currentAngle > Math.PI * 2) {
            currentAngle -= Math.PI * 2;
        }

        // Update position based on orbital motion
        updateOrbitalPosition(player);

        // Spawn particles
        if (!this.level().isClientSide) {
            // Server-side particle spawning for visibility
            if (ticksAlive % 2 == 0 && elementType != null) { // Every other tick
                spawnOrbParticles((ServerLevel) this.level());
            }
        }

        // Check for entity collisions
        if (!this.level().isClientSide) {
            checkEntityCollisions(player);
        }
    }

    /**
     * Update the orb's position to orbit around the player
     */
    private void updateOrbitalPosition(ServerPlayer player) {
        // Calculate orbital position using sin/cos
        double x = player.getX() + Math.cos(currentAngle) * orbitRadius;
        double y = player.getY() + verticalOffset;
        double z = player.getZ() + Math.sin(currentAngle) * orbitRadius;

        this.setPos(x, y, z);
    }

    /**
     * Check for collisions with nearby entities
     */
    private void checkEntityCollisions(ServerPlayer owner) {
        // Get entities within collision range
        for (Entity entity : this.level().getEntities(this, this.getBoundingBox().inflate(0.5))) {
            if (entity instanceof LivingEntity living && entity != owner && living.isAlive()) {
                onHitEntity(new EntityHitResult(living));
                break; // Only hit one entity per tick
            }
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

            // Apply element stack
            if (this.getOwner() instanceof ServerPlayer player) {
                ElementalStackManager.applyElementStack(living, elementType, player, damage);
            }

            // Spawn impact particles
            if (this.level() instanceof ServerLevel serverLevel && elementType != null) {
                spawnImpactParticles(serverLevel, living.position());
            }

            // Discard orb after hitting
            this.discard();
        }
    }

    /**
     * Calculate damage with mastery scaling
     * Formula: baseDamage * (1 + 0.5 * (Mastery - 1))
     */
    private float calculateDamageWithMastery(ServerPlayer caster, ElementType element, float baseDamage) {
        // Get general elemental mastery
        double generalMasteryAttr = caster.getAttributeValue(MasteryAttributes.ELEMENTAL_MASTERY.get());
        float generalMastery = (float)(generalMasteryAttr + 1.0); // Base mastery is 1

        // Get element-specific mastery
        RegistryObject<net.minecraft.world.entity.ai.attributes.Attribute> elementAttr = getElementMasteryAttribute(element);
        double specificMasteryAttr = elementAttr != null ? caster.getAttributeValue(elementAttr.get()) : 0.0;
        float specificMastery = (float)(specificMasteryAttr + 1.0);

        // Use the higher mastery value
        float effectiveMastery = Math.max(generalMastery, specificMastery);

        // Apply formula: baseDamage * (1 + 0.5 * (Mastery - 1))
        return baseDamage * (1f + 0.5f * (effectiveMastery - 1f));
    }

    /**
     * Get the attribute registry object for element-specific mastery
     */
    private RegistryObject<net.minecraft.world.entity.ai.attributes.Attribute> getElementMasteryAttribute(ElementType element) {
        return switch (element) {
            case FIRE -> MasteryAttributes.FIRE_MASTERY;
            case AQUA -> MasteryAttributes.AQUA_MASTERY;
            case LIGHTNING -> MasteryAttributes.LIGHTNING_MASTERY;
            case ICE -> MasteryAttributes.ICE_MASTERY;
            case NATURE -> MasteryAttributes.NATURE_MASTERY;
            case ENDER -> MasteryAttributes.ENDER_MASTERY;
        };
    }

    /**
     * Spawn particle trail for the orb
     */
    private void spawnOrbParticles(ServerLevel level) {
        Vec3 pos = this.position();
        ParticleOptions particle = getParticleForElement(elementType);

        // Spawn orbital trail particles
        for (int i = 0; i < 2; i++) {
            level.sendParticles(particle,
                pos.x, pos.y, pos.z,
                1, 0, 0, 0, 0.02);
        }
    }

    /**
     * Spawn impact particles when hitting an entity
     */
    private void spawnImpactParticles(ServerLevel level, Vec3 pos) {
        ParticleOptions particle = getParticleForElement(elementType);

        // Burst of particles on impact
        for (int i = 0; i < 15; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetY = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

            double velocityX = (level.random.nextDouble() - 0.5) * 0.3;
            double velocityY = level.random.nextDouble() * 0.3;
            double velocityZ = (level.random.nextDouble() - 0.5) * 0.3;

            level.sendParticles(particle,
                pos.x + offsetX, pos.y + 0.5 + offsetY, pos.z + offsetZ,
                1, velocityX, velocityY, velocityZ, 0.05);
        }
    }

    /**
     * Get particle type for element (fallback to vanilla particles)
     */
    private ParticleOptions getParticleForElement(ElementType element) {
        return switch (element) {
            case FIRE -> ParticleTypes.FLAME;
            case AQUA -> ParticleTypes.DRIPPING_WATER;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case NATURE -> ParticleTypes.HAPPY_VILLAGER;
            case ENDER -> ParticleTypes.PORTAL;
        };
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        if (compound.hasUUID("OwnerUUID")) {
            this.ownerUUID = compound.getUUID("OwnerUUID");
        }
        if (compound.contains("ElementType")) {
            this.elementType = ElementType.valueOf(compound.getString("ElementType"));
        }
        this.damage = compound.getFloat("Damage");
        this.ticksAlive = compound.getInt("TicksAlive");
        this.currentAngle = compound.getFloat("CurrentAngle");
        this.orbitRadius = compound.getFloat("OrbitRadius");
        this.orbitSpeed = compound.getFloat("OrbitSpeed");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        if (this.ownerUUID != null) {
            compound.putUUID("OwnerUUID", this.ownerUUID);
        }
        if (this.elementType != null) {
            compound.putString("ElementType", this.elementType.name());
        }
        compound.putFloat("Damage", this.damage);
        compound.putInt("TicksAlive", this.ticksAlive);
        compound.putFloat("CurrentAngle", this.currentAngle);
        compound.putFloat("OrbitRadius", this.orbitRadius);
        compound.putFloat("OrbitSpeed", this.orbitSpeed);
    }
}
