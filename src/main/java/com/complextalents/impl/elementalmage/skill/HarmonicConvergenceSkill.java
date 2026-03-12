package com.complextalents.impl.elementalmage.skill;

import com.complextalents.elemental.registry.ReactionRegistry;
import com.complextalents.impl.elementalmage.ElementalMageData;
import com.complextalents.passive.PassiveManager;
import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.SkillNature;
import com.complextalents.targeting.TargetType;

import io.redspace.ironsspellbooks.network.ClientboundSyncMana;
import io.redspace.ironsspellbooks.setup.Messages;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class HarmonicConvergenceSkill {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents",
            "harmonic_convergence");

    // Level Scaling Arrays
    private static final double[] MANA_BASE = { 10.0, 15.0, 20.0, 25.0, 40.0 };
    private static final double[] MANA_MULT = { 5.0, 8.0, 12.0, 16.0, 25.0 };
    private static final double[] CRIT_PER_STACK = { 0.10, 0.12, 0.15, 0.17, 0.20 };
    private static final double[] CD_BASE = { 0.25, 0.30, 0.35, 0.40, 0.50 };
    private static final double[] CD_MULT = { 0.15, 0.20, 0.25, 0.30, 0.40 };

    public static void register() {
        SkillBuilder.create("complextalents", "harmonic_convergence")
                .nature(SkillNature.ACTIVE)
                .targeting(TargetType.NONE)
                .icon(ResourceLocation.fromNamespaceAndPath("complextalents",
                        "textures/skill/elementalmage/harmonic_convergence.png"))
                .setMaxLevel(5)
                .scaledCooldown(new double[] { 10.0, 10.0, 10.0, 10.0, 10.0 })
                .validate((context, player) -> {
                    ServerPlayer serverPlayer = (ServerPlayer) player;
                    int echoes = PassiveManager.getPassiveStacks(serverPlayer, "resonance_echo");
                    if (echoes <= 0) {
                        serverPlayer.sendSystemMessage(
                                Component.literal(
                                        "\u00A7cYou have no Resonance Echoes to converge!"));
                        return false;
                    }
                    return true;
                })
                .onActive((context, player) -> {
                    ServerPlayer serverPlayer = (ServerPlayer) player;
                    int level = Math.min(Math.max(context.skillLevel() - 1, 0), 4);

                    // 1. Consume Stacks
                    int echoes = PassiveManager.getPassiveStacks(serverPlayer, "resonance_echo");
                    PassiveManager.setPassiveStacks(serverPlayer, "resonance_echo", 0);

                    // 2. Fetch Stats
                    double em = ReactionRegistry.getInstance()
                            .calculateElementalMastery(serverPlayer);
                    double bonusEm = Math.max(0.0, em - 1.0); // "scale with elemental master - 1"

                    // 3. Calculate Mana Restore
                    double manaRestored = echoes * (MANA_BASE[level] + (em * MANA_MULT[level]));

                    // 4. Calculate Critical Hit Stats for Next Spell
                    double critChance = echoes * CRIT_PER_STACK[level];
                    double critDamageBonus = CD_BASE[level] + (bonusEm * CD_MULT[level]);

                    // Apply Mana Recovery
                    try {
                        io.redspace.ironsspellbooks.api.magic.MagicData magicData = io.redspace.ironsspellbooks.api.magic.MagicData
                                .getPlayerMagicData(serverPlayer);
                        double maxMana = serverPlayer.getAttributeValue(
                                io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA
                                        .get());
                        magicData.setMana((float) Math.min(maxMana,
                                magicData.getMana() + manaRestored));
                        Messages.sendToPlayer(new ClientboundSyncMana(magicData), serverPlayer);
                    } catch (Exception e) {
                        // Iron's Spellbooks not loaded or error
                    }

                    // Cache for the Next Spell
                    ElementalMageData.ConvergenceBuff buff = ElementalMageData
                            .getConvergenceBuff(serverPlayer.getUUID());
                    buff.waitingForNextSpell = true;
                    buff.cachedCritChanceOffset = critChance;
                    buff.cachedCritDamageBonus = critDamageBonus;
                    buff.buffWindowTicks = 0;
                    buff.buffedSpellId = null;

                    // Play SFX & Particles
                    ServerLevel sLevel = serverPlayer.serverLevel();
                    sLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(),
                            serverPlayer.getZ(),
                            SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.7f, 1.5f);

                    sLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                            serverPlayer.getX(),
                            serverPlayer.getY() + serverPlayer.getBbHeight() / 2.0,
                            serverPlayer.getZ(),
                            50, 0.5, 0.5, 0.5, 0.2);

                    serverPlayer.sendSystemMessage(Component.literal(String.format(
                            "\u00A7b\u2728 Harmonic Convergence! \u00A7fRestored \u00A7b%.0f Mana\u00A7f. Your next spell gains \u00A7e+%.0f%% Crit Chance \u00A7fand \u00A7c+%.0f%% Crit Damage\u00A7f!",
                            manaRestored, critChance * 100.0, critDamageBonus * 100.0)));
                })
                .register();
    }
}
