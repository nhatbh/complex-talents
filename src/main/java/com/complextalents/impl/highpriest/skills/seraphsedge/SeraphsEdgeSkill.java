package com.complextalents.impl.highpriest.skills.seraphsedge;

import com.complextalents.impl.highpriest.entity.SeraphsBouncingSwordEntity;
import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.SkillNature;
import com.complextalents.skill.event.ResolvedTargetData;
import com.complextalents.targeting.TargetType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Seraph's Edge - A spectral sword that patrols the battlefield, shielding friends and smiting foes.
 * <p>
 * The sword bounces between targets up to 3/4/5/6 times based on skill level. It prefers enemies
 * but can heal/shield allies. Cannot hit the same ally twice in a row without hitting an enemy in between.
 * <p>
 * <strong>Properties:</strong>
 * <ul>
 *   <li>Resource: Piety (no cost for testing)</li>
 *   <li>Cooldown: 5 seconds</li>
 *   <li>Channel Time: 1 second</li>
 *   <li>Range: 32 blocks</li>
 *   <li>Max Level: 4</li>
 *   <li>Max Hits: 3/4/5/6 (by level)</li>
 *   <li>Damage per hit: 8/10/12/15 (by level)</li>
 *   <li>Heal per ally hit: 4/6/8/10 (by level)</li>
 *   <li>Damage Falloff: 15% per bounce</li>
 *   <li>Search Radius: 10 blocks</li>
 * </ul>
 *
 * @see SeraphsBouncingSwordEntity
 */
public class SeraphsEdgeSkill {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "seraphs_edge");

    /**
     * Register this skill.
     */
    public static void register() {
        SkillBuilder.create("complextalents", "seraphs_edge")
                .nature(SkillNature.ACTIVE)
                .targeting(TargetType.ENTITY)
                .icon(ResourceLocation.fromNamespaceAndPath("complextalents", "textures/skill/highpriest/seraphs_edge.png"))
                .maxRange(32.0)
                .minChannelTime(1.0)  // 1 second channel
                .maxChannelTime(1.0)  // Fixed 1 second
                .activeCooldown(5.0)  // 5 second cooldown
                .resourceCost(0.0, "complextalents:piety")  // No cost for testing
                .setMaxLevel(4)
                .scaledStat("hits", new double[]{3, 4, 5, 6})      // Max bounces per level
                .scaledStat("damage", new double[]{8, 10, 12, 15}) // Damage per hit
                .scaledStat("heal", new double[]{4, 6, 8, 10})     // Heal per ally hit
                .scaledStat("range", new double[]{10.0, 10.0, 10.0, 10.0}) // Search radius
                .onActive((context, rawPlayer) -> {
                    ServerPlayer player = (ServerPlayer) rawPlayer;
                    ResolvedTargetData targetData = context.target().getAs(ResolvedTargetData.class);

                    // Get scaled stats based on skill level
                    int maxBounces = (int) context.getStat("hits");
                    double baseDamage = context.getStat("damage");
                    double healAmount = context.getStat("heal");
                    double searchRadius = context.getStat("range");

                    // Create bouncing sword entity with owner
                    SeraphsBouncingSwordEntity sword = new SeraphsBouncingSwordEntity(
                            player.level(),
                            player
                    );

                    sword.setOwner(player);

                    // Configure bouncing behavior
                    sword.configureBouncing(maxBounces, (float) baseDamage, (float) healAmount, searchRadius);

                    // Set homing target if available
                    if (targetData != null && targetData.hasEntity() &&
                        targetData.getTargetEntity() instanceof LivingEntity livingTarget) {
                        sword.setTarget(livingTarget);
                    }

                    // Calculate initial direction from player's look
                    Vec3 aimDir = targetData != null
                        ? targetData.getAimDirection()
                        : player.getLookAngle();

                    // Shoot with calculated direction
                    sword.shoot(aimDir.x, aimDir.y, aimDir.z, 0.2f, 0.0f);

                    // Spawn 2.5 blocks in front of player's eye position
                    Vec3 eyePos = player.getEyePosition(1.0f);
                    Vec3 spawnPos = eyePos.add(aimDir.scale(2.5));
                    sword.setPos(spawnPos.x, spawnPos.y - 0.2, spawnPos.z);

                    // Add entity to world
                    player.level().addFreshEntity(sword);

                    // Play sound
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 1.0f);
                })
                .register();
    }
}
