package com.complextalents.skill.example;

import com.complextalents.skill.SkillBuilder;
import com.complextalents.targeting.TargetType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Example ACTIVE skill: Thunder Strike (with levels!)
 * - Targeting: ENTITY (must target a living entity)
 * - Max Level: 5
 * - Cooldown: 6 seconds
 * - Resource: 25 mana
 * - Effect: Summons a lightning bolt to strike the targeted entity
 * - Scaled stats:
 *   - damage: 5, 8, 12, 18, 25 (extra magic damage)
 *   - knockback: 0.5, 0.7, 0.9, 1.2, 1.5 (knockback strength)
 *   - radius: 1, 1, 2, 2, 3 (additional chain lightning targets)
 *
 * Usage:
 * - /skill assign 1 complextalents:thunder_strike 1 (level 1 - default)
 * - /skill assign 1 complextalents:thunder_strike 3 (level 3)
 * - /skill assign 1 complextalents:thunder_strike 5 (max level)
 */
public class ExampleThunderStrike {

    public static final String ID = "complextalents:thunder_strike";

    /**
     * Register this example skill.
     * Call this during mod initialization or from a skill tree mod.
     */
    public static void register() {
        SkillBuilder.create("complextalents", "thunder_strike")
                .nature(com.complextalents.skill.SkillNature.ACTIVE)
                .targeting(TargetType.ENTITY)
                .maxRange(32.0)
                .activeCooldown(6.0)
                .maxChannelTime(0.5)
                .minChannelTime(0.5)
                .allowSelfTarget(true)
                .resourceCost(25.0, "mana")
                .setMaxLevel(5)
                .scaledStat("damage", new double[]{5.0, 8.0, 12.0, 18.0, 25.0})
                .scaledStat("knockback", new double[]{0.5, 0.7, 0.9, 1.2, 1.5})
                .scaledStat("radius", new double[]{1, 1, 2, 2, 3})
                .onActive((context, rawPlayer) -> {
                    var player = context.player().getAs(net.minecraft.server.level.ServerPlayer.class);
                    var targetData = context.target().getAs(com.complextalents.skill.event.ResolvedTargetData.class);
                    ServerLevel level = player.serverLevel();

                    // Get scaled stats based on player's skill level
                    double damage = context.getStat("damage");
                    double knockbackStrength = context.getStat("knockback");
                    double chainRadius = context.getStat("radius");

                    // Get the targeted entity
                    Entity targetEntity = targetData.getTargetEntity();

                    if (targetEntity == null) {
                        // No entity targeted - should not happen with ENTITY targeting type
                        // but we handle it gracefully
                        return;
                    }

                    // Store if entity was alive before strike (for feedback)
                    boolean wasAlive = targetEntity.isAlive();
                    String entityName = targetEntity.getName().getString();

                    // Summon lightning bolt at the entity's position
                    EntityType.LIGHTNING_BOLT.spawn(level, targetEntity.blockPosition(), null);

                    // Play thunder sound at player location for dramatic effect
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.8f);

                    // Play impact sound at target location
                    level.playSound(null, targetEntity.getX(), targetEntity.getY(), targetEntity.getZ(),
                            SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 1.0f, 1.0f);

                    // Apply additional damage if target is a living entity
                    if (targetEntity instanceof LivingEntity livingTarget) {
                        // Deal damage based on skill level
                        livingTarget.hurt(level.damageSources().magic(), (float) damage);

                        // Knockback effect based on skill level
                        Vec3 knockback = player.position().subtract(targetEntity.position()).normalize()
                                .scale(-knockbackStrength);
                        livingTarget.knockback((float) knockbackStrength, knockback.x, knockback.z);

                        // Chain lightning to nearby entities at higher levels
                        if (chainRadius > 1) {
                            for (Entity nearby : level.getEntitiesOfClass(LivingEntity.class,
                                    livingTarget.getBoundingBox().inflate(chainRadius))) {
                                if (nearby != livingTarget && nearby != player) {
                                    // Create small lightning effect
                                    level.playSound(null, nearby.getX(), nearby.getY(), nearby.getZ(),
                                            SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 0.5f, 1.5f);
                                    nearby.hurt(level.damageSources().magic(), (float) (damage * 0.5));
                                }
                            }
                        }
                    }

                    // Feedback message with level info
                    int skillLevel = context.skillLevel();
                    if (wasAlive) {
                        player.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal(
                                        "\u00A7b[Thunder Strike Lvl " + skillLevel + "] " +
                                                "\u00A7eStruck " + entityName + " for " +
                                                String.format("%.1f", damage) + " damage!"
                                ),
                                true
                        );
                    }
                })
                .register();
    }
}
