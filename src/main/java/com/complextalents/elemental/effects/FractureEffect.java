package com.complextalents.elemental.effects;

import com.complextalents.TalentsMod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Fracture Effect - Shatters defenses
 * Sets target's armor to 0 for the next 3 hits received.
 * Uses NBT data to track the number of hits taken.
 */
public class FractureEffect extends MobEffect {

    private static final String NBT_HIT_COUNTER = "FractureHitCounter";
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("b9c4d5e6-7f8a-4b9c-0d1e-2f3a4b5c6d7e");
    private static final int MAX_HITS = 3;

    public FractureEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is passive, armor reduction is handled via attribute modifier
        // Hit tracking is handled by event listener
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false; // No periodic ticks needed
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);

        // Apply -100% armor modifier (sets armor to 0)
        var armorInstance = attributeMap.getInstance(Attributes.ARMOR);
        if (armorInstance != null) {
            // Remove existing modifier if present
            armorInstance.removeModifier(ARMOR_MODIFIER_UUID);

            AttributeModifier fractureModifier = new AttributeModifier(
                ARMOR_MODIFIER_UUID,
                "Fracture armor shatter",
                -1.0, // -100% armor
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            armorInstance.addTransientModifier(fractureModifier);
        }

        // Reset hit counter
        resetHitCounter(entity);

        // Log fracture effect application
        TalentsMod.LOGGER.info("Fracture effect applied to {} - Armor set to 0 for 3 hits", entity.getName().getString());

        // Send chat message to nearby players
        if (!entity.level().isClientSide) {
            Component message = Component.literal("")
                .append(Component.literal("✦ FRACTURE ✦")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#8A2BE2")).withBold(true)))
                .append(Component.literal("\n"))
                .append(Component.literal(entity.getName().getString())
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF4444"))))
                .append(Component.literal("'s armor SHATTERED! Next ")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#AAAAAA"))))
                .append(Component.literal("3 hits")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFD700")).withBold(true)))
                .append(Component.literal(" will bypass armor!")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#AAAAAA"))));

            entity.level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, entity.getBoundingBox().inflate(16))
                .forEach(player -> player.sendSystemMessage(message));
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        var armorInstance = attributeMap.getInstance(Attributes.ARMOR);
        if (armorInstance != null) {
            armorInstance.removeModifier(ARMOR_MODIFIER_UUID);
        }

        // Log effect removal
        int finalHits = getHitCount(entity);
        TalentsMod.LOGGER.info("Fracture effect removed from {} - Total hits taken: {}/{}", entity.getName().getString(), finalHits, MAX_HITS);

        // Clean up NBT data
        resetHitCounter(entity);
    }

    /**
     * Records a hit on the fractured target.
     * This should be called from a damage event listener.
     *
     * @param entity The fractured entity
     * @return true if the effect should end (max hits reached), false otherwise
     */
    public static boolean recordHit(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        int hits = data.getInt(NBT_HIT_COUNTER) + 1;
        data.putInt(NBT_HIT_COUNTER, hits);

        TalentsMod.LOGGER.info("Fracture hit recorded on {} - Hit {}/{}", entity.getName().getString(), hits, MAX_HITS);

        // Send chat message with hit progress
        if (!entity.level().isClientSide) {
            Component message = Component.literal("")
                .append(Component.literal("✦ FRACTURE HIT ✦")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#8A2BE2")).withBold(true)))
                .append(Component.literal("\n"))
                .append(Component.literal(entity.getName().getString())
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF4444"))))
                .append(Component.literal(" hit ")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#AAAAAA"))))
                .append(Component.literal(String.valueOf(hits))
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFD700")).withBold(true)))
                .append(Component.literal("/")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#AAAAAA"))))
                .append(Component.literal(String.valueOf(MAX_HITS))
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFD700")).withBold(true)))
                .append(Component.literal(hits >= MAX_HITS ? " - Armor restored!" : " - Armor still shattered!")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor(hits >= MAX_HITS ? "#00FF00" : "#FF4444"))));

            entity.level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, entity.getBoundingBox().inflate(16))
                .forEach(player -> player.sendSystemMessage(message));
        }

        // Check if max hits reached
        if (hits >= MAX_HITS) {
            TalentsMod.LOGGER.info("Fracture effect ended on {} - Max hits reached", entity.getName().getString());
            // Remove the effect - will be done by checking effect instance
            return true;
        }

        return false;
    }

    /**
     * Gets the current hit count.
     *
     * @param entity The fractured entity
     * @return Number of hits taken
     */
    public static int getHitCount(LivingEntity entity) {
        return entity.getPersistentData().getInt(NBT_HIT_COUNTER);
    }

    /**
     * Resets the hit counter.
     *
     * @param entity The entity
     */
    public static void resetHitCounter(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_HIT_COUNTER);
    }

    /**
     * Checks if the entity has reached max hits.
     *
     * @param entity The fractured entity
     * @return true if max hits reached
     */
    public static boolean isMaxHitsReached(LivingEntity entity) {
        return getHitCount(entity) >= MAX_HITS;
    }
}
