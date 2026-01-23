package com.complextalents.elemental.superreaction;

import com.complextalents.TalentsMod;
import com.complextalents.capability.TalentsCapabilities;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalStackManager;
import com.complextalents.elemental.ParticleHelper;
import com.complextalents.elemental.attributes.MasteryAttributes;
import com.complextalents.elemental.superreaction.reactions.*;
import com.complextalents.elemental.talents.mage.attunement.ElementalAttunementTalent;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.SpawnReactionTextPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;

/**
 * Handles the detection and execution of Super-Reactions
 * Super-Reactions trigger when 3-6 unique elements are applied to a target
 */
public class SuperReactionHandler {

    // Map of element to its super reaction implementation
    private static final Map<ElementType, SuperReaction> REACTIONS = new HashMap<>();

    static {
        // Register all super reactions
        REACTIONS.put(ElementType.FIRE, new FireSuperReaction());
        REACTIONS.put(ElementType.ICE, new IceSuperReaction());
        REACTIONS.put(ElementType.AQUA, new AquaSuperReaction());
        REACTIONS.put(ElementType.LIGHTNING, new LightningSuperReaction());
        REACTIONS.put(ElementType.NATURE, new NatureSuperReaction());
        REACTIONS.put(ElementType.ENDER, new EnderSuperReaction());
    }

    /**
     * Check and trigger a Super-Reaction if conditions are met
     *
     * @param caster The player causing the reaction
     * @param target The entity with element stacks
     * @param triggeringElement The element that triggered the check
     * @return true if a Super-Reaction was triggered
     */
    public static boolean checkAndTrigger(ServerPlayer caster, LivingEntity target, ElementType triggeringElement) {
        // Get all unique elements on the target
        Set<ElementType> uniqueElements = getUniqueElements(target);

        // Check if we have enough for a Super-Reaction
        SuperReactionTier tier = SuperReactionTier.fromElementCount(uniqueElements.size());

        if (!tier.canTrigger()) {
            return false;
        }

        // Find the element with the highest stack count
        ElementType primaryElement = getPrimaryElement(target);
        if (primaryElement == null) {
            return false;
        }

        // Get the super reaction for the primary element
        SuperReaction reaction = REACTIONS.get(primaryElement);
        if (reaction == null) {
            TalentsMod.LOGGER.warn("No super reaction registered for element: {}", primaryElement);
            return false;
        }

        // Calculate damage and other modifiers
        float baseDamage = calculateBaseDamage(caster, target, tier, primaryElement);

        // Execute the Super-Reaction
        reaction.execute(caster, target, tier, uniqueElements, baseDamage);

        // Apply all 6 elements if Resonant Cascade was consumed
        if (caster.getPersistentData().contains("resonant_cascade_apply_all_elements")) {
            caster.getPersistentData().remove("resonant_cascade_apply_all_elements");
            for (ElementType elem : ElementType.values()) {
                ElementalStackManager.applyElementStack(target, elem, caster, 1.0f);
            }
        }

        // Send visual feedback
        sendVisualFeedback(caster, target, primaryElement, tier);

        // Generate Focus for the caster
        generateFocus(caster, tier);

        // Clear all element stacks (they are consumed)
        clearAllStacks(target);

        // Check for Overflow trigger (Rank 3B) and activate Resonant Cascade (Rank 4B)
        caster.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            int attunementLevel = talents.getTalentLevel(
                ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_attunement")
            );
            ElementalAttunementTalent.checkOverflowTrigger(caster, attunementLevel);
        });

        // Log the reaction
        TalentsMod.LOGGER.info("Super-Reaction triggered! Element: {}, Tier: {}, Damage: {}",
            primaryElement, tier.getDisplayName(), baseDamage);

        return true;
    }

    /**
     * Get all unique elements currently on the target
     */
    private static Set<ElementType> getUniqueElements(LivingEntity target) {
        Set<ElementType> elements = new HashSet<>();
        Map<ElementType, Integer> stacks = ElementalStackManager.getEntityStacks(target);

        for (Map.Entry<ElementType, Integer> entry : stacks.entrySet()) {
            if (entry.getValue() > 0) {
                elements.add(entry.getKey());
            }
        }

        return elements;
    }

    /**
     * Get the element with the highest stack count
     */
    private static ElementType getPrimaryElement(LivingEntity target) {
        Map<ElementType, Integer> stacks = ElementalStackManager.getEntityStacks(target);
        ElementType primary = null;
        int maxStacks = 0;

        for (Map.Entry<ElementType, Integer> entry : stacks.entrySet()) {
            if (entry.getValue() > maxStacks) {
                maxStacks = entry.getValue();
                primary = entry.getKey();
            }
        }

        return primary;
    }

    /**
     * Calculate the base damage for the Super-Reaction
     * Uses design spec formula: Final_Effect = Base_Effect * (1 + Scaling_Factor * (Mastery - 1))
     */
    private static float calculateBaseDamage(ServerPlayer caster, LivingEntity target,
                                            SuperReactionTier tier, ElementType element) {
        // Base damage formula
        float baseDamage = 20f; // Base damage

        // Apply tier multiplier
        baseDamage *= tier.getDamageMultiplier();

        // Apply mastery scaling using correct formula
        float masteryMultiplier = getMasteryMultiplier(caster, element, tier);
        baseDamage *= masteryMultiplier;

        // Check for amplification from Unleash talent (Rank 3B)
        if (caster.getPersistentData().contains("amplification_multiplier")) {
            float amplification = caster.getPersistentData().getFloat("amplification_multiplier");
            baseDamage *= (1f + amplification);
            caster.getPersistentData().remove("amplification_multiplier");
        }

        // Check for Overload damage multiplier (Unleash Rank 3A multi-hit)
        if (caster.getPersistentData().contains("overload_damage_multiplier")) {
            float overloadMultiplier = caster.getPersistentData().getFloat("overload_damage_multiplier");
            baseDamage *= overloadMultiplier;
            caster.getPersistentData().remove("overload_damage_multiplier");
        }

        // Check for Resonant Cascade buff (Attunement Rank 4B)
        if (caster.getPersistentData().contains("resonant_cascade_active")) {
            long expirationTime = caster.getPersistentData().getLong("resonant_cascade_expiration");
            if (caster.level().getGameTime() < expirationTime) {
                baseDamage *= 2.0f; // 200% damage
                caster.getPersistentData().remove("resonant_cascade_active");
                caster.getPersistentData().remove("resonant_cascade_expiration");

                // Apply all 6 elements to target after this reaction
                caster.getPersistentData().putBoolean("resonant_cascade_apply_all_elements", true);
            } else {
                // Expired, clean up
                caster.getPersistentData().remove("resonant_cascade_active");
                caster.getPersistentData().remove("resonant_cascade_expiration");
            }
        }

        // Apply target's current health percentage for some reactions
        float healthPercent = target.getHealth() / target.getMaxHealth();
        if (healthPercent < 0.3f && tier == SuperReactionTier.TIER_4) {
            baseDamage *= 1.5f; // 50% bonus damage on low health targets at max tier
        }

        return baseDamage;
    }

    /**
     * Get mastery multiplier using correct formula from design spec
     * Formula: (1 + Scaling_Factor * (General_Mastery - 1) + Scaling_Factor * (Specific_Mastery - 1))
     * Mastery base value is 1, so attribute value of 0 means mastery of 1
     */
    private static float getMasteryMultiplier(ServerPlayer caster, ElementType element, SuperReactionTier tier) {
        // Get scaling factor for this tier
        float scalingFactor = tier.getScalingFactor();

        // Get general mastery (attribute value + 1 to get mastery value)
        double generalMasteryAttr = caster.getAttributeValue(MasteryAttributes.ELEMENTAL_MASTERY.get());
        float generalMastery = (float)(generalMasteryAttr + 1.0);

        // Get element-specific mastery
        RegistryObject<Attribute> elementAttr = getElementMasteryAttribute(element);
        double specificMasteryAttr = elementAttr != null ? caster.getAttributeValue(elementAttr.get()) : 0.0;
        float specificMastery = (float)(specificMasteryAttr + 1.0);

        // Apply formula: 1 + scalingFactor * (generalMastery - 1) + scalingFactor * (specificMastery - 1)
        return 1f + scalingFactor * (generalMastery - 1f) + scalingFactor * (specificMastery - 1f);
    }

    /**
     * Get the attribute registry object for element-specific mastery
     */
    private static RegistryObject<Attribute> getElementMasteryAttribute(ElementType element) {
        return switch (element) {
            case FIRE -> MasteryAttributes.FIRE_MASTERY;
            case AQUA -> MasteryAttributes.AQUA_MASTERY;
            case LIGHTNING -> MasteryAttributes.LIGHTNING_MASTERY;
            case ICE -> MasteryAttributes.ICE_MASTERY;
            case NATURE -> MasteryAttributes.NATURE_MASTERY;
            case ENDER -> MasteryAttributes.ENDER_MASTERY;
        };
    }

    /**
     * Send visual feedback for the Super-Reaction
     */
    private static void sendVisualFeedback(ServerPlayer caster, LivingEntity target,
                                          ElementType element, SuperReactionTier tier) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Create reaction text
        Component reactionName = Component.literal(element.getDisplayName() + " ")
            .withStyle(element.getChatColor())
            .append(Component.literal("Super-Reaction!")
                .withStyle(ChatFormatting.BOLD));

        Component tierText = Component.literal("Tier " + tier.ordinal())
            .withStyle(ChatFormatting.YELLOW);

        // Send to all nearby players
        BlockPos pos = target.blockPosition();
        for (ServerPlayer player : serverLevel.players()) {
            if (player.blockPosition().distSqr(pos) < 256) { // 16 blocks radius
                PacketHandler.sendTo(new SpawnReactionTextPacket(
                    target.getId(),
                    reactionName.getString() + " " + tierText.getString(),
                    element.getChatColor()
                ), player);
            }
        }

        // Spawn particles
        ParticleHelper.spawnSuperReactionParticles(serverLevel, target.position(), element, tier);
    }

    /**
     * Generate Focus for the caster from the Super-Reaction
     */
    private static void generateFocus(ServerPlayer caster, SuperReactionTier tier) {
        caster.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            // Base Focus generation
            float focusGain = 30f * tier.getDamageMultiplier();

            // Apply Elemental Attunement bonuses
            focusGain *= ElementalAttunementTalent.calculateFocusBonus(caster, true);

            // Add resource
            talents.addResource(focusGain);

            // Notify player
            caster.sendSystemMessage(Component.literal("+" + (int)focusGain + " Focus")
                .withStyle(ChatFormatting.AQUA));
        });
    }

    /**
     * Clear all element stacks from the target
     */
    private static void clearAllStacks(LivingEntity target) {
        for (ElementType element : ElementType.values()) {
            ElementalStackManager.setStacks(target, element, 0);
        }
    }

    /**
     * Check if an entity can trigger Super-Reactions (has 3+ unique elements)
     */
    public static boolean canTriggerSuperReaction(LivingEntity target) {
        Set<ElementType> uniqueElements = getUniqueElements(target);
        return uniqueElements.size() >= 3;
    }

    /**
     * Get the current Super-Reaction tier for an entity
     */
    public static SuperReactionTier getCurrentTier(LivingEntity target) {
        Set<ElementType> uniqueElements = getUniqueElements(target);
        return SuperReactionTier.fromElementCount(uniqueElements.size());
    }
}