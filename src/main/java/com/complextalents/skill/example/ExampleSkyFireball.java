package com.complextalents.skill.example;

import com.complextalents.skill.SkillBuilder;
import com.complextalents.targeting.TargetType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Example ACTIVE skill: Sky Fireball
 * - Targeting: POSITION (ground-targeted)
 * - Cooldown: 8 seconds
 * - Resource: 40 mana
 * - Effect: Summons a fireball high above the target position that crashes down
 *
 * Usage: /skill assign 1 complextalents:sky_fireball
 */
public class ExampleSkyFireball {

    public static final String ID = "complextalents:sky_fireball";

    /**
     * Register this example skill.
     * Call this during mod initialization or from a skill tree mod.
     */
    public static void register() {
        SkillBuilder.create("complextalents", "sky_fireball")
                .nature(com.complextalents.skill.SkillNature.ACTIVE)
                .targeting(TargetType.POSITION)
                .maxRange(48.0)
                .activeCooldown(8.0)
                .maxChannelTime(0.5)
                .minChannelTime(0.5)
                .resourceCost(40.0, "mana")
                .onActive((context, rawPlayer) -> {
                    var player = context.player().getAs(net.minecraft.server.level.ServerPlayer.class);
                    var targetData = context.target().getAs(com.complextalents.skill.event.ResolvedTargetData.class);
                    Level level = player.level();

                    // Get the target position
                    Vec3 targetPos = targetData.getTargetPosition();

                    // Spawn height for the fireball (20 blocks above target)
                    double spawnHeight = 20.0;
                    Vec3 skyPos = new Vec3(targetPos.x, targetPos.y + spawnHeight, targetPos.z);

                    // Calculate direction downward toward target
                    Vec3 direction = targetPos.subtract(skyPos).normalize();
                    double velocity = 0.8; // Speed of descent

                    // Create the fireball
                    LargeFireball fireball = new LargeFireball(
                            level,
                            player,
                            direction.x * velocity,
                            direction.y * velocity,
                            direction.z * velocity,
                            3 // Explosion power
                    );
                    fireball.setPos(skyPos);
                    level.addFreshEntity(fireball);

                    // Visual warning particles at target location (a ring on the ground)
                    if (level instanceof ServerLevel serverLevel) {
                        // Create a warning ring on the ground
                        for (int i = 0; i < 360; i += 15) {
                            double angle = Math.toRadians(i);
                            double radius = 2.5;
                            double px = targetPos.x + Math.cos(angle) * radius;
                            double pz = targetPos.z + Math.sin(angle) * radius;
                            serverLevel.sendParticles(
                                    player,
                                    ParticleTypes.FLAME,
                                    true,
                                    px, targetPos.y, pz,
                                    0, 0, 0.1, 0, 1
                            );
                        }

                        // Spawn some flame particles rising up
                        for (int j = 0; j < 5; j++) {
                            double offsetX = (level.getRandom().nextDouble() - 0.5) * 4;
                            double offsetZ = (level.getRandom().nextDouble() - 0.5) * 4;
                            serverLevel.sendParticles(
                                    player,
                                    ParticleTypes.FLAME,
                                    true,
                                    targetPos.x + offsetX, targetPos.y, targetPos.z + offsetZ,
                                    0, 0, 0.3, 0, 3
                            );
                        }
                    }

                    // Play charging sound at player location
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EVOKER_PREPARE_WOLOLO, SoundSource.PLAYERS, 1.0f, 1.2f);
                })
                .register();
    }
}
