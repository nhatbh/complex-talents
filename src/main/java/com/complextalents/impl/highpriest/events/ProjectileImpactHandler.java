package com.complextalents.impl.highpriest.events;

import com.complextalents.TalentsMod;
import com.complextalents.impl.highpriest.entity.SanctuaryBarrierEntity;
import com.complextalents.impl.highpriest.util.ProjectileDamageResolver;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.highpriest.SpawnBarrierFXPacket;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handler for projectile impacts on Sanctuary Barriers.
 * <p>
 * Uses Forge's event system for projectile collision detection instead of polling.
 * This is performance-optimized and works with all projectiles (vanilla + modded).
 * </p>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class ProjectileImpactHandler {

    /**
     * Handle projectile impacts on Sanctuary Barriers.
     * Cancels the projectile's impact and damages the barrier instead.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        var rayTraceResult = event.getRayTraceResult();
        if (!(rayTraceResult instanceof net.minecraft.world.phys.EntityHitResult hit)) {
            return;
        }

        if (!(hit.getEntity() instanceof SanctuaryBarrierEntity barrier)) {
            return;
        }

        var projectile = event.getProjectile();

        // Check if projectile is within the sphere
        if (!barrier.isProjectileInSphere(projectile)) {
            return;
        }

        // Resolve projectile damage
        float damage = ProjectileDamageResolver.get(projectile);

        // Damage the barrier
        barrier.damage(damage);

        // Destroy the projectile
        projectile.discard();

        // Send hit effect packet
        Vec3 impactPos = projectile.position();
        PacketHandler.sendToNearby(
            new SpawnBarrierFXPacket(
                impactPos.x, impactPos.y, impactPos.z,
                SpawnBarrierFXPacket.EffectType.ENTITY_HIT,
                barrier.getRadius(),
                barrier.getId()
            ),
            (net.minecraft.server.level.ServerLevel) barrier.level(), impactPos
        );

        // Cancel the impact event
        event.setCanceled(true);

        TalentsMod.LOGGER.debug("Projectile hit Sanctuary Barrier. Damage: {}", damage);
    }
}
