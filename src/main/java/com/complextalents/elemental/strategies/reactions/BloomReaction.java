package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.elemental.entity.ModEntities;
import com.complextalents.elemental.entity.NatureCoreEntity;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnBloomReactionPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Bloom Reaction (Aqua + Nature)
 * Spawns a Nature Core entity at the target's location
 * The Nature Core will explode when hit by Fire or Lightning, or self-detonate after 4 seconds
 */
public class BloomReaction implements IReactionStrategy {

    @Override
    public void execute(ReactionContext context) {
        LivingEntity target = context.getTarget();
        Vec3 pos = target.position();

        // Spawn 3 Nature Cores around the target in a triangle pattern
        double radius = 2.0; // Distance from target center
        int coreCount = 3;
        double angleVariance = Math.PI / 6; // 30 degrees of variance per core

        // Add random rotation to the entire pattern for variety
        double randomRotation = context.getLevel().random.nextDouble() * Math.PI * 2;

        for (int i = 0; i < coreCount; i++) {
            // Base angle + random rotation + individual variance
            double baseAngle = ((i / (double) coreCount) * Math.PI * 2) + randomRotation;
            double variance = (context.getLevel().random.nextDouble() - 0.5) * 2 * angleVariance;
            double angle = baseAngle + variance;

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            Vec3 corePos = new Vec3(
                pos.x + offsetX,
                pos.y + 0.5,
                pos.z + offsetZ
            );

            NatureCoreEntity natureCore = ModEntities.NATURE_CORE.get().create(context.getLevel());
            if (natureCore != null) {
                natureCore.setPos(corePos.x, corePos.y, corePos.z);
                natureCore.setPersistenceRequired(); // Prevent despawn
                // Set the owner so they get credit for the explosion damage
                if (context.getAttacker() != null) {
                    natureCore.setOwner(context.getAttacker().getName().getString());
                }
                context.getLevel().addFreshEntity(natureCore);
            }
        }

        // Send visual effect packet (spawn at center for the initial bloom effect)
        PacketHandler.sendToNearby(
            new SpawnBloomReactionPacket(pos),
            context.getLevel(),
            pos
        );
    }

    @Override
    public float calculateDamage(ReactionContext context) {
        // No direct damage - damage comes from Nature Core explosion
        return 0.0f;
    }

    @Override
    public boolean canTrigger(ReactionContext context) {
        return context.getTarget() != null && context.getTarget().isAlive();
    }

    @Override
    public ElementalReaction getReactionType() {
        return ElementalReaction.BLOOM;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean consumesStacks() {
        return true;
    }
}
