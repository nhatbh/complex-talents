package com.complextalents.elemental.entity;

import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnNatureCoreExplosionPacket;
import com.complextalents.network.elemental.SpawnNatureCoreParticlePacket;
import com.complextalents.util.TeamHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Nature Core Entity - A tiny glowing slime block spawned by Bloom reaction
 * Explodes when hit by Fire or Lightning, or self-detonates after 4 seconds
 *
 * Features:
 * - Renders as a tiny slime block item
 * - No AI
 * - No item drops
 * - Glowing effect
 * - Cannot be targeted/attacked
 * - Accepts only 1 element stack (Fire or Lightning)
 * - Auto-detonates after 4 seconds (1 base damage)
 * - Explodes on element application (2 base damage * mastery)
 */
public class NatureCoreEntity extends Mob {

    private static final int LIFETIME_TICKS = 200; // 4 seconds (20 ticks * 4)
    private static final String NBT_TICK_COUNT = "NatureCoreTickCount";
    private static final String NBT_OWNER_USERNAME = "NatureCoreOwnerUsername";

    private String ownerUsername;

    public NatureCoreEntity(EntityType<? extends NatureCoreEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoAi(true);
    }

    /**
     * Sets the owner of this Nature Core entity.
     * The owner will receive credit for damage dealt by the explosion.
     *
     * @param ownerUsername The username of the owner (typically a player)
     */
    public void setOwner(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    /**
     * Gets the owner player if they're still in the level.
     *
     * @return The owner player, or null if not found/invalid
     */
    private ServerPlayer getOwner() {
        if (ownerUsername != null && level() instanceof ServerLevel serverLevel && serverLevel.getServer() != null) {
            return serverLevel.getServer().getPlayerList().getPlayerByName(ownerUsername);
        }
        return null;
    }

    @Override
    public boolean isCurrentlyGlowing() {
        return true; // Always glowing
    }

    @Override
    protected void registerGoals() {
        // NO GOALS - entity does not move or act
    }

    @Override
    public boolean isAttackable() {
        return false; // Cannot be attacked
    }

    @Override
    public boolean isPickable() {
        return false; // Cannot be interacted with
    }

    @Override
    public boolean canBeCollidedWith() {
        return false; // Cannot be collided with
    }

    @Override
    protected boolean shouldDropLoot() {
        return false; // No item drops
    }

    @Override
    public boolean isPushable() {
        return false; // Cannot be pushed
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false; // Should not despawn in peaceful
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // Should not despawn when far away
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        // Send particle effect packet to nearby clients every tick
        Vec3 pos = position();
        PacketHandler.sendToNearby(new SpawnNatureCoreParticlePacket(pos),
            (net.minecraft.server.level.ServerLevel) level(), pos);

        // Check for lifetime expiration using tickCount
        if (this.tickCount >= LIFETIME_TICKS) {
            killYourself();
        }
    }

    /**
     * YOU SHOULD KILL YOURSELF NOW!!! (In Minecraft)
     */
    private void killYourself() {
        // Remove the entity
        discard();
    }

    /**
     * Applies an element to the Nature Core
     * Can only accept one element (FIRE or LIGHTNING)
     *
     * @param mastery The elemental mastery for damage calculation
     * @return true if the element was applied and caused explosion
     */
    public boolean triggerExplosion(float mastery) {
        if (level().isClientSide) {
            return false;
        }
        explode(2.0f * mastery);
        return true;
    }

    /**
     * Makes the Nature Core explode
     * Deals magic damage and strong knockback to nearby entities in a 3-block radius
     * Sends custom particle effect packet instead of using default explosion
     *
     * @param baseDamage The base damage to deal
     */
    private void explode(float baseDamage) {
        if (level().isClientSide) {
            return;
        }

        Vec3 pos = position();
        double radius = 3.0; // 3 block radius
        double knockbackStrength = 1; // Strong knockback force
        double liftStrength = 0.4; // Strong knockback force
        ServerPlayer owner = getOwner();

        // Find nearby entities and deal magic damage with knockback
        level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
            getBoundingBox().inflate(radius))
            .forEach(entity -> {
                if (entity != this) {
                    // Skip allies of the owner - don't damage friendly targets
                    if (owner != null && TeamHelper.isAlly(owner, entity)) {
                        return;
                    }

                    // Apply damage with owner attribution if available
                    DamageSource damageSource;
                    if (owner != null) {
                        damageSource = level().damageSources().playerAttack(owner);
                    } else {
                        damageSource = level().damageSources().magic();
                    }

                    // Apply damage
                    entity.hurt(damageSource, baseDamage);

                    // Apply strong knockback away from explosion center
                    Vec3 entityPos = entity.position();
                    Vec3 direction = entityPos.subtract(pos).normalize();
                    double distance = entityPos.distanceTo(pos);

                    // Knockback is stronger closer to the explosion
                    double knockback = knockbackStrength * (1.0 - (distance / radius));

                    if (knockback > 0) {
                        entity.setDeltaMovement(
                            entity.getDeltaMovement().x + direction.x * knockback,
                            entity.getDeltaMovement().y + knockback * liftStrength, // Add strong upward lift
                            entity.getDeltaMovement().z + direction.z * knockback
                        );
                        entity.hurtMarked = true; // Sync movement to client
                    }

                    TalentsMod.LOGGER.debug("Nature Core exploded on {} for {} damage",
                        entity.getName().getString(), baseDamage);
                }
            });

        // Send explosion particle packet to nearby clients (includes sound)
        PacketHandler.sendToNearby(new SpawnNatureCoreExplosionPacket(pos),
            (net.minecraft.server.level.ServerLevel) level(), pos);

        // Remove the entity
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt(NBT_TICK_COUNT, this.tickCount);
        if (ownerUsername != null) {
            compound.putString(NBT_OWNER_USERNAME, ownerUsername);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(NBT_OWNER_USERNAME)) {
            this.ownerUsername = compound.getString(NBT_OWNER_USERNAME);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

}
