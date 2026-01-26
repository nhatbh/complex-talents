package com.complextalents.elemental.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

import com.complextalents.util.UUIDHelper;

/**
 * Freeze Effect - Encases target in ice
 * Target is frozen and cannot move. Physical hits deal 2.5x damage and break the ice instantly.
 *
 * The effect stores a hit counter in the entity's persistent NBT data to track physical hits.
 * When a physical hit occurs, the counter increments and the effect breaks.
 */
public class FreezeEffect extends MobEffect {

    private static final String NBT_HIT_COUNTER = "FreezeHitCounter";
    private static final String NBT_PHYSICAL_HITS = "FreezePhysicalHits";
    private static final UUID MOVEMENT_SPEED_UUID = UUIDHelper.generateAttributeModifierUUID("elemental_effects", "freeze_slowness");

    public FreezeEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Prevent movement by zeroing out delta movement
        if (!entity.level().isClientSide) {
            entity.setDeltaMovement(0, entity.getDeltaMovement().y * 0.1, 0);
            entity.hurtMarked = true;
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Apply effect every tick to continuously prevent movement
        return true;
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);
        resetHitCounter(entity);

        // Apply maximum slowness - reduces movement speed by 100% (completely immobilizes)
        var speedInstance = attributeMap.getInstance(Attributes.MOVEMENT_SPEED);
        if (speedInstance != null) {
            // Remove existing modifier if present
            speedInstance.removeModifier(MOVEMENT_SPEED_UUID);

            AttributeModifier slowness = new AttributeModifier(
                MOVEMENT_SPEED_UUID,
                "Freeze slowness",
                -1.0, // -100% movement speed
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            speedInstance.addTransientModifier(slowness);
        }
    }

    /**
     * Records a physical hit on the frozen target.
     * This is called by the FreezeReaction when physical damage is detected.
     *
     * @param entity The frozen entity
     * @return true if the ice should break (physical hit occurred), false otherwise
     */
    public static boolean recordPhysicalHit(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        int hits = data.getInt(NBT_PHYSICAL_HITS) + 1;
        data.putInt(NBT_PHYSICAL_HITS, hits);

        // Break the ice on first physical hit
        return hits >= 1;
    }

    /**
     * Resets the physical hit counter.
     * Called when the effect is applied.
     *
     * @param entity The entity
     */
    public static void resetHitCounter(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_PHYSICAL_HITS);
        data.remove(NBT_HIT_COUNTER);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        resetHitCounter(entity);

        // Remove the movement speed modifier
        var speedInstance = attributeMap.getInstance(Attributes.MOVEMENT_SPEED);
        if (speedInstance != null) {
            speedInstance.removeModifier(MOVEMENT_SPEED_UUID);
        }
    }
}
