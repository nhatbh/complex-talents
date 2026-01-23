package com.complextalents.elemental;

import com.complextalents.TalentsMod;
import com.complextalents.capability.PlayerTalents;
import com.complextalents.capability.PlayerTalentsImpl;
import com.complextalents.capability.TalentsCapabilities;
import com.complextalents.config.TalentConfig;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.SpawnParticlesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class ElementalStackManager {
    private static final Map<UUID, Map<ElementType, ElementStack>> entityElements = new HashMap<>();

    public static void init() {
        TalentsMod.LOGGER.info("Elemental Stack Manager initialized");
    }

    /**
     * Applies an element stack to a target entity.
     * This is called from all magic damage sources (vanilla and modded).
     *
     * @param target The entity to apply the element to
     * @param element The element type to apply
     * @param source The entity causing the damage (for talent checking)
     * @param spellDamage The amount of damage the triggering spell dealt
     */
    public static void applyElementStack(LivingEntity target, ElementType element, LivingEntity source, float spellDamage) {
        if (!TalentConfig.enableElementalSystem.get()) return;

        UUID targetId = target.getUUID();
        Map<ElementType, ElementStack> elements = entityElements.computeIfAbsent(targetId, k -> new HashMap<>());

        // Check for reactions BEFORE adding the new element
        // This ensures we check the new element against existing elements only
        if (source instanceof ServerPlayer player) {
            TalentsMod.LOGGER.debug("Source is ServerPlayer: {} (UUID: {})", player.getName().getString(), player.getUUID());
            TalentsMod.LOGGER.debug("Player class: {}, is ServerPlayer: {}", player.getClass().getName(), player instanceof ServerPlayer);
            checkAndTriggerReactions(target, elements, element, player, spellDamage);
        } else {
            TalentsMod.LOGGER.debug("Source is NOT ServerPlayer. Source type: {}, Source: {}",
                source != null ? source.getClass().getName() : "null",
                source != null ? source.getName().getString() : "null");
        }

        // Get or create element stack
        ElementStack stack = elements.get(element);
        boolean isNewStack = false;
        if (stack != null) {
            stack.addStack();
        } else {
            stack = new ElementStack(element, target, source);
            elements.put(element, stack);
            isNewStack = true;
        }

        // Cap the stack count and refresh timer
        int maxStacks = TalentConfig.maxStackCount.get();
        if (stack.getStackCount() > maxStacks) {
            stack.setStackCount(maxStacks);
            stack.refreshTimer(); // Refresh timer after capping
        }

        // Calculate time until expiry for debug message
        long decayMillis = TalentConfig.stackDecayTicks.get() * 50L;
        long timeUntilExpiry = stack.getTimeUntilExpiry(decayMillis);

        // Spawn particle effects for stack application
        if (target.level() instanceof ServerLevel serverLevel) {
            Vec3 particlePos = target.position().add(0, target.getBbHeight() / 2, 0);
            SpawnParticlesPacket packet = new SpawnParticlesPacket(particlePos, element, stack.getStackCount());
            PacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), packet);
        }

        // Send chat message to source player about stack application
        if (source instanceof ServerPlayer player) {
            long secondsUntilExpiry = timeUntilExpiry / 1000;
            String stackStatus = isNewStack ? "NEW" : "REFRESHED";
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(String.format(
                    "§a[%s] §e%s §f%s §7(Stack: %d, Expires: %ds)",
                    stackStatus,
                    target.getName().getString(),
                    element.name(),
                    stack.getStackCount(),
                    secondsUntilExpiry
                ))
            );
        }
    }

    /**
     * Legacy method for backward compatibility - uses default damage of 1.0
     * @deprecated Use applyElementStack with spellDamage parameter instead
     */
    @Deprecated
    public static void applyElementStack(LivingEntity target, ElementType element, LivingEntity source) {
        applyElementStack(target, element, source, 1.0f);
    }

    private static void checkAndTriggerReactions(LivingEntity target, Map<ElementType, ElementStack> elements, ElementType newElement, ServerPlayer player, float spellDamage) {
        TalentsMod.LOGGER.debug("checkAndTriggerReactions called for player: {} (UUID: {})", player.getName().getString(), player.getUUID());

        LazyOptional<PlayerTalents> capabilityOptional = player.getCapability(TalentsCapabilities.PLAYER_TALENTS);
        TalentsMod.LOGGER.debug("Capability present: {}", capabilityOptional.isPresent());

        player.sendSystemMessage(
            net.minecraft.network.chat.Component.literal(
                String.format("§e[Debug] Capability present: %s", capabilityOptional.isPresent())
            )
        );

        capabilityOptional.ifPresent(talents -> {
            // Check if player has Elemental Mastery or any specialist talent
            boolean hasElementalTalent = hasAnyElementalTalent(talents);

            TalentsMod.LOGGER.debug("Player {} has elemental talent: {}", player.getName().getString(), hasElementalTalent);

            if (!hasElementalTalent) {
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                        "§c[No Elemental Talent] §7You need an Elemental Mastery talent to trigger reactions!"
                    )
                );
                return; // No reaction without talent
            }

            // Check for reactions with existing elements
            for (Map.Entry<ElementType, ElementStack> entry : elements.entrySet()) {
                ElementType existingElement = entry.getKey();

                TalentsMod.LOGGER.debug("Checking reaction between {} (existing) and {} (new)", existingElement, newElement);

                if (existingElement != newElement && existingElement.canReactWith(newElement)) {
                    ElementalReaction reaction = existingElement.getReactionWith(newElement);

                    TalentsMod.LOGGER.debug("Reaction found: {}", reaction);

                    if (reaction != null) {
                        // Check if player has the specific talent for this reaction
                        if (canTriggerReaction(talents, reaction, existingElement, newElement)) {
                            TalentsMod.LOGGER.info("Triggering reaction: {}", reaction);
                            ElementalReactionHandler.triggerReaction(target, reaction, newElement, existingElement, player, spellDamage);
                            // Only one reaction at a time
                            break;
                        } else {
                            TalentsMod.LOGGER.debug("Player cannot trigger this reaction (missing specific talent)");
                        }
                    }
                } else {
                    TalentsMod.LOGGER.debug("No reaction possible (same element or incompatible)");
                }
            }
        });
    }

    private static boolean hasAnyElementalTalent(PlayerTalents talents) {
        // Debug: Log all unlocked talents
        List<ResourceLocation> allTalents = talents.getUnlockedTalents();
        TalentsMod.LOGGER.debug("Player has {} talents: {}", allTalents.size(), allTalents);

        // Debug: Check each mastery talent explicitly
        int elementalLevel = talents.getTalentLevel(ElementalTalents.ELEMENTAL_MASTERY);
        int fireLevel = talents.getTalentLevel(ElementalTalents.FIRE_MASTERY);
        int aquaLevel = talents.getTalentLevel(ElementalTalents.AQUA_MASTERY);
        int iceLevel = talents.getTalentLevel(ElementalTalents.ICE_MASTERY);
        int lightningLevel = talents.getTalentLevel(ElementalTalents.LIGHTNING_MASTERY);
        int natureLevel = talents.getTalentLevel(ElementalTalents.NATURE_MASTERY);
        int enderLevel = talents.getTalentLevel(ElementalTalents.ENDER_MASTERY);

        TalentsMod.LOGGER.debug("Elemental Mastery level: {}", elementalLevel);
        TalentsMod.LOGGER.debug("Fire Mastery level: {}", fireLevel);
        TalentsMod.LOGGER.debug("Aqua Mastery level: {}", aquaLevel);
        TalentsMod.LOGGER.debug("Ice Mastery level: {}", iceLevel);
        TalentsMod.LOGGER.debug("Lightning Mastery level: {}", lightningLevel);
        TalentsMod.LOGGER.debug("Nature Mastery level: {}", natureLevel);
        TalentsMod.LOGGER.debug("Ender Mastery level: {}", enderLevel);

        // Send debug info to player via chat
        String talentList = allTalents.isEmpty() ? "none" : allTalents.toString();
        ServerPlayer player = talents instanceof PlayerTalentsImpl impl ? impl.getOwningPlayer() : null;
        if (player != null) {
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    String.format("§e[Debug] Talents: %s | Elemental Mastery level: %d", talentList, elementalLevel)
                )
            );
        }

        // Use getTalentLevel which returns 0 if not present, avoiding multiple hasTalent() calls
        return elementalLevel > 0 || fireLevel > 0 || aquaLevel > 0 ||
               iceLevel > 0 || lightningLevel > 0 || natureLevel > 0 || enderLevel > 0;
    }

    private static boolean canTriggerReaction(PlayerTalents talents, ElementalReaction reaction, ElementType element1, ElementType element2) {
        // Elemental Mastery allows all reactions
        if (talents.getTalentLevel(ElementalTalents.ELEMENTAL_MASTERY) > 0) {
            return true;
        }

        // Mastery talents allow their specific element's reactions
        return switch (element1) {
            case FIRE -> talents.getTalentLevel(ElementalTalents.FIRE_MASTERY) > 0;
            case AQUA -> talents.getTalentLevel(ElementalTalents.AQUA_MASTERY) > 0;
            case ICE -> talents.getTalentLevel(ElementalTalents.ICE_MASTERY) > 0;
            case LIGHTNING -> talents.getTalentLevel(ElementalTalents.LIGHTNING_MASTERY) > 0;
            case NATURE -> talents.getTalentLevel(ElementalTalents.NATURE_MASTERY) > 0;
            case ENDER -> talents.getTalentLevel(ElementalTalents.ENDER_MASTERY) > 0;
        };
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Level level = event.level;
        if (level.isClientSide) return;

        // Early return if no stacks to process
        if (entityElements.isEmpty()) return;

        long decayMillis = TalentConfig.stackDecayTicks.get() * 50L; // Convert ticks to milliseconds
        long gameTime = level.getGameTime();

        // Process all entities with stacks
        entityElements.entrySet().removeIf(entry -> {
            Map<ElementType, ElementStack> stacks = entry.getValue();

            // Get entity reference from the stack itself (more reliable than UUID lookup)
            ElementStack anyStack = stacks.values().iterator().next();
            LivingEntity entity = anyStack.getEntity();

            if (entity == null || !entity.isAlive()) {
                return true; // Remove stacks for dead/non-existent entities
            }

            // Make sure entity is in the correct dimension
            if (!entity.level().equals(level)) {
                return false; // Skip processing this entity in this dimension
            }

            // Spawn particles every 10 ticks (0.5 seconds) for visual feedback
            if (gameTime % 10 == 0) {
                Vec3 particlePos = entity.position().add(0, entity.getBbHeight() / 2, 0);
                for (Map.Entry<ElementType, ElementStack> stackEntry : stacks.entrySet()) {
                    ElementType element = stackEntry.getKey();
                    ElementStack stack = stackEntry.getValue();

                    // Spawn continuous particle effect
                    SpawnParticlesPacket packet = new SpawnParticlesPacket(particlePos, element, stack.getStackCount());
                    PacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
                }
            }

            // Remove expired stacks every second (20 ticks)
            if (gameTime % 20 == 0) {
                stacks.entrySet().removeIf(stackEntry -> stackEntry.getValue().isExpired(decayMillis));
            }

            return stacks.isEmpty(); // Remove entity entry if no stacks remain
        });
    }

    public static void clearEntityStacks(UUID entityId) {
        entityElements.remove(entityId);
    }

    /**
     * Check if an entity has any elemental stacks
     */
    public static boolean hasAnyStack(UUID entityId) {
        Map<ElementType, ElementStack> stacks = entityElements.get(entityId);
        return stacks != null && !stacks.isEmpty();
    }

    /**
     * Check if an entity has any elemental stacks (entity version)
     */
    public static boolean hasAnyStack(LivingEntity entity) {
        return hasAnyStack(entity.getUUID());
    }

    /**
     * Get all elemental stacks for an entity
     * Returns a map of ElementType to stack count
     */
    public static Map<ElementType, Integer> getEntityStacks(UUID entityId) {
        Map<ElementType, Integer> result = new HashMap<>();
        Map<ElementType, ElementStack> stacks = entityElements.get(entityId);
        if (stacks != null) {
            for (Map.Entry<ElementType, ElementStack> entry : stacks.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getStackCount());
            }
        }
        return result;
    }

    /**
     * Get all elemental stacks for an entity (entity version)
     */
    public static Map<ElementType, Integer> getEntityStacks(LivingEntity entity) {
        return getEntityStacks(entity.getUUID());
    }

    /**
     * Get the element type with the highest stack count on an entity
     * Returns null if no stacks are present
     */
    public static ElementType getHighestStack(UUID entityId) {
        Map<ElementType, ElementStack> stacks = entityElements.get(entityId);
        if (stacks == null || stacks.isEmpty()) {
            return null;
        }

        ElementType highest = null;
        int maxCount = 0;
        for (Map.Entry<ElementType, ElementStack> entry : stacks.entrySet()) {
            if (entry.getValue().getStackCount() > maxCount) {
                maxCount = entry.getValue().getStackCount();
                highest = entry.getKey();
            }
        }
        return highest;
    }

    /**
     * Set the stack count for a specific element on an entity
     */
    public static void setStacks(LivingEntity target, ElementType element, int count) {
        if (count <= 0) {
            // Remove the element stack if count is 0 or less
            Map<ElementType, ElementStack> stacks = entityElements.get(target.getUUID());
            if (stacks != null) {
                stacks.remove(element);
                if (stacks.isEmpty()) {
                    entityElements.remove(target.getUUID());
                }
            }
        } else {
            Map<ElementType, ElementStack> stacks = entityElements.computeIfAbsent(target.getUUID(), k -> new HashMap<>());
            ElementStack stack = stacks.get(element);
            if (stack != null) {
                stack.setStackCount(count);
            } else {
                // Create new stack with specified count
                stack = new ElementStack(element, target, null);
                stack.setStackCount(count);
                stacks.put(element, stack);
            }
        }
    }
}
