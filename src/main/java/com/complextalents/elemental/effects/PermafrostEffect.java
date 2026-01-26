package com.complextalents.elemental.effects;

import com.complextalents.util.UUIDHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Permafrost Effect - Slows the target and damages them when they move
 * Target's movement speed is significantly reduced, and taking steps causes frost damage.
 * Unlike Freeze, the target can still move but at a heavy cost.
 */
public class PermafrostEffect extends MobEffect {

    private static final UUID MOVEMENT_SPEED_UUID = UUIDHelper.generateAttributeModifierUUID("elemental_effects", "permafrost_slowness");
    private static final String NBT_LAST_POSITION = "PermafrostLastPosition";

    /**
     * Base damage per meter moved at amplifier 0.
     * 1.0 damage = 0.5 hearts per meter moved
     */
    public static final float BASE_DAMAGE_PER_METER = 1.0f;

    /**
     * Movement speed reduction: -60% movement speed
     */
    private static final double SLOWNESS_AMOUNT = -0.6;

    public PermafrostEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Only apply on server side
        if (entity.level().isClientSide) {
            return;
        }

        // Check if entity has moved since last tick
        CompoundTag data = entity.getPersistentData();
        Vec3 lastPos = null;
        if (data.contains(NBT_LAST_POSITION + "_x")) {
            double lastX = data.getDouble(NBT_LAST_POSITION + "_x");
            double lastY = data.getDouble(NBT_LAST_POSITION + "_y");
            double lastZ = data.getDouble(NBT_LAST_POSITION + "_z");
            lastPos = new Vec3(lastX, lastY, lastZ);
        }

        // Store current position
        Vec3 currentPos = entity.position();
        data.putDouble(NBT_LAST_POSITION + "_x", currentPos.x);
        data.putDouble(NBT_LAST_POSITION + "_y", currentPos.y);
        data.putDouble(NBT_LAST_POSITION + "_z", currentPos.z);

        // If we have a previous position, calculate distance moved and apply damage
        if (lastPos != null) {
            double distance = lastPos.distanceTo(currentPos);

            // Only apply damage if entity moved more than 0.1 blocks (ignore tiny movements)
            if (distance > 0.1) {
                // Calculate damage: base * distance * (amplifier + 1)
                float damage = (float) (BASE_DAMAGE_PER_METER * distance * (amplifier + 1));

                // Apply frost damage
                entity.hurt(entity.level().damageSources().freeze(), damage);
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Apply effect every tick to track movement
        return true;
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);

        // Apply significant slowness - reduces movement speed by 60%
        var speedInstance = attributeMap.getInstance(Attributes.MOVEMENT_SPEED);
        if (speedInstance != null) {
            // Remove existing modifier if present
            speedInstance.removeModifier(MOVEMENT_SPEED_UUID);

            AttributeModifier slowness = new AttributeModifier(
                MOVEMENT_SPEED_UUID,
                "Permafrost slowness",
                SLOWNESS_AMOUNT,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            speedInstance.addTransientModifier(slowness);
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        // Clean up NBT data
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_LAST_POSITION + "_x");
        data.remove(NBT_LAST_POSITION + "_y");
        data.remove(NBT_LAST_POSITION + "_z");
        data.remove(NBT_LAST_POSITION);

        // Remove the movement speed modifier
        var speedInstance = attributeMap.getInstance(Attributes.MOVEMENT_SPEED);
        if (speedInstance != null) {
            speedInstance.removeModifier(MOVEMENT_SPEED_UUID);
        }
    }
}
