package com.complextalents.elemental.entity;

import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnBlackHoleParticlePacket;
import com.complextalents.util.TeamHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Black Hole Entity - A singularity spawned by Flux reaction (Lightning + Ender)
 * Renders as a spinning black concrete block, pulls entities toward it, then implodes for damage
 *
 * Features:
 * - Renders as a black concrete block item spinning on both axes
 * - Pulls all entities in 3-block range toward it every second
 * - Implodes after 5 seconds (shrinks then deals damage)
 * - Deals 2 base damage * mastery magic damage in 2-block radius on implosion
 */
public class BlackHoleEntity extends Mob {

    private static final int LIFETIME_TICKS = 100; // 5 seconds (20 ticks * 5)
    private static final double PULL_RANGE = 5.0; // 5 block pull range
    private static final double DAMAGE_RANGE = 2.0; // 2 block damage range
    private static final double PULL_STRENGTH = 0.1; // Pull strength per tick
    private static final String NBT_OWNER_USERNAME = "BlackHoleOwnerUsername";
    private static final String NBT_IMPLODING = "BlackHoleImplosiong";

    private String ownerUsername;
    private boolean isImploding = false;

    public BlackHoleEntity(EntityType<? extends BlackHoleEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoAi(true);
    }

    /**
     * Sets the owner of this Black Hole entity.
     * The owner will receive credit for damage dealt by the implosion.
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
        return false; // Always glowing for visibility
    }

    @Override
    protected void registerGoals() {
        // NO GOALS - entity does not move or act on its own
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

        Vec3 pos = position();

        // Send particle effect packet to nearby clients every tick
        PacketHandler.sendToNearby(new SpawnBlackHoleParticlePacket(pos, isImploding),
            (ServerLevel) level(), pos);

        // Check if we should start imploding (at 4 seconds, 1 second left)
        if (!isImploding && this.tickCount >= (LIFETIME_TICKS - 20)) {
            isImploding = true;
        }

        // Pull entities toward the black hole every tick
        if (!isImploding) {
            pullEntities();
        }

        // Check for lifetime expiration
        if (this.tickCount >= LIFETIME_TICKS) {
            implode();
        }
    }

    /**
     * Pulls all entities in range toward the black hole
     */
    private void pullEntities() {
        Vec3 pos = position();
        ServerPlayer owner = getOwner();

        level().getEntitiesOfClass(LivingEntity.class,
            getBoundingBox().inflate(PULL_RANGE))
            .forEach(entity -> {
                if (entity != this) {
                    // Skip allies of the owner - don't pull friendly targets
                    if (owner != null && TeamHelper.isAlly(owner, entity)) {
                        return;
                    }

                    Vec3 entityPos = entity.position();
                    Vec3 direction = pos.subtract(entityPos).normalize();
                    double distance = entityPos.distanceTo(pos);

                    // Pull is stronger closer to the black hole
                    double pullStrength = PULL_STRENGTH * (1.0 - (distance / PULL_RANGE));

                    if (pullStrength > 0) {
                        entity.setDeltaMovement(
                            entity.getDeltaMovement().x + direction.x * pullStrength,
                            entity.getDeltaMovement().y, // No vertical pull
                            entity.getDeltaMovement().z + direction.z * pullStrength
                        );
                        entity.hurtMarked = true; // Sync movement to client
                    }
                }
            });
    }

    /**
     * Implodes the black hole, dealing damage to nearby entities
     */
    private void implode() {
        if (level().isClientSide) {
            return;
        }

        Vec3 pos = position();
        ServerPlayer owner = getOwner();
        float mastery = calculateMastery();

        // Find nearby entities and deal magic damage
        level().getEntitiesOfClass(LivingEntity.class,
            getBoundingBox().inflate(DAMAGE_RANGE))
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

                    // Deal 2 base damage * mastery
                    float damage = 2.0f * mastery;
                    entity.hurt(damageSource, damage);

                    TalentsMod.LOGGER.debug("Black Hole imploded on {} for {} damage",
                        entity.getName().getString(), damage);
                }
            });

        // Play implosion sound
        level().playLocalSound(pos.x, pos.y, pos.z,
            net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
            net.minecraft.sounds.SoundSource.HOSTILE,
            1.0f, 0.5f, false);

        // Remove the entity
        discard();
    }

    /**
     * Calculates mastery based on owner's attributes
     *
     * @return The mastery value for damage calculation
     */
    private float calculateMastery() {
        ServerPlayer owner = getOwner();
        if (owner == null) {
            return 1.0f;
        }

        // Get lightning spell power (primary element for this reaction)
        double lightningPower = owner.getAttributeValue(
            net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "lightning_spell_power")));

        // Get ender spell power (secondary element)
        double enderPower = owner.getAttributeValue(
            net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "ender_spell_power")));

        // Use the average of the two
        return (float) ((lightningPower + enderPower) / 2.0);
    }

    /**
     * Check if the black hole is currently imploding
     * Used by renderer to shrink the model
     *
     * @return true if imploding
     */
    public boolean isImploding() {
        return isImploding;
    }

    /**
     * Get the current scale for rendering
     * Returns 1.0 normally, shrinking to 0 during implosion
     *
     * @return The scale factor (0.0 to 1.0)
     */
    public float getRenderScale() {
        if (!isImploding) {
            return 1.0f;
        }

        // Shrink from 1.0 to 0.0 over the last 20 ticks
        int implosionTicks = this.tickCount - (LIFETIME_TICKS - 20);
        return 1.0f - (implosionTicks / 20.0f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (ownerUsername != null) {
            compound.putString(NBT_OWNER_USERNAME, ownerUsername);
        }
        compound.putBoolean(NBT_IMPLODING, isImploding);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(NBT_OWNER_USERNAME)) {
            this.ownerUsername = compound.getString(NBT_OWNER_USERNAME);
        }
        if (compound.contains(NBT_IMPLODING)) {
            this.isImploding = compound.getBoolean(NBT_IMPLODING);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }
}
