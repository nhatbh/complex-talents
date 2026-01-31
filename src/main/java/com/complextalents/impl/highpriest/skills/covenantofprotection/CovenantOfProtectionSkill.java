package com.complextalents.impl.highpriest.skills.covenantofprotection;

import com.complextalents.impl.highpriest.effect.CovenantProtectionEffect;
import com.complextalents.impl.highpriest.effect.HighPriestEffects;
import com.complextalents.network.ActivateBeamPacket;
import com.complextalents.network.DeactivateBeamPacket;
import com.complextalents.network.PacketHandler;
import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.SkillNature;
import com.complextalents.skill.event.ResolvedTargetData;
import com.complextalents.targeting.TargetType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * Covenant of Protection - A sacred vow that allows the Priest to mitigate target damage.
 * <p>
 * Toggle skill: Creates a spiritual bond (golden tether) between the Priest and an target.
 * While linked:
 * <ul>
 *   <li>The target receives reduced damage (30/35/40/50% by level)</li>
 *   <li>Mitigated damage converts to Piety drain on caster</li>
 *   <li>Link breaks if out of range, Piety runs out, or manutarget toggled off</li>
 * </ul>
 * <p>
 * <strong>Properties:</strong>
 * <ul>
 *   <li>Resource: Piety (15 initial cost)</li>
 *   <li>Cooldown: 20 seconds</li>
 *   <li>Range: 15/20/25/30 blocks (by level)</li>
 *   <li>Max Level: 4</li>
 *   <li>Duration: 10/15/20/25 seconds (by level)</li>
 *   <li>Damage Reduction: 30/35/40/50% (by level)</li>
 *   <li>Piety per Damage: 100%/85%/70%/50% (1 damage = 1/rate Piety)</li>
 * </ul>
 */
public class CovenantOfProtectionSkill {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "covenant_of_protection");

    // Target UUID storage key for caster's NBT
    private static final String NBT_TARGET_UUID = "CovenantTargetUUID";

    /**
     * Register this skill.
     */
    public static void register() {
        SkillBuilder.create("complextalents", "covenant_of_protection")
                .nature(SkillNature.ACTIVE)
                .targeting(TargetType.ENTITY)
                .icon(ResourceLocation.fromNamespaceAndPath("complextalents", "textures/skill/highpriest/covenant_of_protection.png"))
                .maxRange(15.0)  // Max range at level 4
                .activeCooldown(20.0)  // 20 second cooldown (starts when toggle ends)
                .resourceCost(15.0, "complextalents:piety")
                .toggleable(true)  // MAKES IT A TOGGLE SKILL
                .toggleMaxDuration(25.0)  // Max duration matches level 4 duration
                .setMaxLevel(4)
                // Duration: 10/15/20/25 seconds
                .scaledStat("duration", new double[]{10.0, 15.0, 20.0, 25.0})
                // Range: 15/20/25/30 blocks
                .scaledStat("range", new double[]{15.0, 20.0, 25.0, 30.0})
                // Damage reduction: 30/35/40/50%
                .scaledStat("reduction", new double[]{0.30, 0.35, 0.40, 0.50})
                // Piety drain rate: 1.0/0.85/0.70/0.50 (higher divisor = less piety per damage)
                .scaledStat("pietyRate", new double[]{1.0, 0.85, 0.70, 0.50})
                .onActive((context, rawPlayer) -> {
                    ServerPlayer player = (ServerPlayer) rawPlayer;
                    ResolvedTargetData targetData = context.target().getAs(ResolvedTargetData.class);

                    // Get scaled stats based on skill level
                    double duration = context.getStat("duration");
                    double damageReduction = context.getStat("reduction");
                    double pietyDrainRate = context.getStat("pietyRate");
                    double range = context.getStat("range");

                    // Validate target exists
                    if (targetData == null || !targetData.hasEntity()) {
                        player.sendSystemMessage(Component.literal("§cYou must target an entity to create Covenant of Protection."));
                        return;
                    }

                    if (!(targetData.getTargetEntity() instanceof LivingEntity target)) {
                        player.sendSystemMessage(Component.literal("§cInvalid target."));
                        return;
                    }

                    // Check if caster already has an active covenant
                    UUID existingTargetId = getStoredTargetUUID(player);
                    if (existingTargetId != null) {
                        // Already has a covenant - check if it's the same target
                        if (existingTargetId.equals(target.getUUID()) && CovenantProtectionEffect.hasActiveCovenant(target)) {
                            player.sendSystemMessage(Component.literal("§cYou already have an active Covenant with this target."));
                        } else {
                            player.sendSystemMessage(Component.literal("§cYou already have an active Covenant link."));
                        }
                        return;
                    }

                    // Check if target already has a covenant from someone else
                    if (CovenantProtectionEffect.hasActiveCovenant(target)) {
                        UUID existingCaster = CovenantProtectionEffect.getStoredCasterUUID(target);
                        if (!player.getUUID().equals(existingCaster)) {
                            player.sendSystemMessage(Component.literal("§cThat target is already protected by another Covenant."));
                        } else {
                            player.sendSystemMessage(Component.literal("§cThat target is already protected by your Covenant."));
                        }
                        return;
                    }

                    // Check range
                    double distance = player.position().distanceTo(target.position());
                    if (distance > range) {
                        player.sendSystemMessage(Component.literal("§cTarget is out of range (max " + (int) range + " blocks)."));
                        return;
                    }

                    // Apply the effect to the target
                    int skillLevel = context.skillLevel();
                    int effectDuration = (int) (duration * 20); // Convert to ticks

                    MobEffectInstance effectInstance = new MobEffectInstance(
                            HighPriestEffects.COVENANT_PROTECTION.get(),
                            effectDuration,
                            skillLevel - 1, // Amplifier is 0-indexed
                            false, false, true
                    );

                    target.addEffect(effectInstance);

                    // Initialize effect data on the target
                    CovenantProtectionEffect.initializeEffectData(
                            target,
                            player.getUUID(),
                            range,
                            damageReduction,
                            pietyDrainRate,
                            skillLevel
                    );

                    // Store target UUID on caster
                    setStoredTargetUUID(player, target.getUUID());

                    // Send packet to create visual link (include range for client-side validation)
                    PacketHandler.sendToNearby(
                            new ActivateBeamPacket(player.getUUID(), target.getUUID(), range),
                            (ServerLevel) player.level(),
                            player.position()
                    );

                    // Play sound
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.2f);

                    // Send confirmation message
                    player.sendSystemMessage(Component.literal("§eCovenant of Protection established! " +
                            String.format("Mitigates %.0f%% damage for %s.", damageReduction * 100, target.getName().getString())));

                    // Only send message to target if it's a player
                    if (target instanceof ServerPlayer) {
                        target.sendSystemMessage(Component.literal("§e" + player.getName().getString() +
                                " has established a Covenant of Protection with you!"));
                    }
                })
                .onToggleOff(rawPlayer -> {
                    // Called when toggle is turned off (manually, by max duration, or by event)
                    ServerPlayer caster = (ServerPlayer) rawPlayer;
                    deactivateCovenant(caster);
                })
                .register();
    }

    // ========== NBT Storage Methods ==========

    /**
     * Store the target entity UUID on the caster.
     */
    public static void setStoredTargetUUID(ServerPlayer caster, UUID targetUUID) {
        caster.getPersistentData().putUUID(NBT_TARGET_UUID, targetUUID);
    }

    /**
     * Get the stored target UUID from the caster.
     */
    public static UUID getStoredTargetUUID(ServerPlayer caster) {
        net.minecraft.nbt.CompoundTag data = caster.getPersistentData();
        if (data.hasUUID(NBT_TARGET_UUID)) {
            return data.getUUID(NBT_TARGET_UUID);
        }
        return null;
    }

    /**
     * Clear the stored target UUID from the caster.
     */
    public static void clearStoredTargetUUID(ServerPlayer caster) {
        caster.getPersistentData().remove(NBT_TARGET_UUID);
    }

    /**
     * Manually deactivate the covenant (toggle off).
     * Called when player manually deactivates the skill.
     */
    public static void deactivateCovenant(ServerPlayer caster) {
        UUID targetId = getStoredTargetUUID(caster);
        if (targetId == null) {
            return;
        }

        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }

        // Find the target entity (may be a player or any LivingEntity)
        LivingEntity target = null;
        ServerPlayer playerTarget = level.getServer().getPlayerList().getPlayer(targetId);
        if (playerTarget != null) {
            target = playerTarget;
        } else {
            // Search for entities in the level
            for (var entity : level.getAllEntities()) {
                if (entity.getUUID().equals(targetId) && entity instanceof LivingEntity living) {
                    target = living;
                    break;
                }
            }
        }

        if (target != null && CovenantProtectionEffect.hasActiveCovenant(target)) {
            // Remove effect - this will trigger the removal event
            target.removeEffect(HighPriestEffects.COVENANT_PROTECTION.get());
        }

        clearStoredTargetUUID(caster);
    }
}
