package com.complextalents.impl.assassin.skill;

import com.complextalents.impl.assassin.data.AssassinData;
import com.complextalents.impl.assassin.effect.AssassinEffects;
import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.SkillNature;
import com.complextalents.targeting.TargetType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Shadow Walk - Enter infinite stealth after 1s channel.
 */
public class ShadowWalkSkill {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "shadow_walk");

    public static void register() {
        SkillBuilder.create("complextalents", "shadow_walk")
                .nature(SkillNature.ACTIVE)
                .description("Phase into shadows for up to 60 seconds. Grants increased movement speed and a massive damage bonus (up to +100%) on your next backstab.")
                .targeting(TargetType.NONE)
                .icon(ResourceLocation.fromNamespaceAndPath("complextalents",
                        "textures/skill/assassin/shadow_walk.png"))
                .minChannelTime(1.0)
                .maxChannelTime(1.0)
                .scaledCooldown(new double[] { 9999.0, 9999.0, 9999.0, 9999.0, 9999.0 })
                .scaledStat("stealthMoveSpeed", new double[] { 0.35, 0.40, 0.45, 0.50, 0.60 })
                .scaledStat("stealthGaugeSize", new double[] { 100.0, 150.0, 200.0, 250.0, 300.0 })
                .scaledStat("stealthGaugeRecovery", new double[] { 5.0, 7.0, 10.0, 15.0, 20.0 })
                .scaledStat("stealthBackstabBuff", new double[] { 0.40, 0.50, 0.60, 0.70, 1.0 })
                .scaledStat("stealthBuffDuration", new double[] { 5.0, 6.0, 7.0, 8.0, 10.0 })
                .scaledStat("stealthDamagePenalty", new double[] { 3.0, 3.0, 3.0, 3.0, 3.0 })
                .scaledStat("visibilityReduction", new double[] { 0.1, 0.08, 0.06, 0.04, 0.02 }) // Lower is better
                .setMaxLevel(5)
                .onActive((context, player) -> {
                    if (!(player instanceof ServerPlayer serverPlayer))
                        return;

                    ServerLevel level = serverPlayer.serverLevel();

                    // Ensure gauge is at least 20% of max when starting
                    double currentGauge = AssassinData.getStealthGauge(serverPlayer);
                    double maxGauge = AssassinData.getMaxGauge(serverPlayer);
                    if (currentGauge < maxGauge * 0.2) {
                        AssassinData.setStealthGauge(serverPlayer, maxGauge * 0.2);
                    }

                    // Apply Shadow Walk effect
                    serverPlayer.addEffect(new MobEffectInstance(
                            AssassinEffects.SHADOW_WALK.get(),
                            72000, // 1 hour (Effective infinite)
                            context.skillLevel() - 1,
                            false,
                            false));

                    // Play stealth sound and particles
                    level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.5f);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                            serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(),
                            25, 0.3, 0.5, 0.3, 0.05);

                    serverPlayer.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("\u00A78Entering Shadow Walk..."));
                })
                .register();
    }
}
