package com.complextalents.impl.yygm.skill;

import com.complextalents.TalentsMod;
import com.complextalents.impl.yygm.EquilibriumData;
import com.complextalents.impl.yygm.effect.ExposedEffect;
import com.complextalents.impl.yygm.effect.HarmonizedEffect;
import com.complextalents.impl.yygm.effect.YinYangEffects;
import com.complextalents.impl.yygm.origin.YinYangGrandmasterOrigin;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.yygm.SpawnYinYangGateFXPacket;
import com.complextalents.skill.BuiltSkill;
import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.SkillRegistry;
import com.complextalents.skill.event.ResolvedTargetData;
import com.complextalents.targeting.TargetType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

/**
 * Eight Formation Battle Array - The Ultimate ability for Yin Yang Grandmaster.
 * <p>
 * Requires 8 stacks of Equilibrium to cast (does not consume them).
 * Applies the Exposed effect to a target with all 8 gates active.
 * Gates do not respawn. Completing all 8 gates converts to Yin Yang Annihilation.
 * </p>
 */
public class EightFormationBattleArraySkill {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "eight_formation_battle_array");

    // Duration by level: [15, 20, 25, 30] seconds
    private static final double[] DURATION_BY_LEVEL = {15.0, 20.0, 25.0, 30.0};
    // Cooldown by level: [180, 160, 140, 120, 100] seconds (3 min to 1 min 40 sec)
    private static final double[] COOLDOWN_BY_LEVEL = {300.0, 270.0, 240.0, 210.0, 180.0};
    // Max range
    public static final double MAX_RANGE = 8.0;

    /**
     * Register the Eight Formation Battle Array skill.
     */
    public static void register() {
        BuiltSkill skill = SkillBuilder.create("complextalents", "eight_formation_battle_array")
                .nature(com.complextalents.skill.SkillNature.ACTIVE)
                .targeting(TargetType.ENTITY)
                .maxRange(MAX_RANGE)
                .scaledCooldown(COOLDOWN_BY_LEVEL)
                // .validate(EightFormationBattleArraySkill::validateCanCast)
                .onActive(EightFormationBattleArraySkill::execute)
                .build();

        SkillRegistry.getInstance().register(skill);
        TalentsMod.LOGGER.info("Eight Formation Battle Array skill registered");
    }

    /**
     * Validate if the player can cast the Ultimate.
     */
    private static boolean validateCanCast(Skill.ExecutionContext context, Object playerObj) {
        if (!(playerObj instanceof ServerPlayer player)) {
            return false;
        }

        // Must be YYGM
        if (!YinYangGrandmasterOrigin.isYinYangGrandmaster(player)) {
            player.sendSystemMessage(Component.literal("Only Yin Yang Grandmasters can use this ability!"));
            return false;
        }

        // Must have 8 stacks of Equilibrium (does NOT consume)
        int equilibrium = EquilibriumData.getEquilibrium(player.getUUID());
        if (equilibrium < EquilibriumData.MAX_EQUILIBRIUM) {
            player.sendSystemMessage(Component.literal("Requires 8 stacks of Equilibrium! (Currently: " + equilibrium + ")"));
            return false;
        }

        // Cannot cast if already have an Exposed target
        if (ExposedEffect.hasExposedTarget(player.getUUID())) {
            player.sendSystemMessage(Component.literal("Already have an Exposed target!"));
            return false;
        }

        // Must have a valid target
        ResolvedTargetData targetData = context.target().getAs(ResolvedTargetData.class);
        if (targetData == null || !targetData.hasEntity()) {
            player.sendSystemMessage(Component.literal("Must target a living entity!"));
            return false;
        }

        Entity targetEntity = targetData.getTargetEntity();
        if (!(targetEntity instanceof LivingEntity)) {
            player.sendSystemMessage(Component.literal("Must target a living entity!"));
            return false;
        }

        return true;
    }

    /**
     * Execute the Ultimate ability.
     */
    private static void execute(Skill.ExecutionContext context, Object playerObj) {
        ServerPlayer player = (ServerPlayer) playerObj;
        ResolvedTargetData targetData = context.target().getAs(ResolvedTargetData.class);

        if (targetData == null || !targetData.hasEntity()) {
            return;
        }

        Entity targetEntity = targetData.getTargetEntity();
        if (!(targetEntity instanceof LivingEntity target)) {
            return;
        }

        // Get skill level for duration scaling
        int skillLevel = context.skillLevel();
        int durationSeconds = getDuration(skillLevel);

        // Step 1: Remove Harmonized effect if present on this target
        Integer harmonizedId = HarmonizedEffect.getHarmonizedEntityId(player.getUUID());
        if (harmonizedId != null && harmonizedId == target.getId()) {
            HarmonizedEffect.clearHarmonizedTracking(player.getUUID());
            target.removeEffect(com.complextalents.impl.yygm.effect.YinYangEffects.HARMONIZED.get());
            HarmonizedEffect.cleanupPlayerData(target, player.getUUID());
            TalentsMod.LOGGER.debug("Eight Formation: Removed Harmonized from target {}",
                target.getName().getString());
        }

        // Step 2: Apply Exposed effect via instance method
        int durationTicks = durationSeconds * 20; // Convert to ticks
        ExposedEffect effect = (ExposedEffect) YinYangEffects.EXPOSED.get();
        effect.applyToTarget(target, player.getUUID(), durationTicks);

        // Step 3: Play FX
        if (player.level() instanceof ServerLevel level) {
            // Play activation sound
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ELDER_GUARDIAN_CURSE, player.getSoundSource(), 1.0f, 1.0f);

            // Send visual FX - use GATE_SPAWN effect for now
            PacketHandler.sendToNearby(
                new SpawnYinYangGateFXPacket(target.position(), SpawnYinYangGateFXPacket.EffectType.GATE_SPAWN),
                level, target.position()
            );

            // Send feedback message
            player.sendSystemMessage(Component.literal("Eight Formation Battle Array activated! " +
                "Complete all 8 gates to trigger Yin Yang Annihilation!"));
        }

        TalentsMod.LOGGER.info("Eight Formation Battle Array cast by {} on {}, duration: {}s",
            player.getName().getString(), target.getName().getString(), durationSeconds);
    }

    /**
     * Get the duration in seconds for a given skill level.
     */
    public static int getDuration(int level) {
        int index = Math.min(Math.max(level - 1, 0), DURATION_BY_LEVEL.length - 1);
        return (int) DURATION_BY_LEVEL[index];
    }

    /**
     * Get the cooldown in seconds for a given skill level.
     */
    public static double getCooldown(int level) {
        int index = Math.min(Math.max(level - 1, 0), COOLDOWN_BY_LEVEL.length - 1);
        return COOLDOWN_BY_LEVEL[index];
    }
}
