package com.complextalents.impl.yygm.effect;

import com.complextalents.TalentsMod;
import com.complextalents.impl.yygm.state.PlayerTargetTracker;
import com.complextalents.impl.yygm.state.YinYangState;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.yygm.YinYangGateStateSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * Yin Yang Annihilation Effect - The reward for completing all 8 Exposed gates.
 * <p>
 * When a YYGM successfully hits all 8 gates of the Exposed effect,
 * the target gains Yin Yang Annihilation. During this state:
 * - All attacks deal true amplified damage from any angle
 * - No gate restrictions
 * - Duration is the remaining Exposed duration
 * </p>
 * <p>
 * Refactored to extend BaseYinYangEffect and use unified PlayerTargetTracker.
 * </p>
 */
public class YinYangAnnihilationEffect extends BaseYinYangEffect {

    /** NBT root key for annihilation data */
    private static final String NBT_ROOT = "yygm_yin_yang_annihilation";

    public YinYangAnnihilationEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF0000, YinYangState.ANNIHILATION, NBT_ROOT);
    }

    @Override
    protected CompoundTag initializePlayerData(CompoundTag tag) {
        // No duration storage - uses Minecraft's effect system
        return tag;
    }

    /**
     * Apply Yin Yang Annihilation effect to a target entity.
     * Removes Harmonized effect if present on this target.
     *
     * @param target The target entity
     * @param playerUuid The YYGM player's UUID
     * @param durationTicks Duration in ticks
     */
    @Override
    public void applyToTarget(LivingEntity target, UUID playerUuid, int durationTicks) {
        if (!target.level().isClientSide) {
            // Remove Harmonized effect if present on this target
            Integer harmonizedId = HarmonizedEffect.getHarmonizedEntityId(playerUuid);
            if (harmonizedId != null && harmonizedId == target.getId()) {
                HarmonizedEffect.clearHarmonizedTracking(playerUuid);
                target.removeEffect(YinYangEffects.HARMONIZED.get());
                HarmonizedEffect.cleanupPlayerData(target, playerUuid);

                // Send removal packet to clear client-side harmonized gate visualization
                if (target.level() instanceof ServerLevel level) {
                    PacketHandler.sendToNearby(
                        new YinYangGateStateSyncPacket(
                            target.getId(), playerUuid, -2, 0, 0, 0, 0, 0
                        ),
                        level, target.position()
                    );
                }

                TalentsMod.LOGGER.debug("Removed Harmonized effect from target {} before applying Yin Yang Annihilation",
                    target.getName().getString());
            }
        }

        // No NBT storage needed for Annihilation - duration tracked by Minecraft

        // Apply the mob effect instance
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            YinYangEffects.YIN_YANG_ANNIHILATION.get(),
            durationTicks,
            0,
            false,
            false  // Hide particles
        ));

        TalentsMod.LOGGER.debug("Applied Yin Yang Annihilation to {} for player {}, duration: {} ticks",
            target.getName().getString(), playerUuid, durationTicks);
    }

    /**
     * Remove the Yin Yang Annihilation effect from a target entity.
     */
    @Override
    public void removeFromTarget(LivingEntity entity, UUID playerUuid) {
        // Remove mob effect
        entity.removeEffect(YinYangEffects.YIN_YANG_ANNIHILATION.get());

        // Cleanup NBT data
        cleanupPlayerData(entity, playerUuid);

        // Clear player tracking if this was their target
        Integer cachedId = PlayerTargetTracker.getEntityId(playerUuid);
        if (cachedId != null && cachedId == entity.getId()) {
            PlayerTargetTracker.clearTarget(playerUuid);
        }

        TalentsMod.LOGGER.debug("Removed Yin Yang Annihilation from {} for player {}",
            entity.getName().getString(), playerUuid);
    }

    // ===== Static API Methods (for backward compatibility) =====

    /**
     * Check if a target has Yin Yang Annihilation by a specific player.
     */
    public static boolean hasYinYangAnnihilation(LivingEntity entity, UUID playerUuid) {
        return entity.hasEffect(YinYangEffects.YIN_YANG_ANNIHILATION.get());
    }

    /**
     * Get the entity ID of the player's current Yin Yang Annihilation target.
     * Delegates to PlayerTargetTracker for unified tracking.
     */
    public static Integer getAnnihilationEntityId(UUID playerUuid) {
        PlayerTargetTracker.TargetData data = PlayerTargetTracker.getTarget(playerUuid);
        if (data != null && data.state() == YinYangState.ANNIHILATION) {
            return data.entityId();
        }
        return null;
    }

    /**
     * Set the player's Yin Yang Annihilation target.
     * Delegates to PlayerTargetTracker for unified tracking.
     */
    public static void setAnnihilationTarget(UUID playerUuid, int entityId) {
        PlayerTargetTracker.setTarget(playerUuid, YinYangState.ANNIHILATION, entityId);
    }

    /**
     * Clear the player's Yin Yang Annihilation target tracking.
     * Delegates to PlayerTargetTracker for unified tracking.
     */
    public static void clearAnnihilationTarget(UUID playerUuid) {
        PlayerTargetTracker.TargetData data = PlayerTargetTracker.getTarget(playerUuid);
        if (data != null && data.state() == YinYangState.ANNIHILATION) {
            PlayerTargetTracker.clearTarget(playerUuid);
        }
    }

    /**
     * Check if the player has an active Yin Yang Annihilation target.
     */
    public static boolean hasAnnihilationTarget(UUID playerUuid) {
        return PlayerTargetTracker.hasTargetInState(playerUuid, YinYangState.ANNIHILATION);
    }

    /**
     * Check if this entity has any Yin Yang Annihilation data from any player.
     */
    public static boolean hasAnyAnnihilation(LivingEntity entity) {
        YinYangAnnihilationEffect effect = (YinYangAnnihilationEffect) YinYangEffects.YIN_YANG_ANNIHILATION.get();
        return effect.hasAnyData(entity);
    }

    /**
     * Get all players who have Yin Yang Annihilation data on this entity.
     */
    public static java.util.Set<UUID> getAnnihilationPlayers(LivingEntity entity) {
        YinYangAnnihilationEffect effect = (YinYangAnnihilationEffect) YinYangEffects.YIN_YANG_ANNIHILATION.get();
        return effect.getPlayersWithData(entity);
    }

    /**
     * Static cleanup method for static context calls.
     */
    public static void cleanupPlayerData(LivingEntity entity, UUID playerUuid) {
        BaseYinYangEffect.cleanupPlayerData(entity, playerUuid, "yygm_yin_yang_annihilation");
    }

    /**
     * Static wrapper for applyToTarget.
     */
    public static void applyToTargetStatic(LivingEntity target, UUID playerUuid, int durationTicks) {
        YinYangAnnihilationEffect effect = (YinYangAnnihilationEffect) YinYangEffects.YIN_YANG_ANNIHILATION.get();
        effect.applyToTarget(target, playerUuid, durationTicks);
    }

    /**
     * Static wrapper for removeFromTarget.
     */
    public static void removeFromTargetStatic(LivingEntity entity, UUID playerUuid) {
        YinYangAnnihilationEffect effect = (YinYangAnnihilationEffect) YinYangEffects.YIN_YANG_ANNIHILATION.get();
        effect.removeFromTarget(entity, playerUuid);
    }
}
