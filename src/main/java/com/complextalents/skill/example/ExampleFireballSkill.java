package com.complextalents.skill.example;

import com.complextalents.skill.SkillBuilder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

/**
 * Example ACTIVE skill: Fireball
 * - Targeting: ENTITY (with fallback to self)
 * - Cooldown: 3 seconds
 * - Resource: 20 mana (placeholder)
 *
 * Usage: /skill assign 1 complextalents:fireball
 */
public class ExampleFireballSkill {

    public static final String ID = "complextalents:fireball";

    /**
     * Register this example skill.
     * Call this during mod initialization or from a skill tree mod.
     */
    public static void register() {
        SkillBuilder.create("complextalents", "fireball")
                .nature(com.complextalents.skill.SkillNature.ACTIVE)
                .targeting(com.complextalents.skill.TargetingType.DIRECTION)
                .maxRange(32.0)
                .activeCooldown(3.0)
                .resourceCost(20.0, "mana")
                .onActive((context, rawPlayer) -> {
                    // Extract player and target from context
                    var player = context.player().getAs(net.minecraft.server.level.ServerPlayer.class);
                    var targetData = context.target().getAs(com.complextalents.skill.event.ResolvedTargetData.class);

                    // Spawn fireball in front of player in casting direction
                    Vec3 eyePos = player.getEyePosition(1.0f);
                    Vec3 dir = targetData.getAimDirection();
                    Vec3 spawnPos = eyePos.add(dir.scale(1.5)); // 1.5 blocks in front to avoid player collision

                    SmallFireball fireball = new SmallFireball(
                            player.level(),
                            player,
                            dir.x * 0.5, dir.y * 0.5, dir.z * 0.5
                    );
                    fireball.setPos(spawnPos);
                    player.level().addFreshEntity(fireball);

                    // Play sound
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                })
                .register();
    }
}
