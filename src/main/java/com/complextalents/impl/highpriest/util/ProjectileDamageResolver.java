package com.complextalents.impl.highpriest.util;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.ShulkerBullet;

/**
 * Utility class for resolving damage from various projectile types.
 * <p>
 * Provides damage values for vanilla projectiles and fallback for modded ones.
 * </p>
 */
public class ProjectileDamageResolver {

    /**
     * Get the damage amount for a given projectile.
     *
     * @param p The projectile
     * @return The damage value
     */
    public static float get(Projectile p) {
        // Vanilla projectiles
        if (p instanceof AbstractArrow arrow) {
            return (float) arrow.getBaseDamage();
        }

        if (p instanceof ThrownTrident) {
            return 8f;
        }

        if (p instanceof Fireball) {
            // Large fireball (from ghasts or dispensers)
            return 6f; // Fixed damage for large fireballs
        }

        if (p instanceof SmallFireball) {
            return 3f;
        }

        if (p instanceof DragonFireball) {
            return 15f;
        }

        if (p instanceof WitherSkull) {
            return 8f;
        }

        if (p instanceof ShulkerBullet) {
            return 4f;
        }

        // Abstract hurting projectiles (blaze fireballs, etc.)
        if (p instanceof AbstractHurtingProjectile) {
            // Power is stored as a private field, use estimated value
            return 5f;
        }

        // Default fallback for modded projectiles
        return 5f;
    }
}
