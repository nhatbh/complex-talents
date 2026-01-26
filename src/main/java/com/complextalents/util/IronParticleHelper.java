package com.complextalents.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for accessing Iron's Spellbooks particles with fallback support.
 * Uses reflection to dynamically access Iron's particle types without hard dependencies.
 */
public class IronParticleHelper {

    private static final Map<String, ResourceLocation> PARTICLE_CACHE = new HashMap<>();

    static {
        // Pre-cache commonly used Iron's Spellbooks particle IDs
        String[] ironParticles = {
            "fire", "dragon_fire", "ice", "snowflake", "lightning", "electricity",
            "acid_bubble", "unstable_ender", "portal", "nature", "firefly",
            "ember", "smoke", "ender", "blood", "magic", "shield"
        };
        for (String particle : ironParticles) {
            PARTICLE_CACHE.put(particle, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", particle));
        }
    }

    /**
     * Gets an Iron's Spellbooks particle by name.
     * Falls back to a vanilla particle if Iron's particle is not found.
     *
     * @param particleName The name of the particle (without namespace)
     * @return The particle options, or null if not found
     */
    @Nullable
    public static ParticleOptions getIronParticle(String particleName) {
        if (particleName == null || particleName.isEmpty()) {
            return null;
        }

        try {
            ResourceLocation particleId = PARTICLE_CACHE.getOrDefault(
                particleName,
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", particleName)
            );

            // Access the particle type from the registry
            var particleType = BuiltInRegistries.PARTICLE_TYPE.get(particleId);
            if (particleType == null) {
                return getFallbackParticle(particleName);
            }

            // For simple particle types without options, the type itself is a ParticleOptions
            if (particleType instanceof ParticleOptions options) {
                return options;
            }

            return getFallbackParticle(particleName);
        } catch (Exception e) {
            // Silently fall back on error
            return getFallbackParticle(particleName);
        }
    }

    /**
     * Returns appropriate vanilla fallback particles based on the particle name.
     * This ensures graceful degradation when Iron's Spellbooks is not available.
     *
     * @param particleName The original particle name
     * @return A vanilla particle as fallback
     */
    private static ParticleOptions getFallbackParticle(String particleName) {
        return switch (particleName.toLowerCase()) {
            case "fire", "dragon_fire", "ember", "blood" -> ParticleTypes.FLAME;
            case "ice", "snowflake" -> ParticleTypes.SNOWFLAKE;
            case "lightning", "electricity" -> ParticleTypes.ELECTRIC_SPARK;
            case "acid_bubble", "nature" -> ParticleTypes.BUBBLE;
            case "unstable_ender", "portal", "ender", "magic" -> ParticleTypes.PORTAL;
            case "firefly" -> ParticleTypes.ITEM_SLIME;
            case "smoke" -> ParticleTypes.SMOKE;
            case "shield" -> ParticleTypes.TOTEM_OF_UNDYING;
            default -> ParticleTypes.CLOUD;
        };
    }

    /**
     * Checks if Iron's Spellbooks is loaded and a specific particle is available.
     *
     * @param particleName The particle name to check
     * @return true if the particle is available
     */
    public static boolean isParticleAvailable(String particleName) {
        if (particleName == null || particleName.isEmpty()) {
            return false;
        }
        try {
            ResourceLocation particleId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", particleName);
            return BuiltInRegistries.PARTICLE_TYPE.get(particleId) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
