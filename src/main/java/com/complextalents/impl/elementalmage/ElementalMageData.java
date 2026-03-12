package com.complextalents.impl.elementalmage;

import com.complextalents.elemental.ElementType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import com.complextalents.util.UUIDHelper;

/**
 * Server-side tracking and logic for Elemental Mage stats.
 * Applies diminishing returns, balance metric scaling, and one-trick overrides
 * based on the exact mathematical framework.
 */
public class ElementalMageData {

    // Per-player elemental stats
    private static final ConcurrentHashMap<UUID, Map<ElementType, Float>> PLAYER_STATS = new ConcurrentHashMap<>();

    // The 5 valid schools for the Elemental Mage
    private static final ElementType[] VALID_SCHOOLS = {
            ElementType.FIRE, ElementType.AQUA, ElementType.LIGHTNING, ElementType.ICE, ElementType.NATURE
    };

    // Attribute ResourceLocations for each school
    private static final Map<ElementType, ResourceLocation> SPELL_POWER_ATTRIBUTES = Map.of(
            ElementType.FIRE, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "fire_spell_power"),
            ElementType.AQUA, ResourceLocation.fromNamespaceAndPath("traveloptics", "aqua_spell_power"),
            ElementType.LIGHTNING, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "lightning_spell_power"),
            ElementType.ICE, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "ice_spell_power"),
            ElementType.NATURE, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "nature_spell_power"));

    // AttributeModifier UUIDs for each school
    private static final Map<ElementType, UUID> SPELL_POWER_UUIDS = Map.of(
            ElementType.FIRE, UUIDHelper.generateAttributeModifierUUID("elemental_mage", "fire_power"),
            ElementType.AQUA, UUIDHelper.generateAttributeModifierUUID("elemental_mage", "aqua_power"),
            ElementType.LIGHTNING, UUIDHelper.generateAttributeModifierUUID("elemental_mage", "lightning_power"),
            ElementType.ICE, UUIDHelper.generateAttributeModifierUUID("elemental_mage", "ice_power"),
            ElementType.NATURE, UUIDHelper.generateAttributeModifierUUID("elemental_mage", "nature_power"));

    // Harmonic Convergence skill tracking
    public static class ConvergenceBuff {
        public boolean waitingForNextSpell = false;
        public String buffedSpellId = null;
        public int buffWindowTicks = 0;
        public double cachedCritChanceOffset = 0.0;
        public double cachedCritDamageBonus = 0.0;
    }

    private static final ConcurrentHashMap<UUID, ConvergenceBuff> CONVERGENCE_BUFFS = new ConcurrentHashMap<>();

    public static ConvergenceBuff getConvergenceBuff(UUID playerUuid) {
        return CONVERGENCE_BUFFS.computeIfAbsent(playerUuid, k -> new ConvergenceBuff());
    }

    /**
     * Gets a player's stat for a specific element.
     */
    public static float getStat(UUID playerUuid, ElementType element) {
        Map<ElementType, Float> stats = PLAYER_STATS.getOrDefault(playerUuid, Map.of());
        return stats.getOrDefault(element, 0.0f);
    }

    /**
     * Set a player's stat directly.
     */
    public static void setStat(UUID playerUuid, ElementType element, float value) {
        PLAYER_STATS.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                .put(element, Math.max(0.0f, value));
    }

    /**
     * Process elemental damage dealt by the player and update stats according to
     * the mathematical framework.
     */
    public static void processElementalDamage(ServerPlayer player, ElementType castElement, float damage) {
        // Ensure it's a valid school for the origin
        if (!isValidSchool(castElement))
            return;

        UUID uuid = player.getUUID();

        // 1. Calculate base value (Delta) with diminishing returns
        // Delta = sqrt(D) / 1000
        float delta = (float) (Math.sqrt(damage) / 1000.0);

        // 2. Calculate the Coefficient of Variation (Cv)
        float s = 0; // Sum of all 5 stats
        for (ElementType school : VALID_SCHOOLS) {
            s += getStat(uuid, school);
        }

        float cv = 0.0f;
        if (s > 0) {
            float mu = s / 5.0f; // Mean
            float varianceSum = 0;
            for (ElementType school : VALID_SCHOOLS) {
                float diff = getStat(uuid, school) - mu;
                varianceSum += diff * diff;
            }
            float variance = varianceSum / 5.0f;
            float sigma = (float) Math.sqrt(variance); // Standard Deviation
            cv = sigma / mu;
        }

        // 3. Define the Target Zones (Anchor Points)
        // Hard Way (5 elements equal): Cv = 0.0
        // Easy Way (Exactly 2 elements equal, 3 zero): Cv ≈ 1.225
        float dist5 = Math.abs(cv - 0.0f);
        float dist2 = Math.abs(cv - 1.22474f); // Exact value is sqrt(1.5) ≈ 1.22474
        float minDist = Math.min(dist5, dist2);

        // Map minimum distance to the Balance Metric (B)
        // Tolerance T = 0.6f. If you deviate more than 0.6 from an anchor, B drops to
        // 0.
        float b = Math.max(0.0f, 1.0f - (minDist / 0.6f));

        float mInc = (0.1f + (0.9f * b));
        float mDec = (1.0f - (0.9f * b)) * 0.6f;

        // 5. The Final Update
        StringBuilder debugMsg = new StringBuilder();
        debugMsg.append("§e[Elemental Mage] Cast: §b").append(castElement.name())
                .append("§e | Dmg: §c").append(String.format("%.1f", damage))
                .append("§e | Cv: §a").append(String.format("%.3f", cv))
                .append("§e | Bal(B): §d").append(String.format("%.2f", b))
                .append("\n");

        for (ElementType school : VALID_SCHOOLS) {
            float currentVal = getStat(uuid, school);
            float newVal;

            if (school == castElement) {
                // Increase the cast school
                float increase = delta * mInc;
                newVal = currentVal + increase;
                debugMsg.append("  §a+ ").append(school.name()).append(": ").append(String.format("%.4f", currentVal))
                        .append(" -> ").append(String.format("%.4f", newVal)).append("\n");
            } else {
                // Decrease the other schools
                float decrease = delta * mDec;
                newVal = Math.max(0.0f, currentVal - decrease);
                if (currentVal > 0) {
                    debugMsg.append("  §c- ").append(school.name()).append(": ")
                            .append(String.format("%.4f", currentVal)).append(" -> ")
                            .append(String.format("%.4f", newVal)).append("\n");
                }
            }
            setStat(uuid, school, newVal);
        }

        // Send chat-based debug info to the player
        player.sendSystemMessage(Component.literal(debugMsg.toString()));

        // Apply changes to the actual player attributes
        applyAttributeModifiers(player);

        // Sync to client (to be implemented)
        syncToClient(player);
    }

    private static boolean isValidSchool(ElementType type) {
        for (ElementType school : VALID_SCHOOLS) {
            if (school == type)
                return true;
        }
        return false;
    }

    /**
     * Applies the current Elemental Mage stats as actual attribute modifiers on the
     * player.
     * Use operation ADDITION to stack properly.
     */
    public static void applyAttributeModifiers(ServerPlayer player) {
        UUID playerUuid = player.getUUID();

        for (ElementType school : VALID_SCHOOLS) {
            float statValue = getStat(playerUuid, school);

            ResourceLocation attrId = SPELL_POWER_ATTRIBUTES.get(school);
            UUID modUuid = SPELL_POWER_UUIDS.get(school);

            if (attrId != null && modUuid != null) {
                Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attrId);
                if (attribute != null) {
                    var attributeInstance = player.getAttributes().getInstance(attribute);
                    if (attributeInstance != null) {
                        // Always remove the existing modifier to refresh it
                        attributeInstance.removeModifier(modUuid);

                        // Add the new modifier if the stat is > 0
                        if (statValue > 0.0f) {
                            AttributeModifier modifier = new AttributeModifier(
                                    modUuid,
                                    "Elemental Mage " + school.name() + " Power",
                                    statValue,
                                    AttributeModifier.Operation.ADDITION);
                            attributeInstance.addTransientModifier(modifier);
                        }
                    }
                }
            }
        }
    }

    // --- Persistence Methods ---

    public static CompoundTag serializeNBT(UUID playerUuid) {
        CompoundTag tag = new CompoundTag();
        Map<ElementType, Float> stats = PLAYER_STATS.getOrDefault(playerUuid, Map.of());
        for (ElementType school : VALID_SCHOOLS) {
            if (stats.containsKey(school)) {
                tag.putFloat(school.name(), stats.get(school));
            }
        }
        return tag;
    }

    public static void deserializeNBT(UUID playerUuid, CompoundTag tag) {
        Map<ElementType, Float> stats = new ConcurrentHashMap<>();
        for (ElementType school : VALID_SCHOOLS) {
            if (tag.contains(school.name())) {
                stats.put(school, tag.getFloat(school.name()));
            }
        }
        if (!stats.isEmpty()) {
            PLAYER_STATS.put(playerUuid, stats);
        }
    }

    public static void cleanup(UUID playerUuid) {
        PLAYER_STATS.remove(playerUuid);
    }

    public static void cleanup(ServerPlayer player) {
        cleanup(player.getUUID());
    }

    public static void syncToClient(ServerPlayer player) {
        Map<ElementType, Float> stats = PLAYER_STATS.getOrDefault(player.getUUID(), Map.of());
        com.complextalents.network.PacketHandler
                .sendTo(new com.complextalents.network.elementalmage.ElementalMageSyncPacket(stats), player);
    }
}
