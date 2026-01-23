package com.complextalents.elemental;

import com.complextalents.TalentsMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages resistance modifications from elemental reactions
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class ResistanceModifier {

    // UUID for Superconduct armor reduction
    private static final UUID SUPERCONDUCT_ARMOR_UUID = UUID.fromString("d8a4f728-9c7e-4b3a-9f01-2c5e8a9b7d3c");
    private static final String SUPERCONDUCT_ARMOR_NAME = "Superconduct Armor Reduction";

    // Track active resistance modifications
    private static final Map<UUID, ResistanceData> activeModifications = new ConcurrentHashMap<>();

    public static class ResistanceData {
        public final float physicalReduction;
        public final long expirationTime;

        public ResistanceData(float physicalReduction, long expirationTime) {
            this.physicalReduction = physicalReduction;
            this.expirationTime = expirationTime;
        }
    }

    /**
     * Apply Superconduct's physical resistance reduction
     */
    public static void applySuperconductReduction(LivingEntity target, float reductionPercent, int durationTicks) {
        UUID targetId = target.getUUID();

        // Store resistance data
        long expirationTime = target.level().getGameTime() + durationTicks;
        activeModifications.put(targetId, new ResistanceData(reductionPercent, expirationTime));

        // Apply armor reduction
        AttributeInstance armorAttribute = target.getAttribute(Attributes.ARMOR);
        if (armorAttribute != null) {
            // Remove existing modifier if present
            armorAttribute.removeModifier(SUPERCONDUCT_ARMOR_UUID);

            // Calculate reduction amount (negative multiplier)
            double reduction = -reductionPercent;
            AttributeModifier modifier = new AttributeModifier(
                SUPERCONDUCT_ARMOR_UUID,
                SUPERCONDUCT_ARMOR_NAME,
                reduction,
                AttributeModifier.Operation.MULTIPLY_BASE
            );

            armorAttribute.addPermanentModifier(modifier);
        }

        // Also reduce armor toughness
        AttributeInstance toughnessAttribute = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughnessAttribute != null) {
            UUID toughnessUUID = UUID.fromString("e9a5f829-0c8f-4c4b-9g02-3d6f9b0c8e4d");
            toughnessAttribute.removeModifier(toughnessUUID);

            AttributeModifier toughnessModifier = new AttributeModifier(
                toughnessUUID,
                "Superconduct Toughness Reduction",
                -reductionPercent,
                AttributeModifier.Operation.MULTIPLY_BASE
            );

            toughnessAttribute.addPermanentModifier(toughnessModifier);
        }
    }

    /**
     * Remove Superconduct reduction when it expires
     */
    public static void removeSuperconductReduction(LivingEntity target) {
        UUID targetId = target.getUUID();
        activeModifications.remove(targetId);

        // Remove armor modifiers
        AttributeInstance armorAttribute = target.getAttribute(Attributes.ARMOR);
        if (armorAttribute != null) {
            armorAttribute.removeModifier(SUPERCONDUCT_ARMOR_UUID);
        }

        AttributeInstance toughnessAttribute = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughnessAttribute != null) {
            UUID toughnessUUID = UUID.fromString("e9a5f829-0c8f-4c4b-9g02-3d6f9b0c8e4d");
            toughnessAttribute.removeModifier(toughnessUUID);
        }
    }

    /**
     * Modify physical damage based on active resistance reductions
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        UUID targetId = target.getUUID();

        // Check if target has active resistance reduction
        ResistanceData data = activeModifications.get(targetId);
        if (data != null) {
            // Check if expired
            if (target.level().getGameTime() > data.expirationTime) {
                removeSuperconductReduction(target);
                return;
            }

            // Check if damage is physical (not magic, fire, etc.)
            if (isPhysicalDamage(event.getSource().getMsgId())) {
                // Increase damage based on resistance reduction
                float originalDamage = event.getAmount();
                float increasedDamage = originalDamage * (1.0f + data.physicalReduction);
                event.setAmount(increasedDamage);
            }
        }
    }

    /**
     * Check if a damage type is physical
     */
    private static boolean isPhysicalDamage(String damageType) {
        return switch (damageType) {
            case "mob", "player", "arrow", "trident", "thrown", "sting",
                 "mobAttack", "playerAttack", "generic" -> true;
            case "inFire", "lightningBolt", "onFire", "lava", "hotFloor",
                 "inWall", "cramming", "drown", "starve", "cactus",
                 "fall", "flyIntoWall", "outOfWorld", "magic", "wither",
                 "dryout", "sweetBerryBush", "freeze", "stalagmite",
                 "indirectMagic", "thorns" -> false;
            default -> false;
        };
    }

    /**
     * Clean up expired modifications periodically
     */
    public static void cleanupExpired(long currentTime) {
        activeModifications.entrySet().removeIf(entry ->
            entry.getValue().expirationTime < currentTime);
    }

    /**
     * Check if an entity has active Superconduct reduction
     */
    public static boolean hasSuperconductReduction(LivingEntity entity) {
        ResistanceData data = activeModifications.get(entity.getUUID());
        return data != null && entity.level().getGameTime() <= data.expirationTime;
    }

    /**
     * Get the current physical reduction percentage for an entity
     */
    public static float getPhysicalReduction(LivingEntity entity) {
        ResistanceData data = activeModifications.get(entity.getUUID());
        if (data != null && entity.level().getGameTime() <= data.expirationTime) {
            return data.physicalReduction;
        }
        return 0f;
    }
}