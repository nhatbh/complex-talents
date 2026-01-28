package com.complextalents.elemental.effects;

import com.complextalents.TalentsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Unstable Bio-energy Effect - Applied by Overgrowth reaction (Lightning + Nature)
 * When the target dies while this effect is active, they explode in a burst of life energy
 * The explosion looks like a totem of undying effect combined with ember sparks
 * Deals 3*mastery magic damage to all enemies in 5 blocks
 */
public class UnstableBioEnergyEffect extends MobEffect {

    private static final String NBT_ATTACKER_USERNAME = "UnstableBioEnergyAttacker";
    private static final String NBT_MASTERY = "UnstableBioEnergyMastery";

    // Explosion range in blocks
    private static final double EXPLOSION_RANGE = 5.0;

    // Damage multiplier: 3 * mastery
    private static final float DAMAGE_MULTIPLIER = 3.0f;

    public UnstableBioEnergyEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Effect is passive - only triggers on death
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false; // No periodic ticks needed
    }

    /**
     * Stores the attacker username when the effect is applied.
     * This ensures proper damage attribution when the target explodes.
     *
     * @param entity The entity with the effect
     * @param attackerUsername The username of the player who applied the effect
     */
    public static void setAttackerUsername(LivingEntity entity, String attackerUsername) {
        CompoundTag data = entity.getPersistentData();
        data.putString(NBT_ATTACKER_USERNAME, attackerUsername);
    }

    /**
     * Gets the stored attacker username from the entity's NBT data.
     *
     * @param entity The entity with the effect
     * @return The attacker username, or null if not found
     */
    public static String getAttackerUsername(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_ATTACKER_USERNAME)) {
            return data.getString(NBT_ATTACKER_USERNAME);
        }
        return null;
    }

    /**
     * Clears the stored attacker username from the entity's NBT data.
     *
     * @param entity The entity with the effect
     */
    public static void clearAttackerUsername(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_ATTACKER_USERNAME);
    }

    /**
     * Stores the elemental mastery when the effect is applied.
     * Used to calculate explosion damage on death.
     *
     * @param entity The entity with the effect
     * @param mastery The elemental mastery value
     */
    public static void setMastery(LivingEntity entity, float mastery) {
        CompoundTag data = entity.getPersistentData();
        data.putFloat(NBT_MASTERY, mastery);
    }

    /**
     * Gets the stored elemental mastery from the entity's NBT data.
     *
     * @param entity The entity with the effect
     * @return The mastery value, or 1.0f if not found
     */
    public static float getMastery(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_MASTERY)) {
            return data.getFloat(NBT_MASTERY);
        }
        return 1.0f;
    }

    /**
     * Triggers the bio-energy explosion when the target dies.
     * Creates an effect similar to totem of undying with ember sparks
     * Deals 3*mastery magic damage to all enemies in 5 blocks
     *
     * @param entity The entity that died
     */
    public static void triggerExplosion(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }

        Vec3 pos = entity.position();
        ServerLevel level = (ServerLevel) entity.level();

        // Get the stored attacker and mastery
        String attackerUsername = getAttackerUsername(entity);
        final ServerPlayer attacker;

        if (attackerUsername != null && level.getServer() != null) {
            attacker = level.getServer().getPlayerList().getPlayerByName(attackerUsername);
        } else {
            attacker = null;
        }

        final float mastery = getMastery(entity);
        final float damage = DAMAGE_MULTIPLIER * mastery;

        TalentsMod.LOGGER.info("Unstable Bio-energy exploded on {} for {} damage (mastery: {})",
            entity.getName().getString(), String.format("%.2f", damage), String.format("%.2f", mastery));

        // Deal damage to all non-ally entities in range
        level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(EXPLOSION_RANGE))
            .forEach(target -> {
                // Don't damage the exploding entity (already dead)
                if (target == entity) {
                    return;
                }

                // Skip allies of the attacker
                if (attacker != null && com.complextalents.util.TeamHelper.isAlly(attacker, target)) {
                    return;
                }

                DamageSource damageSource;
                if (attacker != null) {
                    damageSource = level.damageSources().playerAttack(attacker);
                } else {
                    damageSource = level.damageSources().magic();
                }

                target.hurt(damageSource, damage);

                TalentsMod.LOGGER.debug("Bio-energy explosion hit {} for {} damage",
                    target.getName().getString(), String.format("%.2f", damage));
            });

        // Play a magical sound for the bio-energy explosion (similar to totem effect)
        level.playLocalSound(pos.x, pos.y, pos.z,
            SoundEvents.EVOKER_CAST_SPELL,
            SoundSource.HOSTILE,
            1.5f, 1.0f, false);

        // Clean up NBT data
        clearAttackerUsername(entity);
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_MASTERY);
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        // Clean up NBT data when effect is removed (unless triggering explosion)
        // The explosion handler will clean up separately
        clearAttackerUsername(entity);
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_MASTERY);
    }
}
