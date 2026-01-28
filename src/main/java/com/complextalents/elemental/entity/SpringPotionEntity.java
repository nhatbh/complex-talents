package com.complextalents.elemental.entity;

import com.complextalents.TalentsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Spring Potion Entity - A potion item spawned by Spring reaction (Aqua + Ender)
 *
 * Features:
 * - Renders as a Potion item
 * - No AI
 * - No item drops
 * - Glowing effect
 * - Cannot be targeted/attacked
 * - When a player collides with it, grants a random buff for 10 seconds
 * - Despawns after 15 seconds if not picked up
 * - Buff amplifier scales with the attacker's elemental mastery
 */
public class SpringPotionEntity extends Mob {

    private static final int LIFETIME_TICKS = 300; // 15 seconds (20 ticks * 15)
    private static final int BUFF_DURATION_SECONDS = 10; // 10 seconds
    private static final String NBT_OWNER_USERNAME = "SpringPotionOwnerUsername";
    private static final String NBT_MASTERY = "SpringPotionMastery";

    private String ownerUsername;
    private float mastery = 1.0f;

    public SpringPotionEntity(EntityType<? extends SpringPotionEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoAi(true);
    }

    /**
     * Sets the owner of this Spring Potion entity.
     * Used to calculate the buff amplifier based on their elemental mastery.
     *
     * @param ownerUsername The username of the owner
     * @param mastery The elemental mastery value for buff calculation
     */
    public void setOwner(String ownerUsername, float mastery) {
        this.ownerUsername = ownerUsername;
        this.mastery = mastery;
    }

    /**
     * Gets the owner player if they're still in the level.
     *
     * @return The owner player, or null if not found/invalid
     */
    @SuppressWarnings("unused")
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
        return false; // Cannot be interacted with via right-click
    }

    @Override
    public boolean canBeCollidedWith() {
        return true; // Can be collided with for pickup
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

        // Check for player collision
        for (Player player : level().getEntitiesOfClass(Player.class,
                getBoundingBox().inflate(0.5))) {
            if (player.isAlive()) {
                applyBuffToPlayer(player);
                discard(); // Remove after pickup
                return;
            }
        }

        // Check for lifetime expiration
        if (this.tickCount >= LIFETIME_TICKS) {
            discard();
        }
    }

    /**
     * Applies a random buff to the player.
     * The buff amplifier scales with elemental mastery.
     *
     * @param player The player to apply the buff to
     */
    private void applyBuffToPlayer(Player player) {
        // Calculate amplifier based on mastery
        // Mastery 1-5: amplifier 0, 5-10: amplifier 1, 10+: amplifier 2
        int amplifier = 0;
        if (mastery >= 10) {
            amplifier = 2;
        } else if (mastery >= 5) {
            amplifier = 1;
        }

        // Available buffs
        MobEffect[] possibleBuffs = {
            MobEffects.REGENERATION,
            MobEffects.MOVEMENT_SPEED,
            MobEffects.DAMAGE_BOOST,
            MobEffects.DAMAGE_RESISTANCE,
            MobEffects.ABSORPTION
        };

        // Pick a random buff
        MobEffect selectedBuff = possibleBuffs[level().getRandom().nextInt(possibleBuffs.length)];

        // Apply the buff
        MobEffectInstance effect = new MobEffectInstance(
            selectedBuff,
            BUFF_DURATION_SECONDS * 20, // Convert to ticks
            amplifier,
            false,
            true
        );
        player.addEffect(effect);

        // Log the buff application
        TalentsMod.LOGGER.info("Spring Potion applied {} (amplifier {}) to {}",
            selectedBuff.getDescriptionId(), amplifier, player.getName().getString());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("SpringPotionTickCount", this.tickCount);
        if (ownerUsername != null) {
            compound.putString(NBT_OWNER_USERNAME, ownerUsername);
        }
        compound.putFloat(NBT_MASTERY, mastery);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains(NBT_OWNER_USERNAME)) {
            this.ownerUsername = compound.getString(NBT_OWNER_USERNAME);
        }
        if (compound.contains(NBT_MASTERY)) {
            this.mastery = compound.getFloat(NBT_MASTERY);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }
}
