package com.complextalents.impl.highpriest.skills.divineascendance;

import com.complextalents.impl.highpriest.effect.DivineAscendanceAuraHandler;
import com.complextalents.impl.highpriest.effect.HighPriestEffects;
import com.complextalents.origin.OriginManager;
import com.complextalents.passive.PassiveManager;
import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.SkillNature;
import com.complextalents.targeting.TargetType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Divine Ascendance - The High Priest's Ultimate AoE Buff.
 * <p>
 * Transforms the Priest into a living conduit of divine power, granting massive
 * combat stat bonuses to all nearby allies.
 * </p>
 * <p>
 * <strong>Properties:</strong>
 * <ul>
 *   <li>Cooldown: 360/300/240/180 seconds (by level)</li>
 *   <li>Requirements: 100 Piety AND 10 Grace stacks</li>
 *   <li>Toggle Cost: 10 Piety per second (handled in aura handler)</li>
 *   <li>Radius: 50 blocks</li>
 *   <li>Duration: Sustained by kills (5 Piety restored per kill)</li>
 *   <li>Grants limited flight (5-8 blocks above ground)</li>
 *   <li>Auto-toggles off when Piety reaches 0</li>
 * </ul>
 * <p>
 * <strong>Buff Amount (scales with skill level):</strong>
 * <ul>
 *   <li>Level 1: +30% to all combat stats</li>
 *   <li>Level 2: +45% to all combat stats</li>
 *   <li>Level 3: +60% to all combat stats</li>
 *   <li>Level 4: +90% to all combat stats</li>
 * </ul>
 * </p>
 */
public class DivineAscendanceSkill {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "divine_ascendance");

    /**
     * Register this skill.
     */
    public static void register() {
        SkillBuilder.create("complextalents", "divine_ascendance")
                .nature(SkillNature.ACTIVE)
                .targeting(TargetType.NONE)
                .scaledCooldown(new double[]{360.0, 300.0, 240.0, 180.0})  // 360/300/240/180 second cooldown by level (after toggle off)
                .toggleable(true)
                .setMaxLevel(4)
                .validate((context, player) -> {
                    ServerPlayer serverPlayer = (ServerPlayer) player;

                    // Check requirements: 10 Grace stacks + 100 Piety
                    int grace = PassiveManager.getPassiveStacks(serverPlayer, "grace");
                    double piety = OriginManager.getResource(serverPlayer);

                    if (grace < 10 || piety < 100) {
                        serverPlayer.sendSystemMessage(Component.literal(
                                "\u00A7cDivine Ascendance requires 10 Grace stacks and 100 Piety!"
                        ));
                        return false;
                    }

                    return true;
                })
                .onActive((context, player) -> {
                    ServerPlayer serverPlayer = (ServerPlayer) player;
                    ServerLevel level = serverPlayer.serverLevel();

                    // Get skill level for effect amplifier
                    int skillLevel = context.skillLevel();
                    int amplifier = Math.max(0, skillLevel - 1);

                    // Apply flight effect ONLY to the caster (Priest)
                    serverPlayer.addEffect(new MobEffectInstance(
                            HighPriestEffects.DIVINE_ASCENDANCE_FLIGHT.get(),
                            600,  // 30 seconds, will be refreshed by toggle
                            0,
                            false,
                            false  // No particles - cleaner look
                    ));

                    // Also apply the buff effect to the caster (they get buffs too)
                    serverPlayer.addEffect(new MobEffectInstance(
                            HighPriestEffects.DIVINE_ASCENDANCE.get(),
                            100,  // 5 seconds, refreshed by aura handler
                            amplifier,
                            false,
                            true
                    ));

                    // Visual activation effects
                    level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 1.5f, 1.0f);
                    level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 0.8f);

                    // Feedback message
                    serverPlayer.sendSystemMessage(Component.literal(
                            "\u00A76\u00A7lDIVINE ASCENDANCE ACTIVATED!\u00A7r " +
                                    "\u00A7eYou and your allies are now imbued with divine power!"
                    ));
                })
                .onToggleOff(player -> {
                    ServerPlayer serverPlayer = (ServerPlayer) player;

                    // Remove flight effect from caster
                    serverPlayer.removeEffect(HighPriestEffects.DIVINE_ASCENDANCE_FLIGHT.get());

                    // Remove buff effect from caster
                    serverPlayer.removeEffect(HighPriestEffects.DIVINE_ASCENDANCE.get());

                    // Remove buff from all affected allies
                    DivineAscendanceAuraHandler.removeBuffFromAllPlayers(serverPlayer);

                    // Play deactivation sound
                    ServerLevel level = serverPlayer.serverLevel();
                    level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0f, 1.0f);

                    serverPlayer.sendSystemMessage(Component.literal(
                            "\u00A7cDivine Ascendance has ended."
                    ));
                })
                .register();
    }
}
