package com.complextalents.impl.yygm.effect;

import com.complextalents.impl.yygm.state.YinYangState;
import com.complextalents.impl.yygm.util.YinYangNbtUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;
import java.util.UUID;

/**
 * Abstract base class for all YYGM effects.
 * Provides common functionality for NBT data management, state tracking, and effect lifecycle.
 * <p>
 * This eliminates code duplication between HarmonizedEffect, ExposedEffect, and YinYangAnnihilationEffect.
 * </p>
 */
public abstract class BaseYinYangEffect extends MobEffect {

    private final YinYangState state;
    private final String nbtRoot;

    /**
     * Create a new YYGM effect.
     *
     * @param category The effect category (typically HARMFUL)
     * @param color The color for rendering
     * @param state The YYGM state this effect represents
     * @param nbtRoot The NBT root key for storing effect data
     */
    public BaseYinYangEffect(MobEffectCategory category, int color,
                            YinYangState state, String nbtRoot) {
        super(category, color);
        this.state = state;
        this.nbtRoot = nbtRoot;
    }

    /**
     * Get the YYGM state this effect represents.
     */
    public final YinYangState getYinYangState() {
        return state;
    }

    /**
     * Get the NBT root key for this effect's data storage.
     */
    public final String getNbtRoot() {
        return nbtRoot;
    }

    /**
     * YYGM effects use manual tick handling via YinYangStateManager, not the default tick system.
     */
    @Override
    public final boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    /**
     * Get or create player data NBT for this effect.
     * Uses YinYangNbtUtil for unified NBT management.
     *
     * @param entity The entity storing the data
     * @param playerUuid The player's UUID
     * @return The player's data tag
     */
    protected final CompoundTag getOrCreatePlayerData(LivingEntity entity, UUID playerUuid) {
        return YinYangNbtUtil.getOrCreatePlayerData(entity, playerUuid, nbtRoot,
            tag -> initializePlayerData(tag));
    }

    /**
     * Initialize a new player data tag with default values.
     * Subclasses override this to set effect-specific initial values.
     *
     * @param tag The tag to initialize
     * @return The initialized tag
     */
    protected abstract CompoundTag initializePlayerData(CompoundTag tag);

    /**
     * Save player data to entity NBT.
     *
     * @param entity The entity storing the data
     * @param playerUuid The player's UUID
     * @param data The data to save
     */
    protected final void savePlayerData(LivingEntity entity, UUID playerUuid, CompoundTag data) {
        YinYangNbtUtil.savePlayerData(entity, playerUuid, data, nbtRoot);
    }

    /**
     * Clean up player data from entity NBT.
     * Also removes the root tag if it becomes empty.
     *
     * @param entity The entity storing the data
     * @param playerUuid The player's UUID
     * @param nbtRoot The NBT root key
     */
    public static void cleanupPlayerData(LivingEntity entity, UUID playerUuid, String nbtRoot) {
        YinYangNbtUtil.cleanupPlayerData(entity, playerUuid, nbtRoot);
    }

    /**
     * Clean up player data from entity NBT (instance method for this effect).
     *
     * @param entity The entity storing the data
     * @param playerUuid The player's UUID
     */
    public void cleanupPlayerDataInstance(LivingEntity entity, UUID playerUuid) {
        YinYangNbtUtil.cleanupPlayerData(entity, playerUuid, nbtRoot);
    }

    /**
     * Get all players who have this effect's data on an entity.
     *
     * @param entity The entity to check
     * @return Set of player UUIDs
     */
    public Set<UUID> getPlayersWithData(LivingEntity entity) {
        return YinYangNbtUtil.getPlayersWithData(entity, nbtRoot, "player_uuid");
    }

    /**
     * Check if this entity has any data of this effect type.
     *
     * @param entity The entity to check
     * @return true if any data exists
     */
    public boolean hasAnyData(LivingEntity entity) {
        return YinYangNbtUtil.hasAnyData(entity, nbtRoot);
    }

    /**
     * Apply this effect to a target entity for a player.
     * Subclasses must implement this with their specific logic.
     *
     * @param target The target entity
     * @param playerUuid The player's UUID
     * @param durationTicks Duration in ticks (may be ignored by some effects)
     */
    public abstract void applyToTarget(LivingEntity target, UUID playerUuid, int durationTicks);

    /**
     * Remove this effect from a target entity for a player.
     * Subclasses must implement this with their specific cleanup logic.
     *
     * @param entity The entity
     * @param playerUuid The player's UUID
     */
    public abstract void removeFromTarget(LivingEntity entity, UUID playerUuid);
}
