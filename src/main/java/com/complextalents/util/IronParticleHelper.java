package com.complextalents.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;

public class IronParticleHelper {
     public static ParticleOptions getIronParticle(String particleName) {
        try {
            ResourceLocation particleId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", particleName);
            // Use reflection to access particle registry to avoid deprecation warnings
            var particleType = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.get(particleId);
            if (particleType == null) {
                return ParticleTypes.CRIT; // Fallback to vanilla particle
            }
            // For simple particle types without options, the type itself is a ParticleOptions
            if (particleType instanceof ParticleOptions options) {
                return options;
            }
            return ParticleTypes.CRIT;
        } catch (Exception e) {
            return ParticleTypes.CRIT; // Fallback to vanilla particle
        }
    }

}
