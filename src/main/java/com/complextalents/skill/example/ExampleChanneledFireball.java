package com.complextalents.skill.example;

import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.event.SkillExecuteEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Example CHANNELED skill: Charged Fireball
 * - Hold the skill key to charge (0.5s min, 3.0s max)
 * - Release to fire a larger fireball based on charge time
 * - Damage and explosion scale with channel time (1x to 3x)
 *
 * Usage: /skill assign 1 complextalents:charged_fireball
 */
@Mod.EventBusSubscriber(modid = "complextalents")
public class ExampleChanneledFireball {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "charged_fireball");

    /**
     * Register this example skill.
     */
    public static void register() {
        SkillBuilder.create("complextalents", "charged_fireball")
                .nature(com.complextalents.skill.SkillNature.ACTIVE)
                .targeting(com.complextalents.skill.TargetingType.DIRECTION)
                .maxRange(32.0)
                .minChannelTime(0.5)  // Min 0.5 seconds (client enforces)
                .maxChannelTime(3.0)  // Max 3 seconds (client enforces)
                .activeCooldown(5.0)
                .resourceCost(30.0, "mana")
                .register();
    }

    /**
     * Channeled skill handler: Fires a larger fireball based on channel time.
     */
    @SubscribeEvent
    public static void onChanneledSkillExecute(SkillExecuteEvent event) {
        // Only handle this skill
        if (!event.getSkillId().equals(ID)) {
            return;
        }

        var player = event.getPlayer();
        var targetData = event.getTargetData();
        double channelTime = event.getChannelTime();

        // Spawn fireball at player eye position, in casting direction
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 dir = targetData.getAimDirection();
        Vec3 spawnPos = eyePos.add(dir.scale(1.5)); // 1.5 blocks in front to avoid collision

        // Scale damage and size by channel time (1x to 3x)
        double scale = 1.0 + (channelTime / 3.0) * 2.0;

        LargeFireball fireball = new LargeFireball(
                player.level(),
                player,
                dir.x * scale * 0.5,
                dir.y * scale * 0.5,
                dir.z * scale * 0.5,
                (int) (4 + scale * 4) // Explosion power: 4 to 16
        );
        fireball.setPos(spawnPos);
        player.level().addFreshEntity(fireball);

        // Play sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f + (float) scale * 0.2f, 1.0f);

        // Feedback message
        player.sendSystemMessage(Component.literal(String.format(
                "\u00A7e[Charged Fireball] Scale: \u00A7c%.1fx\u00A7e | Power: \u00A76%d",
                scale, (int) (4 + scale * 4)
        )), true);
    }
}
