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
 * Example ACTIVE skill: Thunder Strike
 * - Targeting: ENTITY (must target a living entity)
 * - Cooldown: 6 seconds
 * - Resource: 25 mana
 * - Effect: Summons a lightning bolt to strike the targeted entity
 *
 * Usage: /skill assign 1 complextalents:thunder_strike
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
                .onActive((context, rawPlayer) -> {
                    var player = context.player().getAs(net.minecraft.server.level.ServerPlayer.class);
                    var targetData = context.target().getAs(com.complextalents.skill.event.ResolvedTargetData.class);
                    ServerLevel level = player.serverLevel();

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
                    // (Lightning bolt does damage, but we add extra for the skill)
                    if (targetEntity instanceof LivingEntity livingTarget) {
                        // Deal 5 magic damage (ignores some armor)
                        livingTarget.hurt(level.damageSources().magic(), 5.0f);

                        // Knockback effect
                        Vec3 knockback = player.position().subtract(targetEntity.position()).normalize()
                                .scale(-0.5); // Push away from caster
                        livingTarget.knockback(0.5f, knockback.x, knockback.z);
                    }

                    // Feedback message
                    if (wasAlive) {
                        player.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal(
                                        "\u00A7b[Thunder Strike] " + "\u00A7eStruck " + entityName
                                                + " with lightning!"
                                ),
                                true
                        );
                    }
                })
                .register();
    }
}
